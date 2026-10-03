package com.ganadopro.ui.alimentacion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.Alimentacion
import com.ganadopro.databinding.ItemAlimentacionBinding

class AlimentacionAdapter(
    private val onDelete: (Alimentacion) -> Unit,
    private val onEdit: (Alimentacion) -> Unit
) : ListAdapter<Alimentacion, AlimentacionAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Alimentacion>() {
            override fun areItemsTheSame(a: Alimentacion, b: Alimentacion) = a.id == b.id
            override fun areContentsTheSame(a: Alimentacion, b: Alimentacion) = a == b
        }
    }

    inner class VH(private val b: ItemAlimentacionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Alimentacion) {
            b.tvTipoAlimento.text = item.tipoAlimento
            b.tvCantidad.text = "${item.cantidad} ${item.unidad}"
            b.tvFecha.text = item.fecha
            b.tvObservaciones.text = item.observaciones ?: ""
            b.btnEliminar.setOnClickListener { onDelete(item) }
            b.root.setOnClickListener { onEdit(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemAlimentacionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
