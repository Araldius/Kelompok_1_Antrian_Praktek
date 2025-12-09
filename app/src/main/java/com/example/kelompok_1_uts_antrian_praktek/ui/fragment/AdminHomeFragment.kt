package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentAdminHomeBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.AntrianAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var antrianAdapter: AntrianAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAntrianHariIni()

        // Tombol Tambah Antrian Manual
        binding.fabAddManual.setOnClickListener {
            showAddManualDialog()
        }
    }

    private fun setupRecyclerView() {
        // Menggunakan adapter yang sama, enableDelete = true agar Admin bisa hapus
        antrianAdapter = AntrianAdapter(emptyList(), true) { antrian ->
            hapusAntrian(antrian)
        }
        binding.rvAdminAntrian.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminAntrian.adapter = antrianAdapter
    }

    private fun loadAntrianHariIni() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("antrian").document(today).collection("pasien")
            .orderBy("nomorAntrian", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val list = snapshots.toObjects(Antrian::class.java)
                    antrianAdapter.updateData(list)

                    if (list.isEmpty()) {
                        binding.tvEmptyStateAdmin.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyStateAdmin.visibility = View.GONE
                    }
                }
            }
    }

    // --- LOGIKA TAMBAH MANUAL ---

    private fun showAddManualDialog() {
        // Inflate layout dialog kustom
        val view = layoutInflater.inflate(R.layout.dialog_add_manual, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tambah Pasien Manual")
            .setView(view)
            .create()

        val etNama = view.findViewById<EditText>(R.id.et_nama_pasien_manual)
        val etKeluhan = view.findViewById<EditText>(R.id.et_keluhan_manual)
        val btnSimpan = view.findViewById<Button>(R.id.btn_simpan_manual)

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val keluhan = etKeluhan.text.toString().trim()

            if (nama.isNotEmpty() && keluhan.isNotEmpty()) {
                // Jangan langsung simpan, tapi CARI dulu usernya
                btnSimpan.isEnabled = false // Cegah klik ganda
                btnSimpan.text = "Mengecek..."
                cariUserDanSimpan(nama, keluhan, dialog)
            } else {
                Toast.makeText(context, "Nama dan Keluhan wajib diisi", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun cariUserDanSimpan(nama: String, keluhan: String, dialog: AlertDialog) {
        // Query ke koleksi users untuk mencari nama yang sama persis
        db.collection("users")
            .whereEqualTo("fullName", nama)
            .limit(1) // Ambil 1 saja yang cocok
            .get()
            .addOnSuccessListener { documents ->
                var userIdTarget = "admin_manual" // Default (Guest)
                var pesan = "Menambahkan sebagai Pasien Manual"

                if (!documents.isEmpty) {
                    // USER KETEMU!
                    val userDoc = documents.documents[0]
                    userIdTarget = userDoc.getString("uid") ?: "admin_manual"
                    val emailUser = userDoc.getString("email")
                    pesan = "Terhubung ke akun: $emailUser"
                }

                Toast.makeText(context, pesan, Toast.LENGTH_SHORT).show()
                prosesSimpanKeFirebase(nama, keluhan, userIdTarget, dialog)
            }
            .addOnFailureListener {
                // Jika error koneksi saat mencari, tetap simpan sebagai manual
                prosesSimpanKeFirebase(nama, keluhan, "admin_manual", dialog)
            }
    }

    private fun prosesSimpanKeFirebase(nama: String, keluhan: String, userId: String, dialog: AlertDialog) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Cek jumlah antrian saat ini untuk menentukan nomor urut
        db.collection("antrian").document(today).collection("pasien").get()
            .addOnSuccessListener { result ->
                val nomorBaru = result.size() + 1

                val antrianBaru = Antrian(
                    id = "", // Akan diisi otomatis oleh Firestore atau bisa dikosongkan
                    namaPasien = nama,
                    nomorAntrian = nomorBaru,
                    status = "Menunggu",
                    userId = userId, // ID User Asli atau "admin_manual"
                    tanggal = today,
                    keluhan = "$keluhan (Input Admin)"
                )

                db.collection("antrian").document(today).collection("pasien")
                    .add(antrianBaru)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Berhasil ditambahkan! No: $nomorBaru", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
                        dialog.dismiss() // Atau biarkan terbuka
                    }
            }
    }

    private fun hapusAntrian(antrian: Antrian) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (antrian.id.isNotEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Antrian?")
                .setMessage("Apakah Anda yakin ingin menghapus antrian ${antrian.namaPasien}?")
                .setPositiveButton("Hapus") { _, _ ->
                    db.collection("antrian").document(today).collection("pasien")
                        .document(antrian.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Antrian dihapus", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}