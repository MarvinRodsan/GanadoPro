package com.ganadopro.ui.vacunas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.Animal
import com.ganadopro.data.entities.Vacuna
import com.ganadopro.databinding.ItemVacunaBinding

/**
 * animalesPorId: cuando la lista es la de "todas las vacunas pendientes"
 * (sin filtrar por animal), se necesita mostrar a qué animal pertenece cada
 * vacuna (Animal + Código), como pide la especificación. En modo "vacunas de
 * un animal" se pasa un mapa vacío y esa línea queda oculta.
 */
class VacunaAdapter(
    private val onDelete: (Vacuna) -> Unit,
    private val onEdit: (Vacuna) -> Unit,
    private var animalesPorId: Map<Long, Animal> = emptyMap()
) : ListAdapter<Vacuna, VacunaAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Vacuna>() {
            override fun areItemsTheSame(a: Vacuna, b: Vacuna) = a.id == b.id
            override fun areContentsTheSame(a: Vacuna, b: Vacuna) = a == b
        }
    }

    fun setAnimales(animales: Map<Long, Animal>) {
        animalesPorId = animales
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemVacunaBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(v: Vacuna) {
            val animal = animalesPorId[v.animalId]
            if (animal != null) {
                b.tvAnimalVacuna.visibility = android.view.View.VISIBLE
                b.tvAnimalVacuna.text = "Animal: ${animal.nombre} (${animal.codigo})"
            } else {
                b.tvAnimalVacuna.visibility = android.view.View.GONE
            }
            b.tvNombreVacuna.text = v.nombre
            b.tvFechaAplicacion.text = "Aplicada: ${v.fechaAplicacion}"
            b.tvProximaDosis.text = if (!v.proximaDosis.isNullOrBlank()) "Próxima dosis: ${v.proximaDosis}" else "Sin próxima dosis"
            b.tvVeterinario.text = if (!v.veterinario.isNullOrBlank()) "Vet: ${v.veterinario}" else ""
            b.btnEliminar.setOnClickListener { onDelete(v) }
            b.root.setOnClickListener { onEdit(v) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemVacunaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
}
