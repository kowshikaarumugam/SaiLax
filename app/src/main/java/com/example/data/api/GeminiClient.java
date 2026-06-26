package com.example.data.api;

import com.example.BuildConfig;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GeminiClient {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build();

    private static final Moshi moshi = new Moshi.Builder()
        .addLast(new KotlinJsonAdapterFactory())
        .build();

    private static final GeminiApiService service = new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeminiApiService.class);

    public static VerificationResult verifyPhotoProof(
        String base64Photo,
        String taskType,
        String taskDetails,
        String base64ReferencePhoto
    ) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("MY_GEMINI_API_KEY")) {
            return new VerificationResult(
                true, // Graceful bypass for testing if key is empty/placeholder
                1.0,
                "Mock validation: API Key is not set or is a placeholder. Proof accepted for testing."
            );
        }

        String promptText;
        if (base64ReferencePhoto != null) {
            promptText = "You are a strict, objective AI Verification assistant for a Smart Proof-Based Alarm App named OwnUp.\n" +
                "You have been provided two photos:\n" +
                "1. The first photo is the baseline REFERENCE image configured by the user for the task \"" + taskType + "\" (details: \"" + taskDetails + "\").\n" +
                "2. The second photo is the LIVE image captured just now to prove they completed the activity.\n\n" +
                "Analyze both images for Object, Visual, Feature, and Scene similarity:\n" +
                "- Verify if the Live image represents a genuine, real-time completion of the task described in \"" + taskDetails + "\".\n" +
                "- Verify if the Live image is highly similar to or visually consistent with the Reference image (e.g., showing the same book/textbook, the same gym weights, the same desk environment, or the same room setup with high-fidelity, representing a >= 80% similarity of context/purpose).\n" +
                "- Strictly reject any fake images, screenshots, photos of computer screens, or unrelated subjects.\n\n" +
                "Provide your response strictly in the following JSON format:\n" +
                "{\n" +
                "  \"verified\": true/false,\n" +
                "  \"confidence\": 0.0 to 1.0,\n" +
                "  \"reason\": \"A brief 1-2 sentence explanation of your comparison and why you accepted (similarity >= 80%) or rejected the proof.\"\n" +
                "}";
        } else {
            promptText = "You are a strict, objective AI Verification assistant for a Smart Proof-Based Alarm App.\n" +
                "Analyze the provided photo. The user has set an alarm for the task type: \"" + taskType + "\" (Description: \"" + taskDetails + "\").\n" +
                "You must verify if the photo shows clear, live proof that the user is actively engaging in or at the location of this task.\n" +
                "For example:\n" +
                "- 'Study' or 'Reading' requires showing open books, textbooks, notepad with handwriting, study desk, pen, computer screen with academic work, or reading material.\n" +
                "- 'Gym' or 'Workout' requires showing gym equipment (dumbbells, treadmill, yoga mat, workout weights), gym interior, workout attire, or workout space.\n" +
                "- 'Custom' task requires matching the details provided: \"" + taskDetails + "\".\n\n" +
                "Provide your response strictly in the following JSON format:\n" +
                "{\n" +
                "  \"verified\": true/false,\n" +
                "  \"confidence\": 0.0 to 1.0,\n" +
                "  \"reason\": \"A brief 1-2 sentence explanation of what you see and why it is accepted or rejected.\"\n" +
                "}";
        }

        List<Part> partsList = new ArrayList<>();
        partsList.add(new Part(promptText, null));

        if (base64ReferencePhoto != null) {
            partsList.add(new Part(null, new InlineData("image/jpeg", base64ReferencePhoto)));
        }
        partsList.add(new Part(null, new InlineData("image/jpeg", base64Photo)));

        List<Content> contents = new ArrayList<>();
        contents.add(new Content(partsList));

        GeminiRequest request = new GeminiRequest(
            contents,
            new GenerationConfig("application/json", 0.2f)
        );

        try {
            Response<GeminiResponse> response = service.verifyProof(apiKey, request).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return new VerificationResult(false, 0.0, "API Error: " + response.code() + " " + response.message());
            }

            GeminiResponse geminiResponse = response.body();
            if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
                return new VerificationResult(false, 0.0, "Empty response from AI engine.");
            }

            Candidate candidate = geminiResponse.getCandidates().get(0);
            if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
                return new VerificationResult(false, 0.0, "Empty response parts from AI engine.");
            }

            String jsonText = candidate.getContent().getParts().get(0).getText();
            if (jsonText == null || jsonText.isEmpty()) {
                return new VerificationResult(false, 0.0, "Empty text from AI engine.");
            }

            // Parse result from JSON
            Moshi simpleMoshi = new Moshi.Builder().build();
            return simpleMoshi.adapter(VerificationResult.class).fromJson(jsonText);
        } catch (Exception e) {
            return new VerificationResult(
                false,
                0.0,
                "AI verification failed due to network or server error: " + e.getMessage()
            );
        }
    }
}
