package com.example.kelompok_1_uts_antrian_praktek.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.data.SessionManager
import com.example.kelompok_1_uts_antrian_praktek.databinding.ListItemAntrianBinding
import com.example.kelompok_1_uts_antrian_praktek.model.Antrian

class AntrianAdapter(
    private var antrianList: List<Antrian>,
    private val isAdmin: Boolean,
    private val onItemClicked: (Antrian) -> Unit
) : RecyclerView.Adapter<AntrianAdapter.AntrianViewHolder>() {

    inner class AntrianViewHolder(val binding: ListItemAntrianBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AntrianViewHolder {
        val binding = ListItemAntrianBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AntrianViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AntrianViewHolder, position: Int) {
        val antrian = antrianList[position]
        val context = holder.itemView.context

        holder.binding.apply {
            tvNomorAntrian.text = antrian.nomorAntrian.toString()
            tvNamaPasien.text = antrian.namaPasien
            tvStatus.text = "${antrian.status} • ${antrian.keluhan}"

            when (antrian.status) {
                "Dipanggil" -> tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary)) // Pastikan warna ini ada di colors.xml
                "Selesai" -> tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                else -> tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark))
            }

            // Highlight punya sendiri
            if (antrian.userId == SessionManager.currentLoggedInUserId) {
                root.strokeWidth = 4
                root.strokeColor = ContextCompat.getColor(context, R.color.green_primary)
            } else {
                root.strokeWidth = 0
            }
        }

        holder.itemView.setOnClickListener {
            if (isAdmin) {
                onItemClicked(antrian)
            } else {
                if (antrian.userId == SessionManager.currentLoggedInUserId) {
                    onItemClicked(antrian)
                }
            }
        }
    }

    override fun getItemCount(): Int = antrianList.size

    fun updateData(newAntrianList: List<Antrian>) {
        this.antrianList = newAntrianList
        notifyDataSetChanged()
    }
}