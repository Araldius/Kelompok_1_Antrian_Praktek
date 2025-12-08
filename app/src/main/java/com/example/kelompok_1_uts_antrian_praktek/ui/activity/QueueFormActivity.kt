package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityQueueFormBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class QueueFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQueueFormBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_queue_form)

        // 1. Load Nama User Otomatis
        loadUserProfile()

        binding.btnSubmitQueue.setOnClickListener {
            daftarAntrian()
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        binding.etPatientName.setText(user.fullName)
                        // Disable input nama karena harus sesuai akun
                        binding.etPatientName.isEnabled = false
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.msg_check_connection), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun daftarAntrian() {
        val nama = binding.etPatientName.text.toString().trim()
        val keluhan = binding.etComplaint.text.toString().trim()

        if (nama.isEmpty()) {
            binding.etPatientName.error = getString(R.string.error_name_required)
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmitQueue.isEnabled = false

        // 2. Cek Status Klinik (Buka/Tutup)
        db.collection("statusKlinik").document("hariIni").get()
            .addOnSuccessListener { doc ->
                val status = doc.getString("status")

                // Jika Tutup
                if (status != null && !status.equals("buka", ignoreCase = true)) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitQueue.isEnabled = true
                    Toast.makeText(this, getString(R.string.msg_clinic_closed), Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Jika Buka, lanjut simpan
                simpanKeFirestore(nama, keluhan)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitQueue.isEnabled = true
                Toast.makeText(this, getString(R.string.msg_check_connection), Toast.LENGTH_SHORT).show()
            }
    }

    private fun simpanKeFirestore(nama: String, keluhan: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val userId = auth.currentUser?.uid ?: "guest"

        // Referensi ke sub-collection: antrian -> [TANGGAL] -> pasien
        val ref = db.collection("antrian").document(todayDate).collection("pasien")

        ref.get().addOnSuccessListener { result ->
            val nomorBaru = result.size() + 1

            val antrianBaru = Antrian(
                namaPasien = nama,
                nomorAntrian = nomorBaru,
                status = "Menunggu",
                userId = userId,
                tanggal = todayDate,
                keluhan = keluhan
            )

            ref.add(antrianBaru).addOnSuccessListener {
                // Update dummy field di dokumen parent (tanggal) agar muncul di console
                db.collection("antrian").document(todayDate).set(mapOf("lastUpdate" to Date()))

                val msg = getString(R.string.msg_queue_success, nomorBaru)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish() // Kembali ke Home
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmitQueue.isEnabled = true
                Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}