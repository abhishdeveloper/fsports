<?php

declare(strict_types=1);

namespace App\Api\Auth;

use App\Config\Database;
use App\Api\Utils\Response;
use Firebase\JWT\JWT;
use PDOException;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';

// Only allow POST requests
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// Ensure JWT secrets are loaded via Dotenv
if (!isset($_ENV['JWT_ACCESS_SECRET']) || !isset($_ENV['JWT_REFRESH_SECRET'])) {
    // Log configuration error internally
    error_log('JWT secrets missing from environment variables.');
    Response::json(500, ['error' => 'Internal server error.']);
}

// Fetch JSON payload
// In CLI testing, read from STDIN instead of php://input
$inputStream = (php_sapi_name() === 'cli') ? 'php://stdin' : 'php://input';
$inputJSON = file_get_contents($inputStream);
$input = json_decode($inputJSON, true);

if (!$input) {
    Response::json(400, ['error' => 'Invalid JSON payload']);
}

$email = filter_var($input['email'] ?? '', FILTER_SANITIZE_EMAIL);
$password = $input['password'] ?? '';

// Ensure required fields
if (empty($email) || empty($password)) {
    Response::json(400, ['error' => 'Email and password are required.']);
}

// Securely handle endpoint paths by stripping query parameters to maintain clean logs
$requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/api/auth/login', PHP_URL_PATH);
// Sanitize IP
$ipAddress = filter_var($_SERVER['REMOTE_ADDR'] ?? '0.0.0.0', FILTER_VALIDATE_IP) ?: '0.0.0.0';

try {
    $pdo = Database::getConnection();

    // Verify User and Password
    $stmt = $pdo->prepare("SELECT id, username, password_hash, account_status, role FROM users WHERE email = :email LIMIT 1");
    $stmt->execute([':email' => $email]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password_hash'])) {
        // Log the failed login attempt
        logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
        Response::json(401, ['error' => 'Invalid credentials']); // Generic response
    }

    // Check account status
    if ($user['account_status'] !== 'active') {
        logSecurityEvent($pdo, $ipAddress, $requestUri, 'failed');
        Response::json(403, ['error' => 'Account is ' . htmlspecialchars($user['account_status'])]);
    }

    // Generate JWT Tokens
    $now = time();
    $accessExpiration = $now + (15 * 60); // 15 minutes
    $refreshExpiration = $now + (7 * 24 * 60 * 60); // 7 days

    $accessTokenPayload = [
        'iss' => 'college-sports-app',
        'sub' => $user['id'],
        'username' => $user['username'],
        'iat' => $now,
        'exp' => $accessExpiration,
    ];

    $refreshTokenPayload = [
        'iss' => 'college-sports-app',
        'sub' => $user['id'],
        'iat' => $now,
        'exp' => $refreshExpiration,
    ];

    $accessToken = JWT::encode($accessTokenPayload, $_ENV['JWT_ACCESS_SECRET'], 'HS256');
    $refreshToken = JWT::encode($refreshTokenPayload, $_ENV['JWT_REFRESH_SECRET'], 'HS256');

    // Store the refresh token securely in the DB
    $updateStmt = $pdo->prepare("UPDATE users SET refresh_token = :refresh_token WHERE id = :id");
    $updateStmt->execute([
        ':refresh_token' => $refreshToken,
        ':id' => $user['id']
    ]);

    // Log the successful login attempt
    logSecurityEvent($pdo, $ipAddress, $requestUri, 'success');

    // Set Access Token as an HttpOnly, Secure cookie for XSS protection in Web Browsers
    setcookie(
        'access_token',
        $accessToken,
        [
            'expires' => $accessExpiration,
            'path' => '/',
            'domain' => $_SERVER['HTTP_HOST'] ?? '',
            'secure' => isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off',
            'httponly' => true,
            'samesite' => 'Strict'
        ]
    );

    // Set Refresh Token as an HttpOnly, Secure cookie
    setcookie(
        'refresh_token',
        $refreshToken,
        [
            'expires' => $refreshExpiration,
            'path' => '/',
            'domain' => $_SERVER['HTTP_HOST'] ?? '',
            'secure' => isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off',
            'httponly' => true,
            'samesite' => 'Strict'
        ]
    );

    // Return success to the web client
    Response::json(200, [
        'message' => 'Login successful',
        'expires_in' => 15 * 60, // seconds
        'role' => $user['role']
    ]);

} catch (PDOException $e) {
    // Log unexpected database errors securely
    error_log("Login DB Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred.']);
} catch (Exception $e) {
    // Log unexpected general errors securely
    error_log("Login General Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An unexpected error occurred.']);
}

/**
 * Log the security event asynchronously
 */
function logSecurityEvent(\PDO $pdo, string $ipAddress, string $endpoint, string $status): void
{
    try {
        $stmt = $pdo->prepare("INSERT INTO security_logs (ip_address, endpoint_accessed, status) VALUES (:ip, :endpoint, :status)");
        $stmt->execute([
            ':ip' => $ipAddress,
            ':endpoint' => $endpoint,
            ':status' => $status
        ]);
    } catch (\Exception $e) {
        // Do not interrupt the flow if logging fails
        error_log("Failed to insert security log: " . $e->getMessage());
    }
}
