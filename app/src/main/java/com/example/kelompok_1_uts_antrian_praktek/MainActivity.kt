package com.example.kelompok_1_uts_antrian_praktek

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var antrianAdapter: AntrianAdapter

    // Firebase Instance
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Variabel untuk mencegah notifikasi bunyi berulang-ulang pada nomor yang sama
    private var lastNotifiedNumber: Int = -1

    // 1. LAUNCHER IZIN NOTIFIKASI (Untuk Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifikasi dimatikan. Anda tidak akan menerima info giliran.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Antrian Hari Ini"

        // Setup Awal
        createNotificationChannel() // Buat saluran notifikasi
        cekDanMintaIzinNotifikasi() // Minta izin pop-up jika perlu

        setupRecyclerView()
        observeFirebaseData()

        // Tombol Navigasi
        binding.fabAddQueue.setOnClickListener {
            startActivity(Intent(this, QueueFormActivity::class.java))
        }
        binding.btnViewSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
    }

    // 2. CEK DAN MINTA IZIN NOTIFIKASI
    private fun cekDanMintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecyclerView() {
        antrianAdapter = AntrianAdapter(emptyList(), false) { antrian ->
            if (antrian.status == "Menunggu") {
                showCancelConfirmationDialog(antrian)
            } else {
                Toast.makeText(this, "Hanya antrian 'Menunggu' yang bisa dibatalkan.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvAntrian.layoutManager = LinearLayoutManager(this)
        binding.rvAntrian.adapter = antrianAdapter
    }

    // 3. LOGIKA UTAMA: MEMBACA DATA REALTIME
    private fun observeFirebaseData() {
        binding.progressBar.visibility = View.VISIBLE

        // Format tanggal harus sama persis dengan nama dokumen di Firebase (yyyy-MM-dd)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Masuk ke sub-collection: antrian -> [TANGGAL] -> pasien
        db.collection("antrian").document(todayDate).collection("pasien")
            .orderBy("nomorAntrian", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    binding.progressBar.visibility = View.GONE
                    // Jangan tampilkan toast error terus menerus jika listener aktif
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val list = mutableListOf<Antrian>()
                    for (doc in snapshots) {
                        val data = doc.toObject(Antrian::class.java)
                        data.id = doc.id // Simpan ID dokumen asli
                        list.add(data)
                    }
                    updateUI(list)
                } else {
                    updateUI(emptyList())
                }
            }
    }

    // 4. UPDATE TAMPILAN LAYAR
    private fun updateUI(list: List<Antrian>) {
        binding.progressBar.visibility = View.GONE

        // Atur tampilan kosong/isi
        if (list.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvAntrian.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvAntrian.visibility = View.VISIBLE
            antrianAdapter.updateData(list)
        }

        // Cari data penting
        val currentUserId = auth.currentUser?.uid
        val currentlyCalled = list.find { it.status == "Dipanggil" }
        val myQueue = list.find { it.userId == currentUserId }

        // Update Text Header
        binding.tvNomorDilayani.text = currentlyCalled?.nomorAntrian?.toString() ?: "-"
        binding.tvNomorAnda.text = myQueue?.nomorAntrian?.toString() ?: "-"

        // Hitung Estimasi Waktu (Logic: 10 menit per orang yg menunggu di depan kita)
        if (myQueue != null && myQueue.status == "Menunggu") {
            val orangDiDepan = list.count { it.status == "Menunggu" && it.nomorAntrian < myQueue.nomorAntrian }
            val estimasiMenit = (orangDiDepan * 10) + 10
            binding.tvEstimasiWaktu.text = "Estimasi ±$estimasiMenit menit lagi"
        } else {
            binding.tvEstimasiWaktu.text = myQueue?.status ?: "Belum ambil antrian"
        }

        // Cek apakah perlu kirim notifikasi
        checkAndSendNotification(myQueue, currentlyCalled)
    }

    // 5. LOGIKA NOTIFIKASI PINTAR
    private fun checkAndSendNotification(myQueue: Antrian?, currentlyCalled: Antrian?) {
        if (myQueue == null || currentlyCalled == null) return

        // Selisih antrian saya dengan yang sedang dipanggil
        val diff = myQueue.nomorAntrian - currentlyCalled.nomorAntrian

        // Cegah notifikasi bunyi berkali-kali jika nomor yang dipanggil masih sama
        if (currentlyCalled.nomorAntrian == lastNotifiedNumber) return

        // Kondisi A: Giliran tinggal 1 atau 2 orang lagi
        if (myQueue.status == "Menunggu" && diff in 1..2) {
            showNotification(
                "Giliran Hampir Tiba!",
                "Bersiaplah! Tinggal $diff antrian lagi menuju giliran Anda."
            )
            lastNotifiedNumber = currentlyCalled.nomorAntrian
        }

        // Kondisi B: Giliran SAYA dipanggil sekarang
        if (myQueue.status == "Menunggu" && diff == 0) {
            showNotification(
                "Giliran Anda!",
                "Nomor ${myQueue.nomorAntrian} sedang dipanggil. Silakan masuk ke ruangan."
            )
            lastNotifiedNumber = currentlyCalled.nomorAntrian
        }
    }

    private fun showNotification(title: String, message: String) {
        // Cek izin manual sebelum menampilkan (Wajib untuk Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "CHANNEL_ANTRIAN")
            .setSmallIcon(R.mipmap.ic_launcher) // Pastikan icon ini ada
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(101, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi Antrian"
            val descriptionText = "Notifikasi saat giliran sudah dekat"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("CHANNEL_ANTRIAN", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 6. DIALOG BATAL ANTRIAN
    private fun showCancelConfirmationDialog(antrian: Antrian) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        AlertDialog.Builder(this)
            .setTitle("Batal Antrian")
            .setMessage("Yakin ingin membatalkan nomor ${antrian.nomorAntrian}?")
            .setPositiveButton("Ya") { _, _ ->
                // Hapus dokumen di Firebase
                db.collection("antrian").document(todayDate)
                    .collection("pasien").document(antrian.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Antrian berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal membatalkan: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    // 7. MENU OPTION (History & Logout)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                return true
            }
            R.id.menu_logout -> {
                auth.signOut()
                startActivity(Intent(this, AuthActivity::class.java))
                finishAffinity() // Tutup semua activity, kembali ke login
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}