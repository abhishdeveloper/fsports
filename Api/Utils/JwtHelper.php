<?php

declare(strict_types=1);

namespace App\Api\Utils;

use Exception;

/**
 * A lightweight, zero-dependency JWT Encoder/Decoder.
 * Designed specifically for Shared Hosting environments where Composer
 * is unavailable or blocked, replacing the need for 'firebase/php-jwt'.
 * Uses HS256 (HMAC-SHA256).
 */
class JwtHelper
{
    /**
     * Base64Url Encodes a string.
     */
    private static function base64UrlEncode(string $data): string
    {
        $b64 = base64_encode($data);
        if ($b64 === false) {
            return '';
        }
        $url = strtr($b64, '+/', '-_');
        return rtrim($url, '=');
    }

    /**
     * Base64Url Decodes a string.
     */
    private static function base64UrlDecode(string $data): string
    {
        $b64 = strtr($data, '-_', '+/');
        $padding = strlen($b64) % 4;
        if ($padding !== 0) {
            $b64 .= str_repeat('=', 4 - $padding);
        }
        $decoded = base64_decode($b64);
        return $decoded !== false ? $decoded : '';
    }

    /**
     * Encodes a payload into a valid JWT.
     *
     * @param array $payload The data to store in the token.
     * @param string $secret The secret key used for the HMAC signature.
     * @return string The signed JWT.
     */
    public static function encode(array $payload, string $secret): string
    {
        $header = json_encode(['typ' => 'JWT', 'alg' => 'HS256']);
        $payloadStr = json_encode($payload);

        if (!$header || !$payloadStr) {
            throw new Exception('JSON encoding failed.');
        }

        $base64UrlHeader = self::base64UrlEncode($header);
        $base64UrlPayload = self::base64UrlEncode($payloadStr);

        $signature = hash_hmac('sha256', $base64UrlHeader . "." . $base64UrlPayload, $secret, true);
        $base64UrlSignature = self::base64UrlEncode($signature);

        return $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    }

    /**
     * Decodes and verifies a JWT.
     *
     * @param string $jwt The token to verify.
     * @param string $secret The secret key to verify against.
     * @return array The decoded payload.
     * @throws Exception If the token is invalid, tampered with, or expired.
     */
    public static function decode(string $jwt, string $secret): array
    {
        $tokenParts = explode('.', $jwt);

        if (count($tokenParts) !== 3) {
            throw new Exception('Invalid token format.');
        }

        $header = self::base64UrlDecode($tokenParts[0]);
        $payload = self::base64UrlDecode($tokenParts[1]);
        $signatureProvided = $tokenParts[2];

        // Re-create the signature to verify
        $signatureCalculated = hash_hmac('sha256', $tokenParts[0] . "." . $tokenParts[1], $secret, true);
        $base64UrlSignatureCalculated = self::base64UrlEncode($signatureCalculated);

        // Verify the signature securely (timing attack safe)
        if (!hash_equals($base64UrlSignatureCalculated, $signatureProvided)) {
            throw new Exception('Invalid token signature.');
        }

        $payloadArray = json_decode($payload, true);

        if (!$payloadArray) {
             throw new Exception('Invalid token payload.');
        }

        // Verify Expiration ('exp' claim)
        if (isset($payloadArray['exp']) && $payloadArray['exp'] < time()) {
            throw new Exception('Token has expired.');
        }

        // Verify Not Before ('nbf' claim)
        if (isset($payloadArray['nbf']) && $payloadArray['nbf'] > time()) {
            throw new Exception('Token is not valid yet.');
        }

        return $payloadArray;
    }
}