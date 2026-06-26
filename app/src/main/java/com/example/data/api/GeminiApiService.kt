package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class VerificationResult(
    val verified: Boolean,
    val confidence: Double,
    val reason: String
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun verifyProof(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun verifyPhotoProof(
        base64Photo: String,
        taskType: String,
        taskDetails: String,
        base64ReferencePhoto: String? = null
    ): VerificationResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return VerificationResult(
                verified = true, // Graceful bypass for testing if key is empty/placeholder
                confidence = 1.0,
                reason = "Mock validation: API Key is not set or is a placeholder. Proof accepted for testing."
            )
        }

        val promptText = if (base64ReferencePhoto != null) {
            """
            You are a strict, objective AI Verification assistant for a Smart Proof-Based Alarm App named OwnUp.
            You have been provided two photos:
            1. The first photo is the baseline REFERENCE image configured by the user for the task "$taskType" (details: "$taskDetails").
            2. The second photo is the LIVE image captured just now to prove they completed the activity.

            Analyze both images for Object, Visual, Feature, and Scene similarity:
            - Verify if the Live image represents a genuine, real-time completion of the task described in "$taskDetails".
            - Verify if the Live image is highly similar to or visually consistent with the Reference image (e.g., showing the same book/textbook, the same gym weights, the same desk environment, or the same room setup with high-fidelity, representing a >= 80% similarity of context/purpose).
            - Strictly reject any fake images, screenshots, photos of computer screens, or unrelated subjects.

            Provide your response strictly in the following JSON format:
            {
              "verified": true/false,
              "confidence": 0.0 to 1.0,
              "reason": "A brief 1-2 sentence explanation of your comparison and why you accepted (similarity >= 80%) or rejected the proof."
            }
            """.trimIndent()
        } else {
            """
            You are a strict, objective AI Verification assistant for a Smart Proof-Based Alarm App.
            Analyze the provided photo. The user has set an alarm for the task type: "$taskType" (Description: "$taskDetails").
            You must verify if the photo shows clear, live proof that the user is actively engaging in or at the location of this task.
            For example:
            - 'Study' or 'Reading' requires showing open books, textbooks, notepad with handwriting, study desk, pen, computer screen with academic work, or reading material.
            - 'Gym' or 'Workout' requires showing gym equipment (dumbbells, treadmill, yoga mat, workout weights), gym interior, workout attire, or workout space.
            - 'Custom' task requires matching the details provided: "$taskDetails".

            Provide your response strictly in the following JSON format:
            {
              "verified": true/false,
              "confidence": 0.0 to 1.0,
              "reason": "A brief 1-2 sentence explanation of what you see and why it is accepted or rejected."
            }
            """.trimIndent()
        }

        val partsList = mutableListOf<Part>()
        partsList.add(Part(text = promptText))
        
        if (base64ReferencePhoto != null) {
            partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64ReferencePhoto)))
        }
        partsList.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Photo)))

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = partsList
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        return try {
            val response = service.verifyProof(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return VerificationResult(false, 0.0, "Empty response from AI engine.")

            // Parse result from JSON
            val adapter = moshi.adapter(VerificationResult::class.java)
            adapter.fromJson(jsonText) ?: VerificationResult(false, 0.0, "Failed to parse AI review output: $jsonText")
        } catch (e: Exception) {
            VerificationResult(
                verified = false,
                confidence = 0.0,
                reason = "AI verification failed due to network or server error: ${e.message}"
            )
        }
    }
}
