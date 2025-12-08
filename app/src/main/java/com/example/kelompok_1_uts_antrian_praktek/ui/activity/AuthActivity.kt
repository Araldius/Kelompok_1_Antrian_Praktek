package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.data.SessionManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityAuthBinding
import com.example.kelompok_1_uts_antrian_praktek.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        // Cek Auto Login
        if (auth.currentUser != null) {
            checkRoleAndRedirect(auth.currentUser!!.email ?: "")
        }

        binding.btnAuthAction.setOnClickListener { handleAuth() }

        binding.tvToggleMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateUI()
        }

        // Set text awal (support HTML tag <b>)
        updateUI()
    }

    private fun handleAuth() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }

        if (isRegisterMode) {
            // --- MODE REGISTRASI ---
            if (fullName.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                return
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid
                    // Simpan data user ke Firestore
                    val newUser = User(uid, email, fullName, "pasien")

                    db.collection("users").document(uid).set(newUser)
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                            // Kembali ke mode login atau langsung login
                            SessionManager.currentLoggedInUserId = uid
                            checkRoleAndRedirect(email)
                        }
                }
                .addOnFailureListener {
                    val msg = getString(R.string.error_register_failed, it.message)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }

        } else {
            // --- MODE LOGIN ---
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    SessionManager.currentLoggedInUserId = result.user!!.uid
                    checkRoleAndRedirect(email)
                }
                .addOnFailureListener {
                    val msg = getString(R.string.error_login_failed, it.message)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun checkRoleAndRedirect(email: String) {
        // Logika sederhana cek role via email string (sesuai request sebelumnya)
        // Idealnya cek field 'role' di Firestore, tapi ini untuk kecepatan demo/UTS
        if (email.contains("dokter")) {
            startActivity(Intent(this, DoctorActivity::class.java))
        } else if (email.contains("admin")) {
            startActivity(Intent(this, AdminActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun updateUI() {
        if (isRegisterMode) {
            binding.tvAuthTitle.text = getString(R.string.auth_title_register)
            binding.tilFullName.visibility = View.VISIBLE // Tampilkan input nama
            binding.btnAuthAction.text = getString(R.string.btn_register)
            binding.tvToggleMode.text = Html.fromHtml(getString(R.string.text_have_account), Html.FROM_HTML_MODE_LEGACY)
        } else {
            binding.tvAuthTitle.text = getString(R.string.auth_title_login)
            binding.tilFullName.visibility = View.GONE // Sembunyikan input nama
            binding.btnAuthAction.text = getString(R.string.btn_login)
            binding.tvToggleMode.text = Html.fromHtml(getString(R.string.text_no_account), Html.FROM_HTML_MODE_LEGACY)
        }
    }
}