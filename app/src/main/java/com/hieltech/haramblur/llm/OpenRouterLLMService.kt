package com.hieltech.haramblur.llm

import android.util.Log
import com.hieltech.haramblur.detection.ContentAction
import com.hieltech.haramblur.detection.WarningLevel
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenRouter LLM Service for fast decision making
 * Uses OpenRouter API to make intelligent decisions about content actions
 */
@Singleton
class OpenRouterLLMService @Inject constructor() {
    
    companion object {
        private const val TAG = "OpenRouterLLMService"
        private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val DEFAULT_MODEL = "google/gemma-2-9b-it:free" // Fast and free model
        private const val REQUEST_TIMEOUT = 3000L // 3 seconds for fast decisions
        
        // Available models for user selection
        val AVAILABLE_MODELS = listOf(
            LLMModel("google/gemma-2-9b-it:free", "Gemma 2 9B (Free)", "Fast and free Google model"),
            LLMModel("microsoft/phi-3-mini-128k-instruct:free", "Phi-3 Mini (Free)", "Microsoft's efficient model"),
            LLMModel("meta-llama/llama-3.1-8b-instruct:free", "Llama 3.1 8B (Free)", "Meta's latest free model"),
            LLMModel("google/gemma-2-27b-it", "Gemma 2 27B (Paid)", "Larger, more capable model"),
            LLMModel("anthropic/claude-3-haiku", "Claude 3 Haiku (Paid)", "Anthropic's fast model"),
            LLMModel("openai/gpt-4o-mini", "GPT-4o Mini (Paid)", "OpenAI's efficient model")
        )
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(REQUEST_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(REQUEST_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(REQUEST_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()
    
    /**
     * Get fast decision from LLM about what action to take
     * @param nsfwRegionCount Number of NSFW regions detected
     * @param maxConfidence Maximum confidence of NSFW detection
     * @param contentDescription Brief description of the content context
     * @param currentApp Current app being used
     * @param apiKey OpenRouter API key
     * @return Recommended ContentAction with reasoning
     */
    suspend fun getFastDecision(
        nsfwRegionCount: Int,
        maxConfidence: Float,
        contentDescription: String = "web content",
        currentApp: String = "browser",
        apiKey: String
    ): LLMDecisionResult = withContext(Dispatchers.IO) {
        
        if (apiKey.isBlank()) {
            Log.w(TAG, "OpenRouter API key not provided - falling back to rule-based decision")
            return@withContext createFallbackDecision(nsfwRegionCount, maxConfidence)
        }
        
        try {
            Log.d(TAG, "🤖 Requesting LLM decision for $nsfwRegionCount regions with confidence $maxConfidence")
            
            val prompt = createDecisionPrompt(nsfwRegionCount, maxConfidence, contentDescription, currentApp)
            val response = callOpenRouterAPI(prompt, apiKey)
            
            val decision = parseDecisionResponse(response, nsfwRegionCount, maxConfidence)
            Log.d(TAG, "🎯 LLM recommended: ${decision.action} - ${decision.reasoning}")
            
            return@withContext decision
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ LLM decision failed, using fallback", e)
            return@withContext createFallbackDecision(nsfwRegionCount, maxConfidence)
        }
    }
    
    /**
     * Create optimized prompt for fast decision making
     */
    private fun createDecisionPrompt(
        regionCount: Int,
        confidence: Float,
        contentDescription: String,
        currentApp: String
    ): String {
        return """
        NSFW Content Decision System - Respond in JSON format only.
        
        CONTEXT:
        - Detected: $regionCount NSFW regions
        - Confidence: ${(confidence * 100).toInt()}%
        - Content: $contentDescription
        - App: $currentApp
        
        ACTIONS AVAILABLE:
        1. NO_ACTION - Safe content, no action needed
        2. SELECTIVE_BLUR - Blur specific regions only
        3. SCROLL_AWAY - Scroll page to move content out of view (gentle)
        4. NAVIGATE_BACK - Go back to previous page (moderate)
        5. AUTO_CLOSE_APP - Close app entirely (aggressive)
        6. GENTLE_REDIRECT - Brief warning then navigate away
        
        RULES:
        - 6+ regions = serious content, take protective action
        - 8+ regions = very serious, stronger action needed
        - 10+ regions = extreme content, immediate protection
        - Consider user experience - don't be too aggressive
        - Prioritize user safety while maintaining usability
        
        Respond ONLY with this JSON format:
        {
            "action": "ACTION_NAME",
            "reasoning": "brief reason (max 50 chars)",
            "confidence": 0.95,
            "urgency": "low|medium|high|critical"
        }
        """.trimIndent()
    }
    
    /**
     * Call OpenRouter API
     */
    private suspend fun callOpenRouterAPI(prompt: String, apiKey: String): String = 
        withContext(Dispatchers.IO) {
            
            val requestBody = JSONObject().apply {
                put("model", DEFAULT_MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 150) // Keep response short for speed
                put("temperature", 0.1) // Low temperature for consistent decisions
                put("top_p", 0.9)
            }
            
            val request = Request.Builder()
                .url(OPENROUTER_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://haramblur.app")
                .addHeader("X-Title", "HaramBlur Content Filter")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            return@withContext withTimeoutOrNull(REQUEST_TIMEOUT) {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string() ?: throw IOException("Empty response")
                } else {
                    throw IOException("API request failed: ${response.code} ${response.message}")
                }
            } ?: throw IOException("Request timeout after ${REQUEST_TIMEOUT}ms")
        }
    
    /**
     * Parse LLM response into decision
     */
    private fun parseDecisionResponse(
        response: String, 
        regionCount: Int, 
        confidence: Float
    ): LLMDecisionResult {
        try {
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            
            // Extract JSON from response
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}") + 1
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                throw Exception("No JSON found in response")
            }
            
            val decisionJson = JSONObject(content.substring(jsonStart, jsonEnd))
            
            val actionString = decisionJson.getString("action")
            val action = parseActionFromString(actionString)
            val reasoning = decisionJson.optString("reasoning", "LLM decision")
            val llmConfidence = decisionJson.optDouble("confidence", 0.8).toFloat()
            val urgency = parseUrgency(decisionJson.optString("urgency", "medium"))
            
            return LLMDecisionResult(
                action = action,
                reasoning = reasoning,
                confidence = llmConfidence,
                urgency = urgency,
                isLLMDecision = true,
                responseTimeMs = 0L // Will be filled by caller
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse LLM response, using fallback", e)
            return createFallbackDecision(regionCount, confidence)
        }
    }
    
    /**
     * Convert string to ContentAction
     */
    private fun parseActionFromString(actionString: String): ContentAction {
        return when (actionString.uppercase()) {
            "NO_ACTION" -> ContentAction.NO_ACTION
            "SELECTIVE_BLUR" -> ContentAction.SELECTIVE_BLUR
            "SCROLL_AWAY" -> ContentAction.SCROLL_AWAY
            "NAVIGATE_BACK" -> ContentAction.NAVIGATE_BACK
            "AUTO_CLOSE_APP" -> ContentAction.AUTO_CLOSE_APP
            "GENTLE_REDIRECT" -> ContentAction.GENTLE_REDIRECT
            "FULL_SCREEN_BLUR" -> ContentAction.FULL_SCREEN_BLUR
            "BLOCK_AND_WARN" -> ContentAction.BLOCK_AND_WARN
            "IMMEDIATE_CLOSE" -> ContentAction.IMMEDIATE_CLOSE
            else -> {
                Log.w(TAG, "Unknown action: $actionString, using fallback")
                if (actionString.contains("CLOSE", ignoreCase = true)) ContentAction.AUTO_CLOSE_APP
                else if (actionString.contains("BACK", ignoreCase = true)) ContentAction.NAVIGATE_BACK
                else if (actionString.contains("SCROLL", ignoreCase = true)) ContentAction.SCROLL_AWAY
                else ContentAction.SELECTIVE_BLUR
            }
        }
    }
    
    /**
     * Parse urgency from string
     */
    private fun parseUrgency(urgencyString: String): UrgencyLevel {
        return when (urgencyString.lowercase()) {
            "low" -> UrgencyLevel.LOW
            "medium" -> UrgencyLevel.MEDIUM
            "high" -> UrgencyLevel.HIGH
            "critical" -> UrgencyLevel.CRITICAL
            else -> UrgencyLevel.MEDIUM
        }
    }
    
    /**
     * Create fallback decision when LLM is unavailable
     */
    private fun createFallbackDecision(regionCount: Int, confidence: Float): LLMDecisionResult {
        val (action, reasoning, urgency) = when {
            regionCount >= 10 && confidence >= 0.8f -> Triple(
                ContentAction.AUTO_CLOSE_APP,
                "Extreme content detected",
                UrgencyLevel.CRITICAL
            )
            regionCount >= 8 && confidence >= 0.7f -> Triple(
                ContentAction.NAVIGATE_BACK,
                "High inappropriate content",
                UrgencyLevel.HIGH
            )
            regionCount >= 6 && confidence >= 0.6f -> Triple(
                ContentAction.SCROLL_AWAY,
                "Multiple NSFW regions",
                UrgencyLevel.MEDIUM
            )
            regionCount >= 4 && confidence >= 0.5f -> Triple(
                ContentAction.SELECTIVE_BLUR,
                "Moderate content detected",
                UrgencyLevel.LOW
            )
            else -> Triple(
                ContentAction.NO_ACTION,
                "Content within tolerance",
                UrgencyLevel.LOW
            )
        }
        
        return LLMDecisionResult(
            action = action,
            reasoning = reasoning,
            confidence = 0.8f,
            urgency = urgency,
            isLLMDecision = false,
            responseTimeMs = 0L
        )
    }
}

/**
 * Result from LLM decision making
 */
data class LLMDecisionResult(
    val action: ContentAction,
    val reasoning: String,
    val confidence: Float, // 0.0 to 1.0
    val urgency: UrgencyLevel,
    val isLLMDecision: Boolean, // true if from LLM, false if fallback
    val responseTimeMs: Long
)

/**
 * Urgency levels for content actions
 */
enum class UrgencyLevel {
    LOW,
    MEDIUM, 
    HIGH,
    CRITICAL
}

/**
 * Available LLM model for user selection
 */
data class LLMModel(
    val id: String,          // Model ID for API calls
    val displayName: String, // User-friendly name
    val description: String  // Model description
)
