<?php

declare(strict_types=1);

namespace App\Api\Matches;

use App\Config\Database;
use App\Api\Utils\Response;
use PDO;
use Exception;

// Bootstrap Application
require_once __DIR__ . '/../../bootstrap.php';
// Require Middleware
require_once __DIR__ . '/../Middleware/auth_middleware.php';

use function App\Api\Middleware\authenticateJWT;

// Only allow GET requests
if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'GET') {
    Response::json(405, ['error' => 'Method Not Allowed']);
}

// 1. Authenticate Request
$userPayload = authenticateJWT();

// 2. Validate URL Parameter
$matchIdStr = $_GET['match_id'] ?? null;

if ($matchIdStr === null || !filter_var($matchIdStr, FILTER_VALIDATE_INT)) {
    Response::json(400, ['error' => 'A valid match_id integer is required.']);
}

$matchId = (int) $matchIdStr;

try {
    $pdo = Database::getConnection();

    // 3. Fetch questions, explicitly excluding `correct_option` for security
    $query = "
        SELECT
            id AS question_id,
            question_text,
            option_a,
            option_b,
            option_c,
            option_d,
            points_multiplier
        FROM questions
        WHERE match_id = :match_id
        ORDER BY created_at ASC
    ";

    $stmt = $pdo->prepare($query);
    $stmt->execute([':match_id' => $matchId]);

    $questions = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (!$questions) {
        // Option 1: Return a 404. Option 2: Return an empty array.
        // For RESTful list endpoints, returning an empty array is usually preferred.
        Response::json(200, ['data' => []]);
    }

    Response::json(200, ['data' => $questions]);

} catch (Exception $e) {
    error_log("Questions Fetch Error: " . $e->getMessage());
    Response::json(500, ['error' => 'An internal server error occurred.']);
}
