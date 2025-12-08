package com.example.kelompok_1_uts_antrian_praktek.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian
import com.example.kelompok_1_uts_antrian_praktek.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _antrianList = MutableLiveData<List<Antrian>>()
    val antrianList: LiveData<List<Antrian>> = _antrianList

    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> = _userData

    // Load Data User untuk Profile
    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _userData.value = document.toObject(User::class.java)
                }
            }
    }

    // Load Antrian Realtime Hari Ini
    fun observeAntrianToday() {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("antrian").document(todayDate).collection("pasien")
            .orderBy("nomorAntrian", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val list = mutableListOf<Antrian>()
                    for (doc in snapshots) {
                        val data = doc.toObject(Antrian::class.java)
                        data.id = doc.id
                        list.add(data)
                    }
                    _antrianList.value = list
                }
            }
    }
}