package com.ganadopro.ui.produccion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.Produccion
import com.ganadopro.databinding.ItemProduccionBinding

class ProduccionAdapter(
    private val onDelete: (Produccion) -> Unit,
    private val onEdit: (Produccion) -> Unit
) : ListAdapter<Produccion, ProduccionAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Produccion>() {
            override fun areItemsTheSame(a: Produccion, b: Produccion) = a.id == b.id
            override fun areContentsTheSame(a: Produccion, b: Produccion) = a == b
        }
    }

    inner class VH(private val b: ItemProduccionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Produccion) {
            b.tvLitros.text = "${item.litros} L"
            b.tvFecha.text = item.fecha
            b.btnEliminar.setOnClickListener { onDelete(item) }
            b.root.setOnClickListener { onEdit(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemProduccionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
