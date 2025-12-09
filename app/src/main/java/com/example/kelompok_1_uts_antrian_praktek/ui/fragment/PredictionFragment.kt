package com.example.kelompok_1_uts_antrian_praktek.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kelompok_1_uts_antrian_praktek.data.remote.RetrofitInstance
import com.example.kelompok_1_uts_antrian_praktek.databinding.FragmentPredictionBinding
import kotlinx.coroutines.launch

class PredictionFragment : Fragment() {

    private var _binding: FragmentPredictionBinding? = null
    private val binding get() = _binding!!

    private var calculatedBmi: Float = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPredictionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBmiCalculator()

        binding.btnPredict.setOnClickListener {
            if (validateInputs()) {
                performPrediction()
            } else {
                Toast.makeText(requireContext(), "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBmiCalculator() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { calculateBmi() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.etWeight.addTextChangedListener(watcher)
        binding.etHeight.addTextChangedListener(watcher)
    }

    private fun calculateBmi() {
        val weightStr = binding.etWeight.text.toString()
        val heightStr = binding.etHeight.text.toString()

        if (weightStr.isNotEmpty() && heightStr.isNotEmpty()) {
            val weight = weightStr.toFloatOrNull() ?: 0f
            val heightCm = heightStr.toFloatOrNull() ?: 0f

            if (weight > 0 && heightCm > 0) {
                val heightM = heightCm / 100
                calculatedBmi = weight / (heightM * heightM)
                binding.tvBmiResult.text = String.format("BMI Anda: %.2f", calculatedBmi)
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (binding.etAge.text.isEmpty()) return false
        if (binding.rgSex.checkedRadioButtonId == -1) return false
        if (binding.rgPhysical.checkedRadioButtonId == -1) return false
        if (binding.etSleepHours.text.isEmpty()) return false

        if (calculatedBmi <= 0) {
            Toast.makeText(context, "Isi Berat dan Tinggi Badan dengan benar", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun performPrediction() {
        binding.cardResult.visibility = View.VISIBLE
        binding.tvResult.text = "Sedang menganalisis data..."
        binding.btnPredict.isEnabled = false

        lifecycleScope.launch {
            try {
                // 1. SIAPKAN DATA INPUT
                val age = binding.etAge.text.toString().toInt()
                val sleep = binding.etSleepHours.text.toString().toFloat()

                val sexVal = if (binding.rbMale.isChecked) 1 else 0
                val physVal = if (binding.rbPhysicalYes.isChecked) 1 else 0

                // 2. BUNGKUS KE MAP
                val inputData = mapOf<String, Any>(
                    "age" to age,
                    "BMI" to calculatedBmi,
                    "sleep_hours" to sleep,
                    "sex" to sexVal,
                    "physical_activity" to physVal
                )

                // 3. KIRIM KE SERVER
                val response = RetrofitInstance.api.getPrediction(inputData)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // 4. BACA RESPON
                    if (body.status == "success") {
                        val sb = StringBuilder("--- HASIL ANALISIS ---\n\n")

                        body.data?.forEach { (penyakit, nilai) ->
                            val namaPenyakit = penyakit.replace("_", " ").uppercase()

                            // --- LOGIKA UTAMA DIUBAH DI SINI ---
                            // Menentukan status berdasarkan nilai probabilitas (Threshold 0.5)
                            val statusRisiko = if (nilai > 0.5) "⚠️ BERISIKO" else "✅ AMAN"

                            // Hanya menampilkan Nama Penyakit dan Status
                            sb.append("$namaPenyakit : $statusRisiko\n")
                        }

                        binding.tvResult.text = sb.toString()
                    } else {
                        binding.tvResult.text = "Gagal: ${body.message}"
                    }
                } else {
                    binding.tvResult.text = "Server Error: ${response.code()}"
                }

            } catch (e: Exception) {
                binding.tvResult.text = "Koneksi Gagal: ${e.message}\nPastikan IP Address benar."
                e.printStackTrace()
            } finally {
                binding.btnPredict.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}