package com.ganadopro.ui.historial

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.HistorialMedico
import com.ganadopro.databinding.ItemHistorialBinding

class HistorialAdapter(
    private val onDelete: (HistorialMedico) -> Unit,
    private val onEdit: (HistorialMedico) -> Unit
) : ListAdapter<HistorialMedico, HistorialAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HistorialMedico>() {
            override fun areItemsTheSame(a: HistorialMedico, b: HistorialMedico) = a.id == b.id
            override fun areContentsTheSame(a: HistorialMedico, b: HistorialMedico) = a == b
        }
    }

    inner class VH(private val b: ItemHistorialBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(h: HistorialMedico) {
            b.tvFecha.text = h.fecha
            b.tvDiagnostico.text = "Diagnóstico: ${h.diagnostico}"
            b.tvTratamiento.text = "Tratamiento: ${h.tratamiento}"
            b.tvObservaciones.text = if (!h.observaciones.isNullOrBlank()) "Obs: ${h.observaciones}" else ""
            b.btnEliminar.setOnClickListener { onDelete(h) }
            b.root.setOnClickListener { onEdit(h) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistorialBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
