package com.example.kelompok_1_uts_antrian_praktek.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // GANTI IP INI dengan IP Laptop/Server tempat ML berjalan.
    // Jika di emulator Android Studio ke localhost laptop, gunakan "http://10.0.2.2:5000/"
    // Jika di HP fisik, gunakan IP Laptop (misal "http://192.168.1.10:5000/") pastikan satu WiFi.
    private const val BASE_URL = "http://192.168.100.236:5000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}