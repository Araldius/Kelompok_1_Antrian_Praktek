package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.os.Bundle
import android.view.View
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

        // Cek apakah ini Dokter?
        val isDoctorView = intent.getBooleanExtra("IS_DOCTOR_VIEW", false)

        supportActionBar?.title = if (isDoctorView) "Pasien Selesai Hari Ini" else "Riwayat Kunjungan"

        adapter = AntrianAdapter(emptyList(), false) {}
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter

        if (isDoctorView) {
            loadDoctorHistory()
        } else {
            loadPatientHistory()
        }
    }

    // MODE DOKTER: Tampilkan yang status 'Selesai' hari ini
    private fun loadDoctorHistory() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("antrian").document(today).collection("pasien")
            .whereEqualTo("status", "Selesai")
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Antrian::class.java)
                if (list.isEmpty()) {
                    Toast.makeText(this, "Belum ada pasien selesai hari ini", Toast.LENGTH_SHORT).show()
                }
                adapter.updateData(list)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // MODE PASIEN: Tampilkan riwayat dia sendiri (semua tanggal)
    private fun loadPatientHistory() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Collection Group Query (Butuh Index)
        db.collectionGroup("pasien")
            .whereEqualTo("userId", uid)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Antrian::class.java)
                if (list.isEmpty()) {
                    Toast.makeText(this, "Belum ada riwayat", Toast.LENGTH_SHORT).show()
                }
                adapter.updateData(list)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal/Butuh Index: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}