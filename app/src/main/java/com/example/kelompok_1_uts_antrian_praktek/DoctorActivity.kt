package com.example.kelompok_1_uts_antrian_praktek

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityDoctorBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DoctorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorBinding
    private lateinit var antrianAdapter: AntrianAdapter
    private val db = FirebaseFirestore.getInstance()

    // List lokal untuk menyimpan data snapshot terbaru
    private var listPasien: List<Antrian> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Dasbor Dokter"

        setupRecyclerView()

        // 1. Dengarkan data Pasien (Realtime)
        observeDataPasien()

        // 2. Dengarkan Status Klinik (Buka/Tutup)
        observeStatusKlinik()

        // --- LOGIKA TOMBOL ---

        // A. TOMBOL PANGGIL (Memanggil pasien urutan pertama yang "Menunggu")
        binding.btnPanggilBerikutnya.setOnClickListener {
            // Cek apakah ada pasien yang sedang dipanggil tapi belum diselesaikan?
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) {
                Toast.makeText(this, "Selesaikan dulu pasien: ${current.namaPasien}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cari pasien "Menunggu" dengan nomor antrian terkecil
            val nextPatient = listPasien
                .filter { it.status == "Menunggu" }
                .minByOrNull { it.nomorAntrian }

            if (nextPatient != null) {
                updateStatusPasien(nextPatient.id, "Dipanggil")
            } else {
                Toast.makeText(this, "Tidak ada pasien antri.", Toast.LENGTH_SHORT).show()
            }
        }

        // B. TOMBOL SELESAI (Menandai pasien yang sedang dipanggil menjadi "Selesai")
        binding.btnSelesai.setOnClickListener {
            val currentPatient = listPasien.find { it.status == "Dipanggil" }

            if (currentPatient != null) {
                updateStatusPasien(currentPatient.id, "Selesai")
            } else {
                Toast.makeText(this, "Belum ada pasien yang dipanggil.", Toast.LENGTH_SHORT).show()
            }
        }

        // C. TOMBOL TERLAMBAT (Pindahkan pasien ke antrian paling belakang)
        binding.btnPindahAkhir.setOnClickListener {
            val currentPatient = listPasien.find { it.status == "Dipanggil" }

            if (currentPatient != null) {
                // 1. Cari nomor antrian terbesar di list hari ini (termasuk yang selesai/menunggu)
                val maxQueueNumber = listPasien.maxOfOrNull { it.nomorAntrian } ?: 0
                val newNumber = maxQueueNumber + 1

                // 2. Update di Firebase: Ubah status jadi 'Menunggu' dan Nomor jadi paling besar
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                db.collection("antrian").document(todayDate)
                    .collection("pasien").document(currentPatient.id)
                    .update(
                        mapOf(
                            "status" to "Menunggu",
                            "nomorAntrian" to newNumber
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "${currentPatient.namaPasien} dipindah ke urutan $newNumber", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Hanya bisa memindah pasien yang sedang dipanggil.", Toast.LENGTH_SHORT).show()
            }
        }

        // D. SWITCH BUKA/TUTUP PRAKTEK
        binding.switchStatusPraktek.setOnCheckedChangeListener { _, isChecked ->
            // Update ke koleksi statusKlinik -> hariIni
            val statusStr = if (isChecked) "buka" else "tutup"
            db.collection("statusKlinik").document("hariIni")
                .set(mapOf("status" to statusStr))
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal update status klinik", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- FUNGSI PENDUKUNG ---

    private fun observeDataPasien() {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        binding.progressBarDokter.visibility = View.VISIBLE

        db.collection("antrian").document(todayDate).collection("pasien")
            .addSnapshotListener { snapshots, _ ->
                binding.progressBarDokter.visibility = View.GONE
                if (snapshots != null) {
                    val list = mutableListOf<Antrian>()
                    for (doc in snapshots) {
                        val d = doc.toObject(Antrian::class.java)
                        d.id = doc.id
                        list.add(d)
                    }
                    // Simpan ke variabel global agar bisa diakses tombol
                    listPasien = list
                    updateUI()
                }
            }
    }

    private fun observeStatusKlinik() {
        db.collection("statusKlinik").document("hariIni")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    // Update UI Switch tanpa memicu listener onCheckedChange berulang kali (opsional logic)
                    val isBuka = status == "buka"
                    if (binding.switchStatusPraktek.isChecked != isBuka) {
                        binding.switchStatusPraktek.isChecked = isBuka
                    }
                    binding.switchStatusPraktek.text = if (isBuka) "Pendaftaran Buka" else "Pendaftaran Tutup"
                }
            }
    }

    private fun updateStatusPasien(docId: String, newStatus: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("antrian").document(todayDate)
            .collection("pasien").document(docId)
            .update("status", newStatus)
            .addOnFailureListener {
                Toast.makeText(this, "Gagal update status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        antrianAdapter = AntrianAdapter(emptyList(), false) {}
        binding.rvAntrianDokter.layoutManager = LinearLayoutManager(this)
        binding.rvAntrianDokter.adapter = antrianAdapter
    }

    private fun updateUI() {
        // 1. Tampilkan info pasien yang sedang dipanggil di kartu atas
        val currentlyCalled = listPasien.find { it.status == "Dipanggil" }

        if (currentlyCalled != null) {
            binding.tvNamaPasienDipanggil.text = currentlyCalled.namaPasien
            binding.tvKeluhanDipanggil.text = "Keluhan: ${currentlyCalled.keluhan} (No. ${currentlyCalled.nomorAntrian})"
            // Aktifkan tombol Selesai & Terlambat
            binding.btnSelesai.isEnabled = true
            binding.btnPindahAkhir.isEnabled = true
            binding.btnPanggilBerikutnya.isEnabled = false // Disable panggil jika masih ada yang diperiksa
        } else {
            binding.tvNamaPasienDipanggil.text = "-"
            binding.tvKeluhanDipanggil.text = "Belum ada pasien dipanggil"
            binding.btnSelesai.isEnabled = false
            binding.btnPindahAkhir.isEnabled = false
            binding.btnPanggilBerikutnya.isEnabled = true
        }

        // 2. List bawah hanya menampilkan yang statusnya "Menunggu", diurutkan nomor antrian
        val waitingList = listPasien
            .filter { it.status == "Menunggu" }
            .sortedBy { it.nomorAntrian }

        antrianAdapter.updateData(waitingList)

        if (waitingList.isEmpty()) {
            binding.tvEmptyStateDokter.visibility = View.VISIBLE
            binding.rvAntrianDokter.visibility = View.GONE
        } else {
            binding.tvEmptyStateDokter.visibility = View.GONE
            binding.rvAntrianDokter.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.doctor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_logout) {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finishAffinity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}