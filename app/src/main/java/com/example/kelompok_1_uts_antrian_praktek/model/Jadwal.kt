package com.example.kelompok_1_uts_antrian_praktek.model

import com.google.firebase.firestore.DocumentId

data class Jadwal(
    @DocumentId
    var id: String = "",    // Ini otomatis terisi "Senin", "Selasa", dll dari ID Dokumen
    var hari: String = "",
    var jamBuka: String = "",
    var jamTutup: String = "",
    var status: String = "",
    var urutan: Int = 0
)