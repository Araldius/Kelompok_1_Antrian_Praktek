package com.example.kelompok_1_uts_antrian_praktek

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityReportBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Laporan Harian"

        updateUI()
    }

    private fun updateUI() {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val displayDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

        binding.tvReportDate.text = "Laporan untuk tanggal: $displayDate"

        // Hitung data dari Firestore hari ini
        db.collection("antrian").document(todayDate).collection("pasien")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val total = snapshots.size()
                    var selesai = 0
                    var menunggu = 0

                    for (doc in snapshots) {
                        val status = doc.getString("status") ?: ""
                        if (status == "Selesai") selesai++
                        else if (status == "Menunggu" || status == "Dipanggil") menunggu++
                    }

                    binding.tvTotalPatients.text = total.toString()
                    binding.tvPatientsDone.text = selesai.toString()
                    binding.tvPatientsWaiting.text = menunggu.toString()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}