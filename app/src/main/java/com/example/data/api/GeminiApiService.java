package com.example.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    Call<GeminiResponse> verifyProof(
        @Query("key") String apiKey,
        @Body GeminiRequest request
    );
}
