<?php

declare(strict_types=1);

namespace App\Api\Middleware;

use App\Api\Utils\Response;
use App\Api\Utils\JwtHelper;
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
    // Ensure JWT secrets are loaded via credentials
    if (!defined('JWT_ACCESS_SECRET')) {
        error_log('JWT_ACCESS_SECRET missing from configuration.');
        Response::json(500, ['error' => 'Internal server error.']);
    }

    // Extract Token from HttpOnly Cookie (for Web Clients) OR Fallback to Authorization Header (for API tools like Postman)
    $jwt = $_COOKIE['access_token'] ?? null;

    if (!$jwt) {
        $headers = getallheaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;

        if ($authHeader && preg_match('/Bearer\s+(.*)$/i', $authHeader, $matches)) {
            $jwt = $matches[1];
        }
    }

    if (!$jwt) {
        Response::json(401, ['error' => 'Missing authentication token. Please log in.']);
    }

    try {
        // Decode and verify the signature using our custom zero-dependency helper
        $decoded = JwtHelper::decode($jwt, JWT_ACCESS_SECRET);

        // Return the payload
        return $decoded;
    } catch (Exception $e) {
        $msg = $e->getMessage();

        // Log expiration internally for debugging if needed
        if (strpos($msg, 'expired') !== false) {
            error_log("JWT Expired: " . $msg);
            Response::json(401, ['error' => 'Token has expired.']);
        }

        // Log invalid signatures as potential tampering attempts
        if (strpos($msg, 'signature') !== false) {
            error_log("JWT Invalid Signature: " . $msg);
            Response::json(401, ['error' => 'Invalid token signature.']);
        }

        // Log other JWT decoding exceptions securely
        error_log("JWT Decode Error: " . $msg);
        Response::json(401, ['error' => 'Unauthorized or invalid token.']);
    }
}
