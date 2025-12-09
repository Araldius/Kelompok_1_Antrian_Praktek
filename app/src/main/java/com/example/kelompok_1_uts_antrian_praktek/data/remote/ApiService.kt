package com.example.kelompok_1_uts_antrian_praktek.data.remote

import com.example.kelompok_1_uts_antrian_praktek.model.PredictionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/predict")
    suspend fun getPrediction(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): Response<PredictionResponse>
}