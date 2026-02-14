package com.hieltech.haramblur.data.api

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp Interceptor that manages OAuth2 Client Credentials flow for Quran Foundation API.
 * - Fetches tokens from the auth endpoint using Basic auth with client_id:client_secret
 * - Caches tokens in memory with a 30-second buffer before expiry
 * - Adds x-auth-token and x-client-id headers to every API request
 * - On 401: clears cache, re-fetches token, retries the request once
 */
class QuranAuthInterceptor(
    private val clientId: String,
    private val clientSecret: String,
    private val authBaseUrl: String = "https://oauth2.quran.foundation"
) : Interceptor {

    companion object {
        private const val TAG = "QuranAuthInterceptor"
        private const val EXPIRY_BUFFER_SECONDS = 30L
    }

    private val gson = Gson()
    private val tokenClient = OkHttpClient.Builder().build()

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAt: Long = 0L

    data class TokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("token_type") val tokenType: String?,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("scope") val scope: String?
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            Log.w(TAG, "Quran API credentials not configured — skipping auth")
            return chain.proceed(chain.request())
        }

        val token = getValidToken() ?: return chain.proceed(chain.request())
        val authedRequest = chain.request().newBuilder()
            .addHeader("x-auth-token", token)
            .addHeader("x-client-id", clientId)
            .build()

        val response = chain.proceed(authedRequest)

        // On 401, clear cache, refresh token, retry once
        if (response.code == 401) {
            Log.w(TAG, "Got 401 — refreshing token and retrying")
            response.close()
            synchronized(this) {
                cachedToken = null
                tokenExpiresAt = 0L
            }
            val freshToken = getValidToken() ?: return chain.proceed(chain.request())
            val retryRequest = chain.request().newBuilder()
                .addHeader("x-auth-token", freshToken)
                .addHeader("x-client-id", clientId)
                .build()
            return chain.proceed(retryRequest)
        }

        return response
    }

    @Synchronized
    private fun getValidToken(): String? {
        val now = System.currentTimeMillis() / 1000
        if (cachedToken != null && now < tokenExpiresAt) {
            return cachedToken
        }
        return fetchNewToken()
    }

    private fun fetchNewToken(): String? {
        try {
            val credentials = "$clientId:$clientSecret"
            val basicAuth = "Basic " + Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
            )

            val body = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("scope", "content")
                .build()

            val request = Request.Builder()
                .url("$authBaseUrl/oauth2/token")
                .post(body)
                .addHeader("Authorization", basicAuth)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = tokenClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                val now = System.currentTimeMillis() / 1000
                cachedToken = tokenResponse.accessToken
                tokenExpiresAt = now + tokenResponse.expiresIn - EXPIRY_BUFFER_SECONDS
                Log.d(TAG, "Token obtained, expires in ${tokenResponse.expiresIn}s")
                return cachedToken
            } else {
                Log.e(TAG, "Token request failed: ${response.code} — $responseBody")
                return null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Token fetch IO error", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Token fetch error", e)
            return null
        }
    }
}

