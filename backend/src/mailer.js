const nodemailer = require('nodemailer');

function createTransporter() {
  const required = ['SMTP_HOST', 'SMTP_USER', 'SMTP_PASS', 'MAIL_FROM'];
  const missing = required.filter((key) => !process.env[key]);
  if (missing.length) {
    throw new Error(`Konfigurasi email belum lengkap: ${missing.join(', ')}`);
  }

  const port = Number(process.env.SMTP_PORT || 587);
  return nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port,
    secure: String(process.env.SMTP_SECURE || 'false').toLowerCase() === 'true' || port === 465,
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS
    }
  });
}

async function sendResetEmail(email, resetUrl) {
  const transporter = createTransporter();
  await transporter.sendMail({
    from: process.env.MAIL_FROM,
    to: email,
    subject: 'Reset password Katalog Film',
    text: `Kami menerima permintaan reset password akun Katalog Film. Buka tautan berikut dalam 15 menit: ${resetUrl}\n\nAbaikan email ini jika kamu tidak meminta reset password.`,
    html: `<!doctype html><html lang="id"><body style="font-family:Arial,sans-serif;color:#222;line-height:1.6"><h2>Reset password Katalog Film</h2><p>Kami menerima permintaan untuk mengganti password akunmu.</p><p><a href="${resetUrl}">Buat password baru</a></p><p>Tautan berlaku selama 15 menit. Abaikan email ini jika kamu tidak meminta reset password.</p></body></html>`
  });
}

module.exports = { sendResetEmail };
