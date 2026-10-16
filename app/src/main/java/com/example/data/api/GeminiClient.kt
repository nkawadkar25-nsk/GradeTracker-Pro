package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioBase64: String? = null
)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Multi-turn chat using gemini-3.8-flash
     */
    suspend fun sendChatMessage(
        messages: List<ChatMessage>,
        systemInstructionText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please set your key in the AI Studio Secrets panel.")
            )
        }

        try {
            val root = JSONObject()

            // System instruction
            val systemPart = JSONObject().put("text", systemInstructionText)
            val systemContent = JSONObject().put("parts", JSONArray().put(systemPart))
            root.put("systemInstruction", systemContent)

            // Conversation history contents
            val contentsArray = JSONArray()
            for (msg in messages) {
                val part = JSONObject().put("text", msg.content)
                val contentObj = JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    put("parts", JSONArray().put(part))
                }
                contentsArray.put(contentObj)
            }
            root.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1500)
            }
            root.put("generationConfig", genConfig)

            val url = "$BASE_URL/gemini-3.8-flash:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from AI Advisor"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Text-To-Speech (TTS) using gemini-3.8-flash-tts
     */
    suspend fun generateSpeechAudio(textPrompt: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please set your key in the AI Studio Secrets panel.")
            )
        }

        try {
            val root = JSONObject()
            val part = JSONObject().put("text", textPrompt)
            val contentObj = JSONObject().put("parts", JSONArray().put(part))
            root.put("contents", JSONArray().put(contentObj))

            // Speech config requesting AUDIO modality
            val voiceConfig = JSONObject().apply {
                put("prebuiltVoiceConfig", JSONObject().put("voiceName", "Kore"))
            }
            val speechConfig = JSONObject().apply {
                put("voiceConfig", voiceConfig)
            }
            val genConfig = JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", speechConfig)
            }
            root.put("generationConfig", genConfig)

            val url = "$BASE_URL/gemini-3.8-flash-tts:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var audioBytes: ByteArray? = null
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        val base64Data = inline.optString("data")
                        if (base64Data.isNotBlank()) {
                            audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            break
                        }
                    }
                }
            }

            if (audioBytes != null && audioBytes.isNotEmpty()) {
                Result.success(audioBytes)
            } else {
                Result.failure(Exception("No audio bytes received from Gemini TTS"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * High-Quality Image Generation using gemini-3-pro-image-preview
     * with user-selectable image size: "1K", "2K", "4K"
     */
    suspend fun generateImage(
        prompt: String,
        imageSize: String = "1K", // "1K", "2K", "4K"
        aspectRatio: String = "1:1" // "1:1", "16:9", "4:3"
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please set your key in the AI Studio Secrets panel.")
            )
        }

        try {
            val root = JSONObject()
            val part = JSONObject().put("text", prompt)
            val contentObj = JSONObject().put("parts", JSONArray().put(part))
            root.put("contents", JSONArray().put(contentObj))

            val imgConfig = JSONObject().apply {
                put("aspectRatio", aspectRatio)
                put("imageSize", imageSize)
            }
            val genConfig = JSONObject().apply {
                put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                put("imageConfig", imgConfig)
            }
            root.put("generationConfig", genConfig)

            val url = "$BASE_URL/gemini-3-pro-image-preview:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var bitmap: Bitmap? = null
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        val base64Data = inline.optString("data")
                        if (base64Data.isNotBlank()) {
                            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            break
                        }
                    }
                }
            }

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(Exception("No image returned from gemini-3-pro-image-preview"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
