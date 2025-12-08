package com.example.kelompok_1_uts_antrian_praktek

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.EditText
import android.widget.LinearLayout
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityScheduleAdminBinding
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleAdminBinding
    private lateinit var adapter: ScheduleAdminAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = ScheduleAdminAdapter(emptyList(), true) { jadwal ->
            showEditDialog(jadwal)
        }
        binding.rvScheduleAdmin.layoutManager = LinearLayoutManager(this)
        binding.rvScheduleAdmin.adapter = adapter
    }

    private fun loadData() {
        db.collection("jadwalPraktek").get().addOnSuccessListener { result ->
            val list = mutableListOf<Jadwal>()
            val urutan = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

            for (doc in result) {
                val j = doc.toObject(Jadwal::class.java)
                j.id = doc.id
                j.hari = doc.id // ID dokumen adalah nama hari
                list.add(j)
            }

            val sorted = list.sortedBy { urutan.indexOf(it.hari) }
            adapter.updateData(sorted)
        }
    }

    private fun showEditDialog(jadwal: Jadwal) {
        // Buat layout sederhana untuk dialog
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputBuka = EditText(this)
        inputBuka.hint = "Jam Buka (Contoh: 09:00)"
        inputBuka.setText(jadwal.jamBuka)
        layout.addView(inputBuka)

        val inputTutup = EditText(this)
        inputTutup.hint = "Jam Tutup (Contoh: 17:00)"
        inputTutup.setText(jadwal.jamTutup)
        layout.addView(inputTutup)

        AlertDialog.Builder(this)
            .setTitle("Ubah Jadwal ${jadwal.hari}")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val bukaBaru = inputBuka.text.toString()
                val tutupBaru = inputTutup.text.toString()
                val statusBaru = if (bukaBaru.isEmpty()) "Tutup" else "Buka"

                // Update ke Firebase
                db.collection("jadwalPraktek").document(jadwal.id)
                    .update(
                        mapOf(
                            "jamBuka" to bukaBaru,
                            "jamTutup" to tutupBaru,
                            "status" to statusBaru
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Jadwal diperbarui", Toast.LENGTH_SHORT).show()
                        loadData() // Refresh list
                    }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}