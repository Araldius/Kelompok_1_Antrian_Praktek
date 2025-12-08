package com.example.kelompok_1_uts_antrian_praktek

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
        supportActionBar?.title = "Riwayat Kunjungan"

        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        adapter = AntrianAdapter(emptyList(), false) {}
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun loadHistoryData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak login", Toast.LENGTH_SHORT).show()
            return
        }

        // Query ini mencari ke SEMUA tanggal di sub-collection 'pasien'
        // WAJIB PUNYA INDEX DI FIREBASE CONSOLE
        db.collectionGroup("pasien")
            .whereEqualTo("userId", userId)
            .orderBy("tanggal", Query.Direction.DESCENDING) // Urutkan dari yg terbaru
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Antrian::class.java)
                adapter.updateData(list)

                if (list.isEmpty()) {
                    Toast.makeText(this, "Belum ada riwayat kunjungan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // PERHATIKAN LOG INI DI LOGCAT
                Log.e("HistoryError", "Gagal load history: ${e.message}")

                if (e.message!!.contains("index")) {
                    Toast.makeText(this, "Eror Index: Cek Logcat untuk link pembuatan Index", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}