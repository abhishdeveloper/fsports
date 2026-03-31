<?php

declare(strict_types=1);

/**
 * ----------------------------------------------------------------------
 * SECURE PRODUCTION CONFIGURATION FILE
 * ----------------------------------------------------------------------
 * Edit this file ONCE before uploading it to your cPanel/Shared Hosting.
 * No SSH, no Composer, no .env required.
 *
 * IMPORTANT: This file is ignored by Git. Do not commit your real passwords.
 */

// 1. Database Settings (Get these from cPanel MySQL Databases)
define('DB_HOST', 'localhost');          // Usually localhost on shared hosting
define('DB_PORT', '3306');
define('DB_NAME', 'college_sports');     // e.g., 'yourusername_collegesports'
define('DB_USER', 'root');               // e.g., 'yourusername_admin'
define('DB_PASS', 'secret');             // Your highly secure DB password
define('DB_CHARSET', 'utf8mb4');

// 2. Authentication Settings
// Generate a long, random string (e.g., 64 characters) for these.
define('JWT_ACCESS_SECRET', 'YOUR_SUPER_SECRET_ACCESS_KEY_FOR_JWT_THAT_IS_LONG_ENOUGH_TO_BE_SECURE');
define('JWT_REFRESH_SECRET', 'YOUR_SUPER_SECRET_REFRESH_KEY_FOR_JWT_THAT_IS_LONG_ENOUGH_TO_BE_SECURE');

// 3. Security / CORS Settings
// Enter your live domain (e.g., 'https://play.abhish.in') or '*' for local testing.
define('ALLOWED_ORIGIN', '*');