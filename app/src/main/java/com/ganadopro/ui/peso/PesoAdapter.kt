package com.ganadopro.ui.peso

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.ControlPeso
import com.ganadopro.databinding.ItemPesoBinding

class PesoAdapter(
    private val onDelete: (ControlPeso) -> Unit,
    private val onEdit: (ControlPeso) -> Unit
) : ListAdapter<ControlPeso, PesoAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ControlPeso>() {
            override fun areItemsTheSame(a: ControlPeso, b: ControlPeso) = a.id == b.id
            override fun areContentsTheSame(a: ControlPeso, b: ControlPeso) = a == b
        }
    }

    inner class VH(private val b: ItemPesoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ControlPeso) {
            b.tvPeso.text = "${item.peso} kg"
            b.tvFecha.text = item.fecha
            b.tvObservaciones.text = item.observaciones ?: ""
            b.btnEliminar.setOnClickListener { onDelete(item) }
            b.root.setOnClickListener { onEdit(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPesoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
