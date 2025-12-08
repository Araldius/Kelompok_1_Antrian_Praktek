package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityMainBinding // REUSE layout activity_main
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.AdminHomeFragment
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.MenuFragment
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.ScheduleAdminFragment

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load Menu Khusus Admin
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_admin)

        // 2. Load Default Fragment
        loadFragment(AdminHomeFragment())

        // 3. Handle Klik Menu
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> loadFragment(AdminHomeFragment())
                R.id.nav_admin_schedule -> loadFragment(ScheduleAdminFragment()) // Admin edit jadwal
                R.id.nav_admin_menu -> loadFragment(MenuFragment())
                else -> false
            }
            true
        }
    }

    // Untuk menangani intent dari ReportActivity jika user tekan back
    // (Opsional, tapi bagus untuk UX)
    override fun onResume() {
        super.onResume()
    }

    private fun loadFragment(f: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, f).commit()
    }
}