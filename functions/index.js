const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

const db = admin.firestore();

function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

function getSmsApiKey() {
  try {
    return functions.config().smsru.key;
  } catch (_) {
    return null;
  }
}

// Отправить OTP на номер телефона
exports.sendOtp = functions.https.onCall(async (data) => {
  const phone = data.phone;

  if (!phone || !/^\+[1-9]\d{10,14}$/.test(phone)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Неверный формат номера. Используйте +7XXXXXXXXXX"
    );
  }

  // Ограничение: не чаще 1 раза в минуту
  const existing = await db.collection("otps").doc(phone).get();
  if (existing.exists) {
    const sentAt = existing.data().sentAt?.toMillis() || 0;
    if (Date.now() - sentAt < 60 * 1000) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "Подождите минуту перед повторной отправкой"
      );
    }
  }

  const otp = generateOtp();
  const expiry = admin.firestore.Timestamp.fromDate(
    new Date(Date.now() + 5 * 60 * 1000)
  );

  await db.collection("otps").doc(phone).set({
    code: otp,
    expiry: expiry,
    sentAt: admin.firestore.Timestamp.now(),
    attempts: 0
  });

  const apiKey = getSmsApiKey();

  if (!apiKey) {
    // Тестовый режим: код сохранён в Firestore, SMS не отправляется
    // Для проверки: Firebase Console → Firestore → otps → {номер} → code
    console.log(`[ТЕСТ] OTP для ${phone}: ${otp}`);
    return { success: true, testMode: true };
  }

  // Отправка через SMS.ru
  const smsPhone = phone.replace("+", "");
  const message = `Код для входа в Медцентр «8 за»: ${otp}. Действителен 5 минут.`;

  try {
    const response = await axios.get("https://sms.ru/sms/send", {
      params: {
        api_id: apiKey,
        to: smsPhone,
        msg: message,
        json: 1
      },
      timeout: 10000
    });

    const result = response.data;
    if (result.status !== "OK") {
      const errText =
        result.sms?.[smsPhone]?.status_text ||
        result.status_text ||
        "Ошибка SMS.ru";
      throw new Error(errText);
    }

    return { success: true };
  } catch (error) {
    console.error("SMS sending failed:", error.message);
    throw new functions.https.HttpsError(
      "internal",
      "Не удалось отправить SMS. Проверьте номер и попробуйте ещё раз."
    );
  }
});

// Проверить OTP и вернуть Custom Token
exports.verifyOtp = functions.https.onCall(async (data) => {
  const { phone, code } = data;

  if (!phone || !code) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Номер телефона и код обязательны"
    );
  }

  const otpDoc = await db.collection("otps").doc(phone).get();

  if (!otpDoc.exists) {
    throw new functions.https.HttpsError(
      "not-found",
      "Код не найден. Запросите новый."
    );
  }

  const otpData = otpDoc.data();

  if (Date.now() > otpData.expiry.toMillis()) {
    await db.collection("otps").doc(phone).delete();
    throw new functions.https.HttpsError(
      "deadline-exceeded",
      "Код истёк. Запросите новый."
    );
  }

  if ((otpData.attempts || 0) >= 5) {
    await db.collection("otps").doc(phone).delete();
    throw new functions.https.HttpsError(
      "resource-exhausted",
      "Слишком много попыток. Запросите новый код."
    );
  }

  if (otpData.code !== code) {
    await db.collection("otps").doc(phone).update({
      attempts: admin.firestore.FieldValue.increment(1)
    });
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Неверный код. Проверьте SMS и попробуйте снова."
    );
  }

  // Удалить использованный OTP
  await db.collection("otps").doc(phone).delete();

  // Стабильный UID на основе номера телефона
  const uid = "u_" + phone.replace(/[^0-9]/g, "");

  // Выдать Firebase Custom Token
  const customToken = await admin.auth().createCustomToken(uid, {
    phone: phone
  });

  return { token: customToken, uid: uid };
});
