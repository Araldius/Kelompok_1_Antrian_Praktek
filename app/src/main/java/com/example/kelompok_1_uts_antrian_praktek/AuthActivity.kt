package com.example.kelompok_1_uts_antrian_praktek

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityAuthBinding // Perhatikan import binding
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        // Auto-login jika sesi masih ada
        if (auth.currentUser != null) {
            cekRoleDanPindah(auth.currentUser!!.email ?: "")
        }

        binding.btnAuthAction.setOnClickListener { handleAuthAction() }

        binding.tvToggleMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateUiForMode()
        }
    }

    private fun handleAuthAction() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Isi email dan password", Toast.LENGTH_SHORT).show()
            return
        }

        if (isRegisterMode) {
            // REGISTER
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                    DummyData.currentLoggedInUserId = it.user?.uid
                    cekRoleDanPindah(email)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // LOGIN
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    DummyData.currentLoggedInUserId = it.user?.uid
                    cekRoleDanPindah(it.user?.email ?: "")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun cekRoleDanPindah(email: String) {
        // Logika role sederhana berdasarkan email (sesuai screenshot user Anda)
        when {
            email.contains("dokter") -> {
                DummyData.currentUserRole = "dokter"
                startActivity(Intent(this, DoctorActivity::class.java))
            }
            email.contains("admin") -> {
                DummyData.currentUserRole = "admin"
                startActivity(Intent(this, AdminActivity::class.java))
            }
            else -> {
                DummyData.currentUserRole = "pasien"
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
        finish()
    }

    private fun updateUiForMode() {
        if (isRegisterMode) {
            binding.authTitle.text = "Buat Akun Baru"
            binding.btnAuthAction.text = "Daftar"
            binding.tvToggleMode.text = Html.fromHtml("Sudah punya akun? <b>Login di sini</b>", Html.FROM_HTML_MODE_LEGACY)
        } else {
            binding.authTitle.text = "Selamat Datang"
            binding.btnAuthAction.text = "Login"
            binding.tvToggleMode.text = Html.fromHtml("Belum punya akun? <b>Registrasi di sini</b>", Html.FROM_HTML_MODE_LEGACY)
        }
    }
}