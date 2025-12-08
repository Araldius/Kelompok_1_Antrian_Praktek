package com.example.kelompok_1_uts_antrian_praktek.model

data class RekamMedis(
    var id: String = "",
    var pasienId: String = "",
    var namaPasien: String = "",
    var keluhanAwal: String = "",
    var diagnosa: String = "",
    var pengobatan: String = "",
    var tanggal: String = "",
    var dokterPemeriksa: String = ""
)