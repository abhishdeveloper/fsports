<?php

declare(strict_types=1);

namespace App\Config;

use PDO;
use PDOException;
use Exception;

class Database
{
    private static ?PDO $instance = null;

    private function __construct()
    {
        // Private constructor to prevent direct instantiation (Singleton pattern)
    }

    private function __clone()
    {
        // Private clone to prevent cloning of the instance
    }

    public function __wakeup()
    {
        // Prevent unserialization
        throw new Exception("Cannot unserialize a singleton.");
    }

    /**
     * Get the database connection instance
     *
     * @return PDO
     * @throws PDOException if connection fails
     */
    public static function getConnection(): PDO
    {
        if (self::$instance === null) {
            $host = $_ENV['DB_HOST'] ?? '127.0.0.1';
            $port = $_ENV['DB_PORT'] ?? '3306';
            $dbName = $_ENV['DB_NAME'] ?? '';
            $charset = $_ENV['DB_CHARSET'] ?? 'utf8mb4';
            $user = $_ENV['DB_USER'] ?? '';
            $pass = $_ENV['DB_PASS'] ?? '';

            if (empty($dbName) || empty($user)) {
                // Do not output detailed errors to the client in production
                error_log("Database configuration is incomplete.");
                header('HTTP/1.1 500 Internal Server Error');
                exit('A critical configuration error occurred.');
            }

            $dsn = sprintf(
                "mysql:host=%s;port=%s;dbname=%s;charset=%s",
                $host,
                $port,
                $dbName,
                $charset
            );

            $options = [
                // Strict error handling
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                // Default fetch mode as associative arrays
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                // Crucial for security: Turn off emulated prepared statements
                PDO::ATTR_EMULATE_PREPARES   => false,
                // Ensure the connection doesn't hang indefinitely
                PDO::ATTR_TIMEOUT            => 5,
            ];

            try {
                self::$instance = new PDO($dsn, $user, $pass, $options);
            } catch (PDOException $e) {
                // Log the actual error, but don't expose it to the client
                error_log("Database Connection Error: " . $e->getMessage());
                header('HTTP/1.1 500 Internal Server Error');
                exit('A database connection error occurred.');
            }
        }

        return self::$instance;
    }
}
