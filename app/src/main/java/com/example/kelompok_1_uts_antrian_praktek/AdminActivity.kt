package com.example.kelompok_1_uts_antrian_praktek

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var antrianAdapter: AntrianAdapter
    private val db = FirebaseFirestore.getInstance()
    private var listPasien: List<Antrian> = listOf() // Simpan data lokal untuk logika tombol

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Dasbor Admin"

        setupRecyclerView()
        observeData()

        // --- FITUR 1: MEMANGGIL PASIEN BERIKUTNYA ---
        binding.btnPanggilBerikutnyaAdmin.setOnClickListener {
            // Cek apakah ada yang sedang dipanggil
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) {
                Toast.makeText(this, "Pasien ${current.namaPasien} sedang dipanggil. Selesaikan dulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cari pasien "Menunggu" dengan nomor terkecil
            val nextPatient = listPasien
                .filter { it.status == "Menunggu" }
                .minByOrNull { it.nomorAntrian }

            if (nextPatient != null) {
                updateStatus(nextPatient.id, "Dipanggil")
            } else {
                Toast.makeText(this, "Tidak ada antrian menunggu.", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol Selesai (Opsional, jika admin juga ingin menyelesaikan)
        binding.btnSelesaiAdmin.setOnClickListener {
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) updateStatus(current.id, "Selesai")
            else Toast.makeText(this, "Belum ada pasien dipanggil.", Toast.LENGTH_SHORT).show()
        }

        // --- FITUR 2: TAMBAH ANTRIAN MANUAL ---
        binding.fabAddManual.setOnClickListener {
            showAddManualDialog()
        }
    }

    // Fungsi Dialog Tambah Manual
    private fun showAddManualDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_manual)

        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        val etNama = dialog.findViewById<EditText>(R.id.et_nama_pasien_manual)
        val etKeluhan = dialog.findViewById<EditText>(R.id.et_keluhan_manual)
        val btnSimpan = dialog.findViewById<Button>(R.id.btn_simpan_manual) // Pastikan ID tombol di XML dialog ada, atau tambahkan tombol programmatically jika belum ada

        dialog.dismiss()
        showNativeAddDialog()
    }

    private fun showNativeAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_manual, null)
        val etNama = view.findViewById<EditText>(R.id.et_nama_pasien_manual)
        val etKeluhan = view.findViewById<EditText>(R.id.et_keluhan_manual)

        AlertDialog.Builder(this)
            .setTitle("Tambah Antrian Manual")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val nama = etNama.text.toString()
                val keluhan = etKeluhan.text.toString()
                if (nama.isNotEmpty()) {
                    simpanAntrianManual(nama, keluhan)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanAntrianManual(nama: String, keluhan: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val ref = db.collection("antrian").document(todayDate).collection("pasien")

        ref.get().addOnSuccessListener { result ->
            val nomorBaru = result.size() + 1
            val data = hashMapOf(
                "namaPasien" to nama,
                "nomorAntrian" to nomorBaru,
                "status" to "Menunggu",
                "userId" to "admin_manual_entry",
                "tanggal" to todayDate,
                "keluhan" to "$keluhan (Manual)"
            )

            ref.add(data).addOnSuccessListener {
                Toast.makeText(this, "Berhasil ditambahkan: No $nomorBaru", Toast.LENGTH_SHORT).show()
                db.collection("antrian").document(todayDate).set(mapOf("lastUpdate" to Date()))
            }
        }
    }

    private fun updateStatus(id: String, status: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("antrian").document(todayDate)
            .collection("pasien").document(id)
            .update("status", status)
    }

    private fun setupRecyclerView() {
        antrianAdapter = AntrianAdapter(emptyList(), true) { antrian ->
            showDeleteConfirmationDialog(antrian)
        }
        binding.rvAntrianAdmin.layoutManager = LinearLayoutManager(this)
        binding.rvAntrianAdmin.adapter = antrianAdapter
    }

    private fun observeData() {
        binding.progressBarAdmin.visibility = View.VISIBLE
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("antrian").document(todayDate).collection("pasien")
            .addSnapshotListener { snapshots, e ->
                binding.progressBarAdmin.visibility = View.GONE
                if (snapshots != null) {
                    val list = snapshots.toObjects(Antrian::class.java)
                    list.forEachIndexed { i, item -> item.id = snapshots.documents[i].id }
                    listPasien = list // Simpan ke global variable

                    updateUI(list)
                }
            }
    }

    private fun updateUI(list: List<Antrian>) {
        if (list.isEmpty()) {
            binding.tvEmptyStateAdmin.visibility = View.VISIBLE
            binding.rvAntrianAdmin.visibility = View.GONE
            binding.tvTotalPasien.text = "Total Pasien: 0"
        } else {
            binding.tvEmptyStateAdmin.visibility = View.GONE
            binding.rvAntrianAdmin.visibility = View.VISIBLE
            binding.tvTotalPasien.text = "Total Pasien: ${list.size}"
            // Admin melihat semua, urutkan dari nomor terkecil
            antrianAdapter.updateData(list.sortedBy { it.nomorAntrian })
        }

        val current = list.find { it.status == "Dipanggil" }
        binding.tvNamaPasienDipanggilAdmin.text = current?.namaPasien ?: "-"
    }

    private fun showDeleteConfirmationDialog(antrian: Antrian) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        AlertDialog.Builder(this)
            .setTitle("Hapus Antrian")
            .setMessage("Hapus antrian ${antrian.namaPasien} (No. ${antrian.nomorAntrian})?")
            .setPositiveButton("Hapus") { _, _ ->
                db.collection("antrian").document(todayDate)
                    .collection("pasien").document(antrian.id).delete()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_manage_schedule -> {
                // FITUR 3: MENGATUR JADWAL
                startActivity(Intent(this, ScheduleAdminActivity::class.java))
                true
            }
            R.id.menu_report -> {
                startActivity(Intent(this, ReportActivity::class.java))
                true
            }
            R.id.menu_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, AuthActivity::class.java))
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}