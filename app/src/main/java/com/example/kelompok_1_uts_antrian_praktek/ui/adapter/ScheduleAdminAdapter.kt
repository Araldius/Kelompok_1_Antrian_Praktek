package com.example.kelompok_1_uts_antrian_praktek.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kelompok_1_uts_antrian_praktek.databinding.ListItemScheduleAdminBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Jadwal

class ScheduleAdminAdapter(
    private var list: List<Jadwal>,
    private val isAdmin: Boolean,
    private val onEdit: (Jadwal) -> Unit
) : RecyclerView.Adapter<ScheduleAdminAdapter.VH>() {

    inner class VH(val binding: ListItemScheduleAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Pastikan ini menggunakan 'ListItemScheduleAdminBinding'
        // Dan pastikan import-nya benar (kadang ada import yang nyasar ke layout lain)
        val binding = ListItemScheduleAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val jadwal = list[position]

        // --- BAGIAN PENTING: MENAMPILKAN NAMA HARI ---
        // Mengambil field 'hari' dari Firebase (contoh: "Senin")
        holder.binding.tvDayName.text = jadwal.hari
        // ---------------------------------------------

        if (jadwal.status.equals("Buka", true)) {
            holder.binding.tvScheduleInfo.text = "${jadwal.jamBuka} - ${jadwal.jamTutup}"
        } else {
            holder.binding.tvScheduleInfo.text = "Tutup"
        }

        // Sembunyikan tombol Edit jika bukan Admin (Pasien)
        if (isAdmin) {
            holder.binding.btnEditSchedule.visibility = View.VISIBLE
            holder.binding.btnEditSchedule.setOnClickListener { onEdit(jadwal) }
        } else {
            holder.binding.btnEditSchedule.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Jadwal>) {
        this.list = newList
        notifyDataSetChanged()
    }
}