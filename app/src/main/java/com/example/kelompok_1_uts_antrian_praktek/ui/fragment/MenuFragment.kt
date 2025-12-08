package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.kelompok_1_uts_antrian_praktek.data.SessionManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentMenuBinding
import com.example.kelompok_1_uts_antrian_praktek.ui.activity.AuthActivity
import com.example.kelompok_1_uts_antrian_praktek.ui.activity.HistoryActivity
import com.example.kelompok_1_uts_antrian_praktek.ui.activity.ReportActivity
import com.example.kelompok_1_uts_antrian_praktek.ui.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth

class MenuFragment : Fragment() {
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadUserProfile()
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            binding.tvProfileName.text = user.fullName
            binding.tvProfileEmail.text = user.email

            // Simpan Role ke SessionManager untuk jaga-jaga
            SessionManager.currentUserRole = user.role

            // --- LOGIKA TOMBOL BERDASARKAN PERAN ---
            when (user.role) {
                "admin" -> {
                    binding.btnRiwayat.text = "Laporan Klinik"
                    binding.btnRiwayat.setOnClickListener {
                        startActivity(Intent(requireContext(), ReportActivity::class.java))
                    }
                }
                "dokter" -> {
                    binding.btnRiwayat.text = "Pasien Selesai Hari Ini"
                    binding.btnRiwayat.setOnClickListener {
                        // Buka HistoryActivity tapi dengan mode Dokter
                        val intent = Intent(requireContext(), HistoryActivity::class.java)
                        intent.putExtra("IS_DOCTOR_VIEW", true)
                        startActivity(intent)
                    }
                }
                else -> { // Pasien
                    binding.btnRiwayat.text = "Riwayat Kunjungan"
                    binding.btnRiwayat.setOnClickListener {
                        startActivity(Intent(requireContext(), HistoryActivity::class.java))
                    }
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finishAffinity()
        }
    }
}