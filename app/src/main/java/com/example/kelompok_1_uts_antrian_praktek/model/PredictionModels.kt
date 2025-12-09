package com.example.kelompok_1_uts_antrian_praktek.model

import com.google.gson.annotations.SerializedName

// Menyesuaikan dengan output app.py: {'status': '...', 'data': {...}, 'message': '...'}
data class PredictionResponse(
    @SerializedName("status") val status: String, // "success" atau "error"
    @SerializedName("data") val data: Map<String, Float>?, // Hasil prediksi
    @SerializedName("message") val message: String? // Pesan error jika ada
)