package com.example.kelompok_1_uts_antrian_praktek.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kelompok_1_uts_antrian_praktek.R
import com.example.kelompok_1_uts_antrian_praktek.model.User

class UserAdapter(
    private var list: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_user_name)
        val tvEmail: TextView = view.findViewById(R.id.tv_user_email)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = list[position]
        holder.tvName.text = user.fullName
        holder.tvEmail.text = user.email
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<User>) {
        list = newList
        notifyDataSetChanged()
    }
}