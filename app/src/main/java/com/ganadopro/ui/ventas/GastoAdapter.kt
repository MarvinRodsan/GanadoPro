package com.ganadopro.ui.ventas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.Gasto
import com.ganadopro.databinding.ItemGastoBinding

class GastoAdapter(
    private val onDelete: (Gasto) -> Unit,
    private val onEdit: (Gasto) -> Unit
) : ListAdapter<Gasto, GastoAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Gasto>() {
            override fun areItemsTheSame(a: Gasto, b: Gasto) = a.id == b.id
            override fun areContentsTheSame(a: Gasto, b: Gasto) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemGastoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(gasto: Gasto) {
            binding.tvConcepto.text = gasto.concepto
            binding.tvFechaGasto.text = gasto.fecha
            binding.tvMonto.text = "-$${String.format("%.2f", gasto.monto)}"
            binding.btnEliminarGasto.setOnClickListener { onDelete(gasto) }
            binding.root.setOnClickListener { onEdit(gasto) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGastoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}