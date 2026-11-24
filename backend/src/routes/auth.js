const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const crypto = require('node:crypto');
const { OAuth2Client } = require('google-auth-library');
const pool = require('../db');
const { sendResetEmail } = require('../mailer');

const router = express.Router();
const googleClient = new OAuth2Client();
const SALT_ROUNDS = 12;
const RESET_MINUTES = 15;

function normaliseEmail(value) {
  return String(value || '').trim().toLowerCase();
}

function validEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) && value.length <= 190;
}

function passwordIsValid(value) {
  return typeof value === 'string' && value.length >= 8 && Buffer.byteLength(value, 'utf8') <= 72;
}

function createToken(user) {
  return jwt.sign(
    { email: user.email },
    process.env.JWT_SECRET,
    { subject: String(user.id), expiresIn: process.env.JWT_EXPIRES_IN || '7d', issuer: 'katalog-film-api' }
  );
}

function publicUser(user) {
  return { id: Number(user.id), name: user.name, email: user.email };
}

function authResult(user, message) {
  return { message, token: createToken(user), user: publicUser(user) };
}

router.post('/register', async (req, res, next) => {
  try {
    const name = String(req.body.name || '').trim();
    const email = normaliseEmail(req.body.email);
    const password = req.body.password;

    if (name.length < 2 || name.length > 120) {
      return res.status(400).json({ message: 'Nama harus terdiri dari 2 sampai 120 karakter.' });
    }
    if (!validEmail(email)) return res.status(400).json({ message: 'Format email tidak valid.' });
    if (!passwordIsValid(password)) {
      return res.status(400).json({ message: 'Password harus 8 sampai 72 karakter.' });
    }

    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
    const [result] = await pool.execute(
      'INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)',
      [name, email, passwordHash]
    );
    const user = { id: result.insertId, name, email };
    return res.status(201).json(authResult(user, 'Akun berhasil dibuat.'));
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ message: 'Email sudah terdaftar. Silakan masuk.' });
    }
    return next(error);
  }
});

router.post('/login', async (req, res, next) => {
  try {
    const email = normaliseEmail(req.body.email);
    const password = req.body.password;
    if (!validEmail(email) || typeof password !== 'string') {
      return res.status(400).json({ message: 'Email atau password belum valid.' });
    }

    const [rows] = await pool.execute(
      'SELECT id, name, email, password_hash FROM users WHERE email = ? LIMIT 1',
      [email]
    );
    const user = rows[0];
    if (!user) return res.status(401).json({ message: 'Email atau password salah.' });
    if (!user.password_hash) {
      return res.status(401).json({ message: 'Akun ini menggunakan Google. Lanjutkan dengan Google.' });
    }
    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) return res.status(401).json({ message: 'Email atau password salah.' });
    return res.json(authResult(user, 'Login berhasil.'));
  } catch (error) {
    return next(error);
  }
});

router.post('/google', async (req, res, next) => {
  try {
    if (!process.env.GOOGLE_WEB_CLIENT_ID) {
      return res.status(503).json({ message: 'Google OAuth belum dikonfigurasi pada backend.' });
    }
    const idToken = String(req.body.idToken || '');
    if (!idToken) return res.status(400).json({ message: 'Google ID token wajib dikirim.' });

    let ticket;
    try {
      ticket = await googleClient.verifyIdToken({
        idToken,
        audience: process.env.GOOGLE_WEB_CLIENT_ID
      });
    } catch (_) {
      return res.status(401).json({ message: 'Google ID token tidak valid atau kedaluwarsa.' });
    }
    const payload = ticket.getPayload();
    if (!payload || !payload.sub || !payload.email || payload.email_verified !== true) {
      return res.status(401).json({ message: 'Identitas Google tidak valid atau email belum terverifikasi.' });
    }

    const googleSub = payload.sub;
    const email = normaliseEmail(payload.email);
    if (!validEmail(email)) {
      return res.status(401).json({ message: 'Alamat email dari akun Google tidak valid.' });
    }
    const name = String(payload.name || email.split('@')[0]).slice(0, 120);

    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();
      const [byGoogle] = await connection.execute(
        'SELECT id, name, email FROM users WHERE google_sub = ? LIMIT 1 FOR UPDATE',
        [googleSub]
      );
      let user = byGoogle[0];
      if (!user) {
        const [byEmail] = await connection.execute(
          'SELECT id, name, email, google_sub FROM users WHERE email = ? LIMIT 1 FOR UPDATE',
          [email]
        );
        if (byEmail[0]) {
          user = byEmail[0];
          if (user.google_sub && user.google_sub !== googleSub) {
            await connection.rollback();
            return res.status(409).json({ message: 'Email sudah terhubung ke akun Google lain.' });
          }
          const googleControlsEmail = email.endsWith('@gmail.com') || Boolean(payload.hd);
          if (!user.google_sub && !googleControlsEmail) {
            await connection.rollback();
            return res.status(409).json({ message: 'Masuk dengan email dan password terlebih dahulu untuk menghubungkan akun ini ke Google.' });
          }
          await connection.execute(
            'UPDATE users SET google_sub = ?, name = ? WHERE id = ?',
            [googleSub, name, user.id]
          );
          user.name = name;
        } else {
          const [inserted] = await connection.execute(
            'INSERT INTO users (name, email, google_sub) VALUES (?, ?, ?)',
            [name, email, googleSub]
          );
          user = { id: inserted.insertId, name, email };
        }
      }
      await connection.commit();
      return res.json(authResult(user, 'Login Google berhasil.'));
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  } catch (error) {
    if (error.name === 'Error' && /token|audience|jwt/i.test(error.message)) {
      return res.status(401).json({ message: 'Google ID token tidak valid atau kedaluwarsa.' });
    }
    return next(error);
  }
});

router.post('/forgot-password', async (req, res, next) => {
  try {
    const email = normaliseEmail(req.body.email);
    if (!validEmail(email)) return res.status(400).json({ message: 'Format email tidak valid.' });

    const [rows] = await pool.execute(
      'SELECT id, email FROM users WHERE email = ? LIMIT 1',
      [email]
    );
    if (rows[0]) {
      const rawToken = crypto.randomBytes(32).toString('hex');
      const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
      await pool.execute(
        `UPDATE users SET reset_token_hash = ?,
         reset_token_expires_at = DATE_ADD(UTC_TIMESTAMP(), INTERVAL ${RESET_MINUTES} MINUTE)
         WHERE id = ?`,
        [tokenHash, rows[0].id]
      );
      const base = String(process.env.PUBLIC_BASE_URL || '').replace(/\/$/, '');
      if (!base) throw new Error('PUBLIC_BASE_URL belum diatur.');
      const resetUrl = `${base}/reset-password?token=${rawToken}`;
      await sendResetEmail(email, resetUrl);
    }

    // Respons sama untuk email terdaftar maupun tidak agar email tidak bisa ditebak.
    return res.json({ message: 'Jika email terdaftar, tautan reset akan dikirim. Periksa kotak masuk dan folder spam.' });
  } catch (error) {
    return next(error);
  }
});

router.post('/reset-password', async (req, res, next) => {
  const token = String(req.body.token || '');
  const password = req.body.password;
  if (!/^[a-f0-9]{64}$/i.test(token)) {
    return res.status(400).json({ message: 'Tautan reset tidak valid atau sudah kedaluwarsa.' });
  }
  if (!passwordIsValid(password)) {
    return res.status(400).json({ message: 'Password baru harus 8 sampai 72 karakter.' });
  }

  const tokenHash = crypto.createHash('sha256').update(token).digest('hex');
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();
    const [rows] = await connection.execute(
      `SELECT id FROM users
       WHERE reset_token_hash = ? AND reset_token_expires_at > UTC_TIMESTAMP()
       LIMIT 1 FOR UPDATE`,
      [tokenHash]
    );
    if (!rows[0]) {
      await connection.rollback();
      return res.status(400).json({ message: 'Tautan reset tidak valid atau sudah kedaluwarsa.' });
    }
    const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
    await connection.execute(
      `UPDATE users SET password_hash = ?, reset_token_hash = NULL,
       reset_token_expires_at = NULL WHERE id = ?`,
      [passwordHash, rows[0].id]
    );
    await connection.commit();
    return res.json({ message: 'Password berhasil diperbarui. Silakan kembali ke aplikasi untuk login.' });
  } catch (error) {
    await connection.rollback();
    return next(error);
  } finally {
    connection.release();
  }
});

module.exports = router;
