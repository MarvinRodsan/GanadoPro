package com.ganadopro.ui.reproduccion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.Reproduccion
import com.ganadopro.databinding.ItemReproduccionBinding

class ReproduccionAdapter(
    private val onDelete: (Reproduccion) -> Unit,
    private val onEdit: (Reproduccion) -> Unit
) : ListAdapter<Reproduccion, ReproduccionAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Reproduccion>() {
            override fun areItemsTheSame(a: Reproduccion, b: Reproduccion) = a.id == b.id
            override fun areContentsTheSame(a: Reproduccion, b: Reproduccion) = a == b
        }
    }

    inner class VH(private val b: ItemReproduccionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Reproduccion) {
            b.tvTipoEvento.text = item.tipoEvento
            b.tvFecha.text = item.fecha
            b.tvObservaciones.text = item.observaciones ?: ""
            b.tvResultado.text = if (!item.resultado.isNullOrBlank()) "Resultado: ${item.resultado}" else ""
            b.btnEliminar.setOnClickListener { onDelete(item) }
            b.root.setOnClickListener { onEdit(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemReproduccionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
