package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityMainBinding
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.DoctorHomeFragment
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.MenuFragment
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.ScheduleFragment

class DoctorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding // REUSE layout activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load Menu Khusus Dokter
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_doctor)

        // 2. Load Default Fragment
        loadFragment(DoctorHomeFragment())

        // 3. Handle Klik Menu
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_doctor_home -> loadFragment(DoctorHomeFragment())
                R.id.nav_doctor_schedule -> loadFragment(ScheduleFragment()) // Dokter hanya lihat jadwal
                R.id.nav_doctor_menu -> loadFragment(MenuFragment())
                else -> false
            }
            true
        }
    }

    private fun loadFragment(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, f).commit()
    }
}