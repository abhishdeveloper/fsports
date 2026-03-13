<?php

declare(strict_types=1);

namespace App\Config;

use PDO;
use PDOException;
use Dotenv\Dotenv;
use Exception;

// Assuming db_connect.php is in the config/ directory and vendor is in the root
$autoloadPath = __DIR__ . '/../vendor/autoload.php';

if (!file_exists($autoloadPath)) {
    // Fail securely if autoload is missing, preventing further execution
    header('HTTP/1.1 500 Internal Server Error');
    exit('Autoloader not found. Please run composer install.');
}

require_once $autoloadPath;

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
            self::loadEnvironmentVariables();

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

    /**
     * Securely load environment variables using Dotenv
     */
    private static function loadEnvironmentVariables(): void
    {
        // Path to the directory containing the .env file (root directory)
        $envPath = __DIR__ . '/../';

        if (file_exists($envPath . '.env')) {
            try {
                $dotenv = Dotenv::createImmutable($envPath);
                $dotenv->load();

                // Validate required variables
                $dotenv->required(['DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASS']);
            } catch (Exception $e) {
                error_log("Dotenv Error: " . $e->getMessage());
                header('HTTP/1.1 500 Internal Server Error');
                exit('A configuration loading error occurred.');
            }
        } else {
            // Environment variables might be set directly by the server (e.g., Apache/Nginx, Docker)
            // It's acceptable to not have a .env file if they are provided by the environment,
            // but for safety, we ensure the minimal required vars exist in $_ENV or getenv()

            // In case of fastcgi or direct environment variables, they might be in $_SERVER or getenv
            if (!isset($_ENV['DB_NAME'])) {
                $_ENV['DB_HOST'] = getenv('DB_HOST');
                $_ENV['DB_PORT'] = getenv('DB_PORT');
                $_ENV['DB_NAME'] = getenv('DB_NAME');
                $_ENV['DB_USER'] = getenv('DB_USER');
                $_ENV['DB_PASS'] = getenv('DB_PASS');
                $_ENV['DB_CHARSET'] = getenv('DB_CHARSET');
            }
        }
    }
}
