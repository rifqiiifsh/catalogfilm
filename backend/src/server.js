require('dotenv').config();

const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const jwt = require('jsonwebtoken');
const pool = require('./db');
const authRoutes = require('./routes/auth');

const app = express();
const port = Number(process.env.PORT || 3000);

if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
  throw new Error('JWT_SECRET wajib diisi dengan string acak minimal 32 karakter.');
}

app.disable('x-powered-by');
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: true, credentials: false }));
app.use(express.json({ limit: '20kb' }));

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: 'draft-8',
  legacyHeaders: false,
  message: { message: 'Terlalu banyak permintaan. Coba lagi beberapa menit.' }
});

app.get('/api/health', async (req, res, next) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', service: 'katalog-film-api', database: 'connected' });
  } catch (error) {
    next(error);
  }
});

app.use('/api/auth', authLimiter, authRoutes);

app.get('/reset-password', (req, res) => {
  res.type('html').send(`<!doctype html>
<html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Reset Password - Katalog Film</title>
<style>body{font-family:Arial,sans-serif;background:#10131b;color:#f5f5f5;display:grid;min-height:100vh;place-items:center;margin:0;padding:20px}.card{width:min(420px,100%);background:#1d2330;padding:28px;border-radius:18px;box-sizing:border-box}input,button{box-sizing:border-box;width:100%;padding:14px;border-radius:10px;margin-top:12px;font-size:16px}input{border:1px solid #51596b;background:#111722;color:#fff}button{border:0;background:#e0a84f;color:#1b1b1b;font-weight:bold;cursor:pointer}p{color:#bac1ce;line-height:1.5}#status{margin-top:14px}</style></head>
<body><main class="card"><h2>Buat password baru</h2><p>Masukkan password baru untuk akun Katalog Film. Minimal 8 karakter.</p>
<form id="resetForm"><input id="password" type="password" minlength="8" maxlength="72" autocomplete="new-password" placeholder="Password baru" required><input id="confirm" type="password" minlength="8" maxlength="72" autocomplete="new-password" placeholder="Ulangi password baru" required><button type="submit">Simpan password</button></form><p id="status" role="status"></p></main>
<script>const form=document.getElementById('resetForm');const statusText=document.getElementById('status');form.addEventListener('submit',async(event)=>{event.preventDefault();const password=document.getElementById('password').value;const confirm=document.getElementById('confirm').value;if(password!==confirm){statusText.textContent='Password tidak sama.';return;}const token=new URLSearchParams(location.search).get('token')||'';try{const response=await fetch('/api/auth/reset-password',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token,password})});const data=await response.json();statusText.textContent=data.message||'Permintaan selesai.';if(response.ok)form.remove();}catch(error){statusText.textContent='Tidak bisa terhubung ke server. Coba lagi.';}});</script></body></html>`);
});

app.get('/api/me', (req, res) => {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  if (!token) return res.status(401).json({ message: 'Token autentikasi wajib dikirim.' });
  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET, { issuer: 'katalog-film-api' });
    return res.json({ userId: Number(payload.sub), email: payload.email });
  } catch (_) {
    return res.status(401).json({ message: 'Sesi tidak valid atau sudah kedaluwarsa.' });
  }
});

app.use((req, res) => res.status(404).json({ message: 'Endpoint tidak ditemukan.' }));
app.use((error, req, res, next) => {
  console.error(error);
  if (res.headersSent) return next(error);
  const status = error.code === 'ER_CON_COUNT_ERROR' ? 503 : 500;
  const message = process.env.NODE_ENV === 'production'
    ? 'Terjadi kesalahan pada server.'
    : (error.message || 'Terjadi kesalahan pada server.');
  res.status(status).json({ message });
});

async function start() {
  await pool.query('SELECT 1');
  app.listen(port, '0.0.0.0', () => {
    console.log(`Katalog Film API aktif di http://0.0.0.0:${port}`);
  });
}

start().catch((error) => {
  console.error('Backend gagal dijalankan:', error.message);
  process.exit(1);
});
