package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentScheduleBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Jadwal
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.ScheduleAdminAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ScheduleAdminAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // false = User biasa (tidak bisa edit)
        adapter = ScheduleAdminAdapter(emptyList(), false) {}

        binding.rvScheduleFragment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScheduleFragment.adapter = adapter

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}