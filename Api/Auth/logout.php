<?php

declare(strict_types=1);

namespace App\Api\Auth;

use App\Api\Utils\Response;

require_once __DIR__ . '/../../bootstrap.php';

// Only allow POST requests for state-changing actions
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// Expire the access_token securely
setcookie(
    'access_token',
    '',
    [
        'expires' => time() - 3600, // Expiration in the past
        'path' => '/',
        'domain' => $_SERVER['HTTP_HOST'] ?? '',
        'secure' => isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off',
        'httponly' => true,
        'samesite' => 'Strict'
    ]
);

// Expire the refresh_token securely
setcookie(
    'refresh_token',
    '',
    [
        'expires' => time() - 3600, // Expiration in the past
        'path' => '/',
        'domain' => $_SERVER['HTTP_HOST'] ?? '',
        'secure' => isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off',
        'httponly' => true,
        'samesite' => 'Strict'
    ]
);

Response::json(200, ['message' => 'Logged out successfully.']);