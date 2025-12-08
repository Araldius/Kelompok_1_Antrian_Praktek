package com.example.kelompok_1_uts_antrian_praktek.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.databinding.ActivityMainBinding
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.HomeFragment
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.MenuFragment
import com.example.kelompok_1_uts_antrian_praktek.ui.fragment.ScheduleFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load halaman pertama (Home)
        loadFragment(HomeFragment())

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_schedule -> {
                    loadFragment(ScheduleFragment())
                    true
                }
                R.id.nav_menu -> {
                    loadFragment(MenuFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}