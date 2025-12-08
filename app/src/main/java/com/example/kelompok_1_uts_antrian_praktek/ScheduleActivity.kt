package com.example.kelompok_1_uts_antrian_praktek

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScheduleBinding
    private lateinit var adapter: ScheduleAdminAdapter // Anda perlu menyalin adapter ini juga dari proyek lama
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Gunakan adapter dari project lama, pastikan package name disesuaikan
        adapter = ScheduleAdminAdapter(emptyList(), false) {}
        binding.rvSchedule.layoutManager = LinearLayoutManager(this)
        binding.rvSchedule.adapter = adapter

        loadJadwal()
    }

    private fun loadJadwal() {
        db.collection("jadwalPraktek").get().addOnSuccessListener { docs ->
            val list = mutableListOf<Jadwal>()
            val urutan = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

            for (doc in docs) {
                val j = doc.toObject(Jadwal::class.java)
                j.id = doc.id
                j.hari = doc.id // Nama dokumen = Nama hari
                list.add(j)
            }

            // Urutkan hari
            val sorted = list.sortedBy { urutan.indexOf(it.hari) }
            adapter.updateData(sorted)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}