package com.college.sportsmeet.network

import com.college.sportsmeet.utils.TokenManager
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton providing the highly secure Retrofit API Service instance.
 * Employs Certificate Pinning and Bearer Token Injection via Interceptors.
 */
object ApiClient {

    private const val BASE_URL = "https://api.collegesports.com/"

    // Lazy initialization of the Retrofit service to ensure Context was already provided to TokenManager
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideSecureOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Constructs a highly secure OkHttpClient configuration.
     */
    private fun provideSecureOkHttpClient(): OkHttpClient {

        // 1. SSL Certificate Pinning
        // This is a critical security measure to prevent Man-In-The-Middle (MITM) attacks.
        // Replace "sha256/DUMMY_HASH=" with the actual SHA-256 Public Key Hash of your server's certificate.
        val certificatePinner = CertificatePinner.Builder()
            .add("api.collegesports.com", "sha256/DUMMY_HASH=")
            // Optionally pin a backup cert (e.g., intermediate CA) to avoid breaking app on cert rotation
            // .add("api.collegesports.com", "sha256/BACKUP_HASH=")
            .build()

        // 2. HTTP Logging Interceptor
        // Extremely useful for debugging, but be cautious not to log sensitive headers in production.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // For detailed payloads during dev
            // Redact the Authorization header from logs so JWTs don't leak into logcat
            redactHeader("Authorization")
        }

        // 3. Assemble the secure client
        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Interceptor to inject the JWT Access Token automatically into outgoing requests.
     */
    private class AuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest: Request = chain.request()

            // Some endpoints like login or register don't need a token.
            // A simple check can skip the token logic if needed, though most backends
            // simply ignore headers they don't explicitly require.
            if (originalRequest.url.encodedPath.contains("login.php") ||
                originalRequest.url.encodedPath.contains("register.php")) {
                return chain.proceed(originalRequest)
            }

            // Retrieve token securely from EncryptedSharedPreferences
            val token = TokenManager.getAccessToken()

            // If a token exists, add it to the Authorization header
            val modifiedRequest = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest // Proceed without token (let the backend return 401)
            }

            return chain.proceed(modifiedRequest)
        }
    }
}