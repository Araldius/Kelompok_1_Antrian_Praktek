package com.example.kelompok_1_uts_antrian_praktek.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData untuk Daftar Antrian Hari Ini
    private val _antrianList = MutableLiveData<List<Antrian>>()
    val antrianList: LiveData<List<Antrian>> get() = _antrianList

    // LiveData untuk Data User (Agar bisa sapa nama)
    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> get() = _userData

    // --- FUNGSI 1: Ambil Data User yang Sedang Login ---
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        _userData.value = user
                    }
                }
            }
    }

    // --- FUNGSI 2: Ambil/Dengar Antrian Hari Ini (Realtime) ---
    fun observeAntrianToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("antrian").document(today).collection("pasien")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    val list = snapshots.toObjects(Antrian::class.java)
                    // Urutkan berdasarkan nomor antrian
                    _antrianList.value = list.sortedBy { it.nomorAntrian }
                }
            }
    }
}