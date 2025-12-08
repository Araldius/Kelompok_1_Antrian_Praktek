package com.example.kelompok_1_uts_antrian_praktek.model

data class User(
    var uid: String = "",
    var email: String = "",
    var fullName: String = "", // Nama Asli
    var role: String = "pasien" // pasien, dokter, admin
)