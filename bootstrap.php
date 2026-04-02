<?php

declare(strict_types=1);

// ----------------------------------------------------------------------
// Global Error & Exception Handlers (Failsafe JSON Output)
// ----------------------------------------------------------------------
// Instead of a blank 500 HTML page, we catch all fatal errors and throw
// strict JSON so the frontend Javascript can actually read and display the issue.
ini_set('display_errors', '0');
error_reporting(E_ALL);

function customExceptionHandler($exception) {
    header('Content-Type: application/json; charset=UTF-8');
    http_response_code(500);
    echo json_encode([
        'error' => 'Critical System Error',
        'details' => $exception->getMessage()
    ]);
    exit;
}

function customErrorHandler($errno, $errstr, $errfile, $errline) {
    // Only throw Exceptions for fatal errors to be caught by our Exception Handler
    if (error_reporting() & $errno) {
        throw new \ErrorException($errstr, 0, $errno, $errfile, $errline);
    }
    return false;
}

set_exception_handler('customExceptionHandler');
set_error_handler('customErrorHandler');
register_shutdown_function(function() {
    $error = error_get_last();
    if ($error !== null && in_array($error['type'], [E_ERROR, E_CORE_ERROR, E_COMPILE_ERROR, E_PARSE])) {
        customExceptionHandler(new \ErrorException($error['message'], 0, $error['type'], $error['file'], $error['line']));
    }
});

// ----------------------------------------------------------------------
// Load Configurations
// ----------------------------------------------------------------------
$credentialsPath = __DIR__ . '/Config/credentials.php';
if (!file_exists($credentialsPath)) {
    throw new \Exception("Missing Config/credentials.php file. The application cannot start.");
}
require_once $credentialsPath;

/**
 * Custom Zero-Dependency PSR-4 Autoloader
 * Replaces Composer's vendor/autoload.php for Shared Hosting compatibility.
 * Automatically loads any class in the 'App\' namespace based on its directory path.
 */
spl_autoload_register(function ($class) {
    // Project-specific namespace prefix
    $prefix = 'App\\';

    // Base directory for the namespace prefix
    $base_dir = __DIR__ . '/';

    // Does the class use the namespace prefix?
    $len = strlen($prefix);
    if (strncmp($prefix, $class, $len) !== 0) {
        return; // No, move to the next registered autoloader
    }

    // Get the relative class name
    $relative_class = substr($class, $len);

    // CRITICAL: Linux (HestiaCP) is case-sensitive!
    // Our folders are named 'Api' and 'Config', but namespaces are typically standard cased
    // Make sure we convert backslashes to forward slashes correctly
    $file = $base_dir . str_replace('\\', '/', $relative_class) . '.php';

    // If the file exists, require it
    if (file_exists($file)) {
        require $file;
    } else {
        // Log explicitly if the autoloader fails to find the class on Linux
        error_log("Autoloader Error: Class '$class' not found at '$file'");
    }
});
