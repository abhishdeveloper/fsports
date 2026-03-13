<?php

declare(strict_types=1);

namespace App\Api\Auth;

use App\Config\Database;
use App\Api\Utils\Response;
use PDOException;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';

// Only allow POST requests
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// Fetch JSON payload
// In CLI testing, read from STDIN instead of php://input
$inputStream = (php_sapi_name() === 'cli') ? 'php://stdin' : 'php://input';
$inputJSON = file_get_contents($inputStream);
$input = json_decode($inputJSON, true);

if (!$input) {
    Response::json(400, ['error' => 'Invalid JSON payload']);
}

// Extract and sanitize inputs
$username = filter_var($input['username'] ?? '', FILTER_SANITIZE_FULL_SPECIAL_CHARS);
$email = filter_var($input['email'] ?? '', FILTER_SANITIZE_EMAIL);
$rollNumber = filter_var($input['roll_number'] ?? '', FILTER_SANITIZE_FULL_SPECIAL_CHARS);
$branch = filter_var($input['branch'] ?? '', FILTER_SANITIZE_FULL_SPECIAL_CHARS);
$password = $input['password'] ?? '';

// Validate required fields
if (empty($username) || empty($email) || empty($rollNumber) || empty($branch) || empty($password)) {
    Response::json(400, ['error' => 'All fields are required.']);
}

// Validate email format strictly
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    Response::json(400, ['error' => 'Invalid email format.']);
}

// Check password strength (e.g., minimum 8 chars)
if (strlen($password) < 8) {
    Response::json(400, ['error' => 'Password must be at least 8 characters long.']);
}

try {
    $pdo = Database::getConnection();

    // Hash the password using Argon2id for maximum security
    $passwordHash = password_hash($password, PASSWORD_ARGON2ID);

    // Prepare statement to insert new user
    // We rely on unique database constraints (idx_email, idx_roll_number, idx_username) to prevent duplicates
    $stmt = $pdo->prepare(
        "INSERT INTO users (username, email, roll_number, branch, password_hash)
         VALUES (:username, :email, :roll_number, :branch, :password_hash)"
    );

    $stmt->execute([
        ':username' => $username,
        ':email' => $email,
        ':roll_number' => $rollNumber,
        ':branch' => $branch,
        ':password_hash' => $passwordHash,
    ]);

    Response::json(201, ['message' => 'User registered successfully.']);

} catch (PDOException $e) {
    // Check for MySQL duplicate entry error code (1062)
    if ($e->getCode() === '23000' && strpos($e->getMessage(), '1062') !== false) {
        // Find which field triggered the duplicate entry error
        if (strpos($e->getMessage(), 'idx_email') !== false) {
            $conflictMessage = 'Email already exists.';
        } elseif (strpos($e->getMessage(), 'idx_roll_number') !== false) {
            $conflictMessage = 'Roll number already exists.';
        } elseif (strpos($e->getMessage(), 'idx_username') !== false) {
            $conflictMessage = 'Username already exists.';
        } else {
            $conflictMessage = 'A user with these credentials already exists.';
        }
        Response::json(409, ['error' => $conflictMessage]);
    }

    // Log unexpected database errors securely
    error_log("Registration DB Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred.']);

} catch (Exception $e) {
    // Log unexpected general errors securely
    error_log("Registration General Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An unexpected error occurred.']);
}
