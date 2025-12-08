package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentScheduleAdminBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Jadwal
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.ScheduleAdminAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleAdminFragment : Fragment() {
    private var _binding: FragmentScheduleAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ScheduleAdminAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ScheduleAdminAdapter(emptyList(), true) { jadwal -> showEditDialog(jadwal) }
        binding.rvScheduleAdmin.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScheduleAdmin.adapter = adapter

        loadJadwal()
    }

    private fun loadJadwal() {
        // Ambil data dari Firebase
        db.collection("jadwalPraktek")
            .orderBy("urutan") // OPTIMASI: Kita bisa minta Firebase mengurutkannya langsung!
            .get()
            .addOnSuccessListener { res ->
                val list = res.toObjects(Jadwal::class.java).onEach {
                    // Pastikan nama hari tampil (mengambil dari ID jika field hari kosong)
                    if (it.hari.isEmpty()) it.hari = it.id
                }

                // Masukkan ke adapter (Data sudah urut dari Firebase karena .orderBy)
                adapter.updateData(list)
            }
            .addOnFailureListener {
                // Handle error
            }
    }

    private fun showEditDialog(jadwal: Jadwal) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etBuka = EditText(requireContext()).apply { hint = "Jam Buka"; setText(jadwal.jamBuka) }
        val etTutup = EditText(requireContext()).apply { hint = "Jam Tutup"; setText(jadwal.jamTutup) }

        layout.addView(etBuka)
        layout.addView(etTutup)

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah ${jadwal.hari}")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val status = if (etBuka.text.isEmpty()) "Tutup" else "Buka"
                db.collection("jadwalPraktek").document(jadwal.id).update(
                    mapOf("jamBuka" to etBuka.text.toString(), "jamTutup" to etTutup.text.toString(), "status" to status)
                ).addOnSuccessListener { loadJadwal() }
            }
            .setNegativeButton("Batal", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}