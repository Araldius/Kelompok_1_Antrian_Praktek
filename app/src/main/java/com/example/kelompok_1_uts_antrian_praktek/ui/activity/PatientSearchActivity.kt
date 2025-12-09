package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.model.User
import com.example.kelompok_1_uts_antrian_praktek.ui.adapter.UserAdapter
import com.google.firebase.firestore.FirebaseFirestore

class PatientSearchActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: UserAdapter
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_search)

        // 1. Inisialisasi View
        etSearch = findViewById(R.id.et_search_query)
        btnBack = findViewById(R.id.btn_back) // Tombol panah kembali
        val rvList = findViewById<RecyclerView>(R.id.rv_patient_list)

        // 2. LOGIKA TOMBOL BACK (Menutup Activity)
        btnBack.setOnClickListener {
            finish()
        }

        // 3. Setup Recycler View & Adapter
        adapter = UserAdapter(emptyList()) { user ->
            // Saat item pasien diklik, buka HistoryActivity
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("TARGET_USER_ID", user.uid)
            intent.putExtra("TARGET_USER_NAME", user.fullName)
            startActivity(intent)
        }

        rvList.layoutManager = LinearLayoutManager(this)
        rvList.adapter = adapter

        // 4. (Opsional) Listener saat mengetik
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Jika ingin pencarian otomatis saat mengetik, uncomment baris ini:
                // val query = s.toString().trim()
                // if (query.isNotEmpty()) searchPatient(query)
            }
        })

        // 5. Fitur Klik Icon Kaca Pembesar (Drawable Right) di EditText
        etSearch.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                // Hitung area sentuh (apakah pas di ikon kanan?)
                if (event.rawX >= (etSearch.right - etSearch.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {

                    val query = etSearch.text.toString().trim()
                    if (query.isNotEmpty()) {
                        searchPatient(query)
                    } else {
                        Toast.makeText(this, "Ketik nama pasien dahulu", Toast.LENGTH_SHORT).show()
                    }

                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // 6. Fitur Tombol Enter/Search di Keyboard HP
        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchPatient(query)
                }
                true // Menutup keyboard
            } else {
                false
            }
        }
    }

    private fun searchPatient(keyword: String) {
        // Query ke Firestore
        // Mencari user dengan role='pasien' dan namanya mirip keyword
        db.collection("users")
            .whereEqualTo("role", "pasien")
            .whereGreaterThanOrEqualTo("fullName", keyword)
            .whereLessThanOrEqualTo("fullName", keyword + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val users = result.toObjects(User::class.java)

                if (users.isEmpty()) {
                    Toast.makeText(this, "Pasien tidak ditemukan", Toast.LENGTH_SHORT).show()
                }

                adapter.updateData(users)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}