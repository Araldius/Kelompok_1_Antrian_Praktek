package com.example.kelompok_1_uts_antrian_praktek

data class Antrian(
    var id: String = "",
    var namaPasien: String = "",
    var nomorAntrian: Int = 0,
    var status: String = "",      // "Menunggu", "Dipanggil", "Selesai"
    var userId: String = "",
    var tanggal: String = "",     // Format: yyyy-MM-dd
    var keluhan: String = ""
)

data class Jadwal(
    var id: String = "",          // ID Dokumen (Senin, Selasa, dll)
    var hari: String = "",        // Nama Hari
    var jamBuka: String = "",
    var jamTutup: String = "",
    var status: String = ""       // "Buka" atau "Tutup"
)

object DummyData {
    // Menyimpan sesi lokal user yang login
    var currentLoggedInUserId: String? = null
    var currentUserRole: String? = "pasien"
}