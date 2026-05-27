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
    const msgId = `<${Date.now()}.${Math.random().toString(36).slice(2)}@massagecentr.app>`;
    await transporter.sendMail({
      from: `"Med Center 8za" <${process.env.GMAIL_USER}>`,
      replyTo: process.env.GMAIL_USER,
      to: email,
      subject: `Код подтверждения: ${otp}`,
      messageId: msgId,
      headers: {
        "X-Mailer": "MassageCentr App",
        "X-Priority": "1",
        "Precedence": "transactional",
      },
      text: `Ваш код для входа в приложение: ${otp}\n\nКод действителен 5 минут.\nЕсли вы не запрашивали этот код — проигнорируйте это письмо.`,
      html: `
        <table width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;">
          <tr><td style="padding:16px 0;border-bottom:2px solid #8DC63F;">
            <span style="font-size:17px;font-weight:bold;color:#333;">Медицинский центр «8 за»</span>
          </td></tr>
          <tr><td style="padding:20px 0 8px;">
            <p style="margin:0 0 12px;color:#333;font-size:15px;">Ваш код для входа:</p>
            <p style="margin:0;font-size:36px;font-weight:bold;letter-spacing:8px;color:#222;background:#f5f5f5;
                      border-radius:6px;padding:10px 16px;display:inline-block;">${otp}</p>
            <p style="margin:16px 0 0;color:#666;font-size:13px;">Код действителен 5 минут.</p>
            <p style="margin:8px 0 0;color:#999;font-size:12px;">Если вы не запрашивали этот код — проигнорируйте письмо.</p>
          </td></tr>
        </table>
      `,
    });

    return res.status(200).json({ success: true });
  } catch (err) {
    console.error("Email send error:", err.message);
    return res.status(500).json({ error: "Не удалось отправить письмо. Проверьте настройки Gmail." });
  }
};
