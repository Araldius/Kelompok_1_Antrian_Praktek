package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.data.SessionManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentHomeBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.ui.activity.QueueFormActivity
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.AntrianAdapter
import com.example.kelompok_1_uts_antrian_praktek.ui.viewmodel.MainViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var antrianAdapter: AntrianAdapter
    private val db = FirebaseFirestore.getInstance()

    // Variabel agar notifikasi tidak bunyi berulang untuk nomor yang sama
    private var lastNotifiedNumber: Int = -1

    // Launcher untuk minta izin notifikasi (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Notifikasi dimatikan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Saluran Notifikasi (Wajib untuk Android O ke atas)
        createNotificationChannel()
        // 2. Cek Izin
        checkNotificationPermission()

        setupRecycler()

        viewModel.observeAntrianToday()
        viewModel.antrianList.observe(viewLifecycleOwner) { list ->
            updateUI(list)
        }

        binding.fabAddQueue.setOnClickListener {
            startActivity(Intent(requireContext(), QueueFormActivity::class.java))
        }
    }

    private fun updateUI(list: List<Antrian>) {
        // ... (Logika update list seperti sebelumnya) ...
        if (list.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvAntrian.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvAntrian.visibility = View.VISIBLE
            antrianAdapter.updateData(list)
        }

        val current = list.find { it.status == "Dipanggil" }
        val myQueue = list.find { it.userId == SessionManager.currentLoggedInUserId }

        binding.tvNomorDilayani.text = current?.nomorAntrian?.toString() ?: "-"
        binding.tvNomorAnda.text = myQueue?.nomorAntrian?.toString() ?: "-"

        if (myQueue != null && myQueue.status == "Menunggu") {
            val depan = list.count { it.status == "Menunggu" && it.nomorAntrian < myQueue.nomorAntrian }
            binding.tvEstimasiWaktu.text = "Estimasi ${(depan*10)+10} menit"
        } else {
            binding.tvEstimasiWaktu.text = myQueue?.status ?: "Belum ambil"
        }

        // --- LOGIKA NOTIFIKASI (Sesuai Materi M08) ---
        checkAndSendNotification(myQueue, current)
    }

    private fun checkAndSendNotification(myQueue: Antrian?, current: Antrian?) {
        if (myQueue == null || current == null) return

        // Hitung jarak antrian
        val diff = myQueue.nomorAntrian - current.nomorAntrian

        // Cek agar tidak spam (hanya bunyi jika nomor yang dipanggil berubah)
        if (current.nomorAntrian == lastNotifiedNumber) return

        // KONDISI 1: Giliran Hampir Tiba (Kurang dari 2 orang)
        if (myQueue.status == "Menunggu" && diff in 1..2) {
            sendNotification(
                "Giliran Hampir Tiba!",
                "Persiapkan diri Anda, ${diff} antrian lagi menuju giliran Anda."
            )
            lastNotifiedNumber = current.nomorAntrian
        }

        // KONDISI 2: Giliran Pasien Dipanggil
        if (myQueue.status == "Menunggu" && diff == 0) {
            sendNotification(
                "Giliran Anda!",
                "Nomor ${myQueue.nomorAntrian} sedang dipanggil dokter. Silakan masuk."
            )
            lastNotifiedNumber = current.nomorAntrian
        }
    }

    private fun sendNotification(title: String, message: String) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // Jangan kirim jika tidak ada izin
        }

        // Membuat Builder Notifikasi (Slide 531)
        val builder = NotificationCompat.Builder(requireContext(), "CHANNEL_ANTRIAN")
            .setSmallIcon(R.mipmap.ic_launcher) // Ganti dengan R.drawable.ic_app_logo jika ada
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Heads-up notification
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Getar
            .setAutoCancel(true) // Hilang saat diklik

        NotificationManagerCompat.from(requireContext()).notify(101, builder.build())
    }

    private fun createNotificationChannel() {
        // Wajib untuk Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi Antrian"
            val descriptionText = "Channel untuk info antrian klinik"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("CHANNEL_ANTRIAN", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecycler() {
        antrianAdapter = AntrianAdapter(emptyList(), false) {
            // Logic Batal Antrian (Sama seperti sebelumnya)
        }
        binding.rvAntrian.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAntrian.adapter = antrianAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}