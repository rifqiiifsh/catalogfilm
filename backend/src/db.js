const mysql = require('mysql2/promise');

const pool = mysql.createPool({
  // Accept both the project's DB_* names and Railway MySQL service variables.
  // On Railway, define DB_HOST/DB_PORT/DB_USER/DB_PASSWORD as references to
  // the MySQL service variables and keep DB_NAME=katalog_film.
  host: process.env.DB_HOST || process.env.MYSQLHOST || '127.0.0.1',
  port: Number(process.env.DB_PORT || process.env.MYSQLPORT || 3306),
  user: process.env.DB_USER || process.env.MYSQLUSER || 'root',
  password: process.env.DB_PASSWORD || process.env.MYSQLPASSWORD || '',
  database: process.env.DB_NAME || 'katalog_film',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0,
  charset: 'utf8mb4',
  timezone: 'Z',
  decimalNumbers: true
});

module.exports = pool;
