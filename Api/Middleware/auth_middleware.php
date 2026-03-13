<?php

declare(strict_types=1);

namespace App\Api\Middleware;

use App\Api\Utils\Response;
use Firebase\JWT\JWT;
use Firebase\JWT\Key;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';

/**
 * Validates the JWT Bearer token from the Authorization header.
 * Halts execution and returns 401 if invalid.
 *
 * @return array The decoded token payload as an associative array.
 */
function authenticateJWT(): array
{
    // Ensure JWT secrets are loaded via Dotenv
    if (!isset($_ENV['JWT_ACCESS_SECRET'])) {
        error_log('JWT_ACCESS_SECRET missing from environment variables.');
        Response::json(500, ['error' => 'Internal server error.']);
    }

    $headers = getallheaders();
    $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;

    if (!$authHeader) {
        Response::json(401, ['error' => 'Missing Authorization header.']);
    }

    // Extract Bearer token
    if (preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
        $jwt = $matches[1];
    } else {
        Response::json(401, ['error' => 'Invalid Authorization header format. Expected "Bearer <token>".']);
    }

    try {
        // Decode and verify the signature using the HS256 algorithm
        $decoded = JWT::decode($jwt, new Key($_ENV['JWT_ACCESS_SECRET'], 'HS256'));

        // Return the payload
        return (array) $decoded;
    } catch (\Firebase\JWT\ExpiredException $e) {
        // Log expiration internally for debugging if needed
        error_log("JWT Expired: " . $e->getMessage());
        Response::json(401, ['error' => 'Token has expired.']);
    } catch (\Firebase\JWT\SignatureInvalidException $e) {
        // Log invalid signatures as potential tampering attempts
        error_log("JWT Invalid Signature: " . $e->getMessage());
        Response::json(401, ['error' => 'Invalid token signature.']);
    } catch (Exception $e) {
        // Log other JWT decoding exceptions securely
        error_log("JWT Decode Error: " . $e->getMessage());
        Response::json(401, ['error' => 'Unauthorized or invalid token.']);
    }
}
