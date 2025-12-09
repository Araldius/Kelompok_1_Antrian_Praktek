package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.example.kelompok_1_uts_antrian_praktek.ui.activity.MainActivity
import com.example.kelompok_1_uts_antrian_praktek.ui.activity.QueueFormActivity
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.AntrianAdapter
import com.example.kelompok_1_uts_antrian_praktek.ui.viewmodel.MainViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    private lateinit var antrianAdapter: AntrianAdapter

    // Variabel untuk notifikasi agar tidak spam
    private var lastNotifiedNumber: Int = -1

    // Permission Launcher untuk Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Awal (Notifikasi & Recycler)
        createNotificationChannel()
        checkNotificationPermission()
        setupRecycler()

        // 2. Load Data User (Untuk Header "Hallo, Nama")
        viewModel.loadUserProfile()
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            binding.tvWelcomeTitle.text = "Hallo, ${user.fullName}"
        }

        // 3. Load & Observe Antrian (Untuk List & Info)
        viewModel.observeAntrianToday()
        viewModel.antrianList.observe(viewLifecycleOwner) { list ->
            updateUI(list)
        }

        // 4. Tombol Tambah Antrian
        binding.fabAddQueue.setOnClickListener {
            startActivity(Intent(requireContext(), QueueFormActivity::class.java))
        }
    }

    private fun updateUI(list: List<Antrian>) {
        if (list.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvAntrian.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvAntrian.visibility = View.VISIBLE
            antrianAdapter.updateData(list)
        }

        // Logika Info Panel
        val current = list.find { it.status == "Dipanggil" }
        val myQueue = list.find { it.userId == SessionManager.currentLoggedInUserId }

        binding.tvNomorDilayani.text = current?.nomorAntrian?.toString() ?: "-"
        binding.tvNomorAnda.text = myQueue?.nomorAntrian?.toString() ?: "-"

        if (myQueue != null && myQueue.status == "Menunggu") {
            val depan = list.count { it.status == "Menunggu" && it.nomorAntrian < myQueue.nomorAntrian }
            binding.tvEstimasiWaktu.text = "Estimasi ${(depan * 10) + 10} menit"
        } else {
            binding.tvEstimasiWaktu.text = myQueue?.status ?: "Belum ambil"
        }

        // Cek Notifikasi
        checkAndSendNotification(myQueue, current)
    }

    private fun setupRecycler() {
        antrianAdapter = AntrianAdapter(emptyList(), false) { /* Read only here */ }
        binding.rvAntrian.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAntrian.adapter = antrianAdapter
    }

    // --- LOGIKA NOTIFIKASI ---
    private fun checkAndSendNotification(myQueue: Antrian?, current: Antrian?) {
        if (myQueue == null || current == null) return

        val diff = myQueue.nomorAntrian - current.nomorAntrian
        if (current.nomorAntrian == lastNotifiedNumber) return

        // 1. Giliran Hampir Tiba (Kurang 1-2 orang)
        if (myQueue.status == "Menunggu" && diff in 1..2) {
            sendNotification("Giliran Hampir Tiba!", "Persiapkan diri Anda, tinggal $diff antrian lagi.")
            lastNotifiedNumber = current.nomorAntrian
        }

        // 2. Giliran Dipanggil
        if ((myQueue.status == "Menunggu" && diff == 0) || (current.userId == myQueue.userId)) {
            sendNotification("Giliran Anda!", "Nomor ${myQueue.nomorAntrian} sedang dipanggil dokter!")
            lastNotifiedNumber = current.nomorAntrian
        }
    }

    private fun sendNotification(title: String, message: String) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(requireContext(), "CHANNEL_ANTRIAN")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Pastikan icon ada
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireContext())) {
            notify(101, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("CHANNEL_ANTRIAN", "Notifikasi Antrian", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel untuk info panggilan antrian"
            }
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}