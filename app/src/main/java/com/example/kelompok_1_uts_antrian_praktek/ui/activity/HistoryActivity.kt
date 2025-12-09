package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityHistoryBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.AntrianAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: AntrianAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val isDoctorView = intent.getBooleanExtra("IS_DOCTOR_VIEW", false)
        val targetUserId = intent.getStringExtra("TARGET_USER_ID")
        val targetUserName = intent.getStringExtra("TARGET_USER_NAME")

        adapter = AntrianAdapter(emptyList(), false) {} // Mode Read-Only (False)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        // --- PILIH MODE TAMPILAN ---
        when {
            // MODE 1: Hasil Search (Admin/Dokter melihat Rekam Medis Pasien Tertentu)
            targetUserId != null -> {
                supportActionBar?.title = "Rekam Medis: $targetUserName"
                loadSpecificPatientHistory(targetUserId)
            }
            // MODE 2: Dokter melihat Antrian Hari Ini
            isDoctorView -> {
                supportActionBar?.title = "Pasien Selesai Hari Ini"
                loadDoctorHistory()
            }
            // MODE 3: Pasien melihat Riwayat Sendiri
            else -> {
                supportActionBar?.title = "Riwayat Kunjungan"
                loadPatientHistory()
            }
        }
    }

    // FUNGSI LOAD 1: Ambil dari koleksi 'rekam_medis' (Detail Lengkap)
    private fun loadSpecificPatientHistory(uid: String) {
        // DEBUGGING: Cek apakah UID diterima
        // Toast.makeText(this, "Mencari rekam medis ID: $uid", Toast.LENGTH_SHORT).show()

        // PENTING: Pastikan nama collection di Firebase Console Anda adalah "rekam_medis"
        // (Bukan "RekamMedis" atau "rekammedis")
        db.collection("rekam_medis")
            .whereEqualTo("pasienId", uid) // Pastikan field di dokumen bernama "pasienId"
            // .orderBy("tanggal", Query.Direction.DESCENDING) // Hapus dulu orderBy jika Index belum dibuat (sering bikin error)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    Antrian(
                        id = doc.id,
                        namaPasien = doc.getString("namaPasien") ?: "Tanpa Nama",
                        // Menggabungkan Diagnosa & Obat ke kolom keluhan untuk ditampilkan
                        keluhan = "📅 ${doc.getString("tanggal")}\n\n" +
                                "🩺 Diagnosa: ${doc.getString("diagnosa") ?: "-"}\n" +
                                "💊 Obat: ${doc.getString("pengobatan") ?: "-"}",
                        tanggal = doc.getString("tanggal") ?: "",
                        status = "Selesai" // Status dummy agar tampil hijau
                    )
                }

                if (list.isEmpty()) {
                    Toast.makeText(this, "Data rekam medis kosong/tidak ditemukan", Toast.LENGTH_LONG).show()
                }
                adapter.updateData(list)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal ambil data: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // FUNGSI LOAD 2: Ambil dari koleksi 'antrian' (Hari Ini & Selesai)
    private fun loadDoctorHistory() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("antrian").document(today).collection("pasien")
            .whereEqualTo("status", "Selesai")
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Antrian::class.java)
                if (list.isEmpty()) Toast.makeText(this, "Belum ada pasien selesai", Toast.LENGTH_SHORT).show()
                adapter.updateData(list)
            }
    }

    // FUNGSI LOAD 3: Ambil Riwayat User (Semua Tanggal)
    private fun loadPatientHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collectionGroup("pasien")
            .whereEqualTo("userId", uid)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Antrian::class.java)
                if (list.isEmpty()) Toast.makeText(this, "Belum ada riwayat", Toast.LENGTH_SHORT).show()
                adapter.updateData(list)
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}