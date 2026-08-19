package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
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

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateWithPrompt(prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext null
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} $responseBody")
                return@withContext null
            }

            parseCandidateText(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            null
        }
    }

    suspend fun analyzeReceiptImage(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext null
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are an expert grocery receipt and pantry item scanner.
                Analyze this grocery receipt or item image and extract all purchased/visible food items.
                For each food item, provide a JSON array with:
                - "name": string (clean common food name, e.g. "Spinach", "Whole Milk", "Apples")
                - "quantity": number (e.g. 1.0, 2.0)
                - "unit": string (e.g. "pcs", "pack", "carton", "g", "kg", "loaf", "bottle")
                - "category": string (MUST be one of: "PRODUCE", "DAIRY", "PANTRY", "MEAT_SEAFOOD", "BAKERY", "BEVERAGES", "FROZEN", "SNACKS_CONDIMENTS")
                - "estimatedShelfLifeDays": integer (realistic days before it spoils in fridge/pantry, e.g., fresh produce 4-7, milk 7-10, meat 3, pantry 180)
                - "estimatedPrice": number (approximate price in USD if visible, else realistic estimate)
                - "storageLocation": string ("FRIDGE", "PANTRY", or "FREEZER")

                Return ONLY valid JSON in format:
                {
                  "storeName": "Store name if readable",
                  "items": [
                    {
                      "name": "Organic Milk",
                      "quantity": 1,
                      "unit": "carton",
                      "category": "DAIRY",
                      "estimatedShelfLifeDays": 8,
                      "estimatedPrice": 3.99,
                      "storageLocation": "FRIDGE"
                    }
                  ]
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini vision error: ${response.code} $responseBody")
                return@withContext null
            }

            parseCandidateText(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Exception analyzing image", e)
            null
        }
    }

    private fun parseCandidateText(jsonResponse: String): String? {
        try {
            val root = JSONObject(jsonResponse)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            return parts.getJSONObject(0).optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response JSON", e)
            return null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if too large to save bandwidth and speed up processing
        val maxDim = 1200
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
            val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
