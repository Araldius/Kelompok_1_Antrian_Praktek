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
import java.text.SimpleDateFormat
import java.util.*

class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var antrianAdapter: AntrianAdapter
    private val db = FirebaseFirestore.getInstance()
    private var listPasien: List<Antrian> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        observeData()

        binding.btnPanggilBerikutnyaAdmin.setOnClickListener {
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) {
                Toast.makeText(context, "Selesaikan dulu yang dipanggil", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val next = listPasien.filter { it.status == "Menunggu" }.minByOrNull { it.nomorAntrian }
            if (next != null) updateStatus(next.id, "Dipanggil")
            else Toast.makeText(context, "Antrian kosong", Toast.LENGTH_SHORT).show()
        }

        binding.btnSelesaiAdmin.setOnClickListener {
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) updateStatus(current.id, "Selesai")
        }

        binding.fabAddManual.setOnClickListener { showAddManualDialog() }
    }

    private fun showAddManualDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_manual, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()

        val btnSimpan = view.findViewById<Button>(R.id.btn_simpan_manual)
        val etNama = view.findViewById<EditText>(R.id.et_nama_pasien_manual)
        val etKeluhan = view.findViewById<EditText>(R.id.et_keluhan_manual)

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim() // Hapus spasi depan/belakang
            val keluhan = etKeluhan.text.toString().trim()

            if(nama.isNotEmpty()) {
                // UPDATE: Jangan langsung simpan, cari dulu usernya
                cariUserDanSimpan(nama, keluhan)
                dialog.dismiss()
            } else {
                etNama.error = "Nama wajib diisi"
            }
        }
        dialog.show()
    }

    // --- FITUR PENCARIAN USER ---
    private fun cariUserDanSimpan(nama: String, keluhan: String) {
        // Cari di koleksi 'users' apakah ada nama yang persis sama
        db.collection("users")
            .whereEqualTo("fullName", nama)
            .limit(1) // Ambil 1 saja jika ada kembar
            .get()
            .addOnSuccessListener { documents ->
                var userIdTarget = "admin_manual" // Default ID jika user tidak ketemu

                if (!documents.isEmpty) {
                    // USER KETEMU! Pakai UID aslinya
                    val userDoc = documents.documents[0]
                    userIdTarget = userDoc.getString("uid") ?: userDoc.id

                    Toast.makeText(context, "Akun ditemukan! Data akan masuk ke riwayat ${userDoc.getString("email")}", Toast.LENGTH_LONG).show()
                } else {
                    // USER TIDAK KETEMU
                    Toast.makeText(context, "Akun tidak ditemukan. Disimpan sebagai Pasien Manual.", Toast.LENGTH_SHORT).show()
                }

                // Lanjut simpan dengan userId yang sudah ditentukan (Asli atau Manual)
                simpanKeDatabase(nama, keluhan, userIdTarget)
            }
            .addOnFailureListener {
                // Jika error koneksi, tetap simpan sebagai manual
                simpanKeDatabase(nama, keluhan, "admin_manual")
            }
    }

    private fun simpanKeDatabase(nama: String, keluhan: String, userId: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Cek nomor antrian terakhir
        db.collection("antrian").document(today).collection("pasien").get().addOnSuccessListener { res ->
            val no = res.size() + 1

            val data = Antrian(
                namaPasien = nama,
                nomorAntrian = no,
                status = "Menunggu",
                userId = userId, // <--- Ini kuncinya (UID Asli atau "admin_manual")
                tanggal = today,
                keluhan = "$keluhan (Manual)"
            )

            db.collection("antrian").document(today).collection("pasien").add(data).addOnSuccessListener {
                // Trigger update waktu agar admin dashboard refresh
                db.collection("antrian").document(today).set(mapOf("lastUpdate" to Date()))
            }
        }
    }

    private fun updateStatus(id: String, status: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("antrian").document(today).collection("pasien").document(id).update("status", status)
    }

    private fun observeData() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("antrian").document(today).collection("pasien").addSnapshotListener { s, _ ->
            if (s != null) {
                listPasien = s.toObjects(Antrian::class.java).onEachIndexed { i, d -> d.id = s.documents[i].id }
                updateUI()
            }
        }
    }

    private fun updateUI() {
        val current = listPasien.find { it.status == "Dipanggil" }
        binding.tvNamaPasienDipanggilAdmin.text = current?.namaPasien ?: "-"

        val totalSelesai = listPasien.count { it.status == "Selesai" }
        val totalMenunggu = listPasien.count { it.status == "Menunggu" || it.status == "Dipanggil" }

        binding.tvTotalPasien.text = "Total: ${listPasien.size} | Selesai: $totalSelesai | Menunggu: $totalMenunggu"

        antrianAdapter.updateData(listPasien.sortedBy { it.nomorAntrian })
        binding.tvEmptyStateAdmin.visibility = if (listPasien.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRecycler() {
        antrianAdapter = AntrianAdapter(emptyList(), true) { antrian ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus")
                .setMessage("Hapus ${antrian.namaPasien}?")
                .setPositiveButton("Ya") { _,_ ->
                    db.collection("antrian").document(today).collection("pasien").document(antrian.id).delete()
                }
                .setNegativeButton("Batal", null).show()
        }
        binding.rvAntrianAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAntrianAdmin.adapter = antrianAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}