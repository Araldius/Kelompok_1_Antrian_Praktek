package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentDoctorHomeBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.model.RekamMedis
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.AntrianAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DoctorHomeFragment : Fragment() {

    private var _binding: FragmentDoctorHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var antrianAdapter: AntrianAdapter
    private val db = FirebaseFirestore.getInstance()
    private var listPasien: List<Antrian> = listOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDoctorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        observeData()
        observeStatusKlinik()

        binding.btnPanggilBerikutnya.setOnClickListener {
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) {
                Toast.makeText(context, "Selesaikan dulu pasien ini", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val next = listPasien.filter { it.status == "Menunggu" }.minByOrNull { it.nomorAntrian }
            if (next != null) updateStatus(next.id, "Dipanggil")
            else Toast.makeText(context, "Tidak ada pasien", Toast.LENGTH_SHORT).show()
        }

        binding.btnSelesai.setOnClickListener {
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) showRekamMedisDialog(current)
            else Toast.makeText(context, "Belum ada yang dipanggil", Toast.LENGTH_SHORT).show()
        }

        binding.btnPindahAkhir.setOnClickListener {
            val current = listPasien.find { it.status == "Dipanggil" }
            if (current != null) {
                val max = listPasien.maxOfOrNull { it.nomorAntrian } ?: 0
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                db.collection("antrian").document(today).collection("pasien").document(current.id)
                    .update(mapOf("status" to "Menunggu", "nomorAntrian" to max + 1))
            }
        }

        binding.switchStatusPraktek.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "buka" else "tutup"
            db.collection("statusKlinik").document("hariIni").set(mapOf("status" to status))
        }
    }

    private fun showRekamMedisDialog(pasien: Antrian) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etDiagnosa = EditText(requireContext()).apply { hint = "Diagnosa" }
        val etObat = EditText(requireContext()).apply { hint = "Resep Obat" }
        layout.addView(etDiagnosa)
        layout.addView(etObat)

        AlertDialog.Builder(requireContext())
            .setTitle("Rekam Medis")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val rm = RekamMedis(
                    pasienId = pasien.userId,
                    namaPasien = pasien.namaPasien,
                    keluhanAwal = pasien.keluhan,
                    diagnosa = etDiagnosa.text.toString(),
                    pengobatan = etObat.text.toString(),
                    tanggal = pasien.tanggal
                )
                db.collection("rekam_medis").add(rm).addOnSuccessListener {
                    updateStatus(pasien.id, "Selesai")
                    Toast.makeText(context, "Disimpan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
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

    private fun observeStatusKlinik() {
        db.collection("statusKlinik").document("hariIni").addSnapshotListener { s, _ ->
            if (s != null && s.exists()) {
                val buka = s.getString("status") == "buka"
                if (binding.switchStatusPraktek.isChecked != buka) binding.switchStatusPraktek.isChecked = buka
                binding.switchStatusPraktek.text = if (buka) "Pendaftaran Buka" else "Pendaftaran Tutup"
            }
        }
    }

    private fun updateUI() {
        val current = listPasien.find { it.status == "Dipanggil" }
        binding.tvNamaPasienDipanggil.text = current?.namaPasien ?: "-"
        binding.tvKeluhanDipanggil.text = "Keluhan: ${current?.keluhan ?: "-"}"

        binding.btnSelesai.isEnabled = current != null
        binding.btnPindahAkhir.isEnabled = current != null
        binding.btnPanggilBerikutnya.isEnabled = current == null

        val waiting = listPasien.filter { it.status == "Menunggu" }.sortedBy { it.nomorAntrian }
        antrianAdapter.updateData(waiting)
        binding.tvEmptyStateDokter.visibility = if (waiting.isEmpty()) View.VISIBLE else View.GONE

        val doneCount = listPasien.count { it.status == "Selesai" }
        binding.tvTotalSelesaiHariIni.text = "Pasien Selesai: $doneCount"
    }

    private fun setupRecycler() {
        antrianAdapter = AntrianAdapter(emptyList(), false) {}
        binding.rvAntrianDokter.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAntrianDokter.adapter = antrianAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}