const nodemailer = require("nodemailer");
const admin = require("firebase-admin");

// Инициализация Firebase Admin (один раз)
if (!admin.apps.length) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}
const db = admin.firestore();

function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const { email } = req.body || {};
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return res.status(400).json({ error: "Неверный формат email" });
  }

  const emailKey = email.toLowerCase().replace(/\./g, "_");

  // Ограничение: не чаще 1 раза в минуту
  const existing = await db.collection("otps").doc(emailKey).get();
  if (existing.exists) {
    const sentAt = existing.data().sentAt?.toMillis() || 0;
    if (Date.now() - sentAt < 60_000) {
      return res.status(429).json({ error: "Подождите минуту перед повторной отправкой" });
    }
  }

  const otp = generateOtp();
  const expiry = admin.firestore.Timestamp.fromDate(new Date(Date.now() + 5 * 60_000));

  await db.collection("otps").doc(emailKey).set({
    code: otp,
    expiry,
    sentAt: admin.firestore.Timestamp.now(),
    attempts: 0,
  });

  // Отправка через Gmail SMTP
  const transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: process.env.GMAIL_USER,
      pass: process.env.GMAIL_APP_PASSWORD,
    },
  });

  try {
    await transporter.sendMail({
      from: `"Медцентр «8 за»" <${process.env.GMAIL_USER}>`,
      to: email,
      subject: "Код для входа — Медицинский центр «8 за»",
      text: `Ваш код подтверждения: ${otp}\n\nКод действителен 5 минут.\nЕсли вы не запрашивали этот код — проигнорируйте письмо.`,
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 420px; margin: 0 auto;">
          <div style="background:#8DC63F; padding:16px 24px; border-radius:8px 8px 0 0;">
            <h2 style="color:#fff; margin:0; font-size:18px;">Медицинский центр «8 за»</h2>
          </div>
          <div style="background:#f9f9f9; padding:24px; border-radius:0 0 8px 8px; border:1px solid #e0e0e0;">
            <p style="color:#333; margin-top:0;">Ваш код для входа в приложение:</p>
            <div style="font-size:38px; font-weight:bold; letter-spacing:10px; color:#333; background:#fff;
                        border:2px solid #8DC63F; border-radius:8px; padding:12px; text-align:center; margin:16px 0;">
              ${otp}
            </div>
            <p style="color:#666; font-size:13px;">Код действителен <strong>5 минут</strong>.</p>
            <p style="color:#999; font-size:12px; margin-bottom:0;">
              Если вы не запрашивали этот код — просто проигнорируйте письмо.
            </p>
          </div>
        </div>
      `,
    });

    return res.status(200).json({ success: true });
  } catch (err) {
    console.error("Email send error:", err.message);
    return res.status(500).json({ error: "Не удалось отправить письмо. Проверьте настройки Gmail." });
  }
};
