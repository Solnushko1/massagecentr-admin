const admin = require("firebase-admin");

if (!admin.apps.length) {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}
const db = admin.firestore();

module.exports = async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const { email, code } = req.body || {};
  if (!email || !code) {
    return res.status(400).json({ error: "Email и код обязательны" });
  }

  const emailKey = email.toLowerCase().replace(/\./g, "_");
  const otpDoc = await db.collection("otps").doc(emailKey).get();

  if (!otpDoc.exists) {
    return res.status(404).json({ error: "Код не найден. Запросите новый." });
  }

  const data = otpDoc.data();

  if (Date.now() > data.expiry.toMillis()) {
    await db.collection("otps").doc(emailKey).delete();
    return res.status(410).json({ error: "Код истёк. Запросите новый." });
  }

  if ((data.attempts || 0) >= 5) {
    await db.collection("otps").doc(emailKey).delete();
    return res.status(429).json({ error: "Слишком много попыток. Запросите новый код." });
  }

  if (data.code !== code) {
    await db.collection("otps").doc(emailKey).update({
      attempts: admin.firestore.FieldValue.increment(1),
    });
    return res.status(401).json({ error: "Неверный код. Проверьте письмо и попробуйте снова." });
  }

  // Код верный — удаляем одноразовый OTP
  await db.collection("otps").doc(emailKey).delete();
  return res.status(200).json({ success: true });
};
