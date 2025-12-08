package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
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

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Laporan Statistik"

        setupSpinner()
    }

    private fun setupSpinner() {
        binding.spinnerPeriode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Panggil loadReport berdasarkan pilihan user
                when (position) {
                    0 -> loadReport("hari")   // Pilihan 1: Hari Ini
                    1 -> loadReport("minggu") // Pilihan 2: Minggu Ini
                    2 -> loadReport("bulan")  // Pilihan 3: Bulan Ini
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadReport(periode: String) {
        binding.progressBarReport.visibility = View.VISIBLE
        binding.tvPatientsDone.text = "-"

        // Setup Calendar
        val calendar = Calendar.getInstance()
        // Set locale ke Indonesia agar Senin dianggap awal minggu (opsional, tapi bagus)
        val localeID = Locale("id", "ID")
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMM yyyy", localeID)

        // 1. Tentukan Tanggal Akhir (Selalu Hari Ini)
        val endDate = dbFormat.format(Date())

        // 2. Tentukan Tanggal Awal (Start Date)
        var startDate = ""
        var displayText = ""

        when (periode) {
            "hari" -> {
                // Hari Ini: Start = End
                startDate = endDate
                displayText = displayFormat.format(Date())
            }
            "minggu" -> {
                // Minggu Ini: Mundur ke hari Senin pada minggu yang sedang berjalan
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

                startDate = dbFormat.format(calendar.time)
                displayText = "${displayFormat.format(calendar.time)} - Hari Ini"
            }
            "bulan" -> {
                // Bulan Ini: Mundur ke Tanggal 1 pada bulan yang sedang berjalan
                calendar.set(Calendar.DAY_OF_MONTH, 1)

                startDate = dbFormat.format(calendar.time)
                displayText = "${displayFormat.format(calendar.time)} - Hari Ini"
            }
        }

        binding.tvReportDate.text = displayText

        // QUERY KE FIREBASE
        // Logika: Cari status "Selesai" ANTARA startDate dan endDate
        db.collectionGroup("pasien")
            .whereEqualTo("status", "Selesai")
            .whereGreaterThanOrEqualTo("tanggal", startDate)
            .whereLessThanOrEqualTo("tanggal", endDate)
            .get()
            .addOnSuccessListener { result ->
                binding.progressBarReport.visibility = View.GONE

                val total = result.size()
                binding.tvPatientsDone.text = total.toString()

                if (total == 0) {
                    Toast.makeText(this, "Data kosong untuk periode ini", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBarReport.visibility = View.GONE
                binding.tvPatientsDone.text = "0"
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}