package com.example.kelompok_1_uts_antrian_praktek.model

data class Antrian(
    var id: String = "",
    var namaPasien: String = "",
    var nomorAntrian: Int = 0,
    var status: String = "",
    var userId: String = "",
    var tanggal: String = "",
    var keluhan: String = ""
)