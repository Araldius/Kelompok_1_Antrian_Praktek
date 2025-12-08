package com.example.kelompok_1_uts_antrian_praktek

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityQueueFormBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class QueueFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQueueFormBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSubmitQueue.setOnClickListener { daftarAntrian() }
    }

    private fun daftarAntrian() {
        val nama = binding.etPatientName.text.toString()
        val keluhan = binding.etComplaint.text.toString()

        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitQueue.isEnabled = false

        // --- REVISI: CEK STATUS KLINIK DULU ---
        db.collection("statusKlinik").document("hariIni").get()
            .addOnSuccessListener { document ->
                val status = document.getString("status")

                // Jika status bukan "buka" (artinya tutup/null), tolak pendaftaran
                if (status != "buka") {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitQueue.isEnabled = true
                    Toast.makeText(this, "Maaf, pendaftaran sedang DITUTUP oleh dokter.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Jika status "buka", lanjut proses pendaftaran...
                prosesSimpanKeFirebase(nama, keluhan)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitQueue.isEnabled = true
                Toast.makeText(this, "Gagal cek status klinik: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prosesSimpanKeFirebase(nama: String, keluhan: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val ref = db.collection("antrian").document(todayDate).collection("pasien")

        ref.get().addOnSuccessListener { result ->
            val nomorBaru = result.size() + 1

            val data = hashMapOf(
                "namaPasien" to nama,
                "nomorAntrian" to nomorBaru,
                "status" to "Menunggu",
                "userId" to userId,
                "tanggal" to todayDate,
                "keluhan" to keluhan
            )

            ref.add(data).addOnSuccessListener {
                // Update dummy field di parent agar dokumen tanggal terbentuk
                db.collection("antrian").document(todayDate).set(mapOf("lastUpdate" to Date()))

                Toast.makeText(this, "Berhasil! No. $nomorBaru", Toast.LENGTH_LONG).show()
                finish()
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitQueue.isEnabled = true
                Toast.makeText(this, "Gagal simpan: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}