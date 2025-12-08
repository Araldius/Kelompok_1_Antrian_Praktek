package com.example.kelompok_1_uts_antrian_praktek

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kelompok_1_uts_antrian_praktek.databinding.ListItemAntrianBinding

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
            tvStatus.text = "${antrian.status} • ${antrian.keluhan}" // Tampilkan keluhan sedikit

            // Warna Status
            when (antrian.status) {
                "Dipanggil" -> tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_primary))
                "Selesai" -> tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_selesai))
                else -> tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_menunggu))
            }

            // Highlight punya sendiri
            if (antrian.userId == DummyData.currentLoggedInUserId) {
                root.strokeWidth = 4
                root.strokeColor = ContextCompat.getColor(context, R.color.green_light)
            } else {
                root.strokeWidth = 0
            }
        }

        holder.itemView.setOnClickListener {
            if (isAdmin) {
                onItemClicked(antrian)
            } else {
                if (antrian.userId == DummyData.currentLoggedInUserId) {
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