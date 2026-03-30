<?php

declare(strict_types=1);

namespace App\Api\Utils;

class Response
{
    /**
     * Send a JSON response with the appropriate headers.
     *
     * @param int $statusCode The HTTP status code
     * @param array $data The data to encode as JSON
     */
    public static function json(int $statusCode, array $data): void
    {
        // Load allowed origin from environment or default to '*'
        $allowedOrigin = $_ENV['ALLOWED_ORIGIN'] ?? '*';

        header("Access-Control-Allow-Origin: " . $allowedOrigin);
        header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
        header("Access-Control-Allow-Headers: Content-Type, Authorization");
        header("Access-Control-Allow-Credentials: true"); // REQUIRED for HttpOnly cookies
        header("Content-Type: application/json; charset=UTF-8");
        header("Access-Control-Max-Age: 3600");

        // Handle preflight requests
        if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
            http_response_code(200);
            exit();
        }

        http_response_code($statusCode);
        echo json_encode($data);
        exit();
    }
}
