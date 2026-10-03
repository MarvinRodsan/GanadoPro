package com.ganadopro.ui.ventas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.data.entities.Animal
import com.ganadopro.data.entities.Venta
import com.ganadopro.databinding.ItemVentaBinding

class VentaAdapter(
    private val onDelete: (Venta) -> Unit,
    private val onEdit: (Venta) -> Unit
) : ListAdapter<Venta, VentaAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Venta>() {
            override fun areItemsTheSame(a: Venta, b: Venta) = a.id == b.id
            override fun areContentsTheSame(a: Venta, b: Venta) = a == b
        }
    }

    // Para mostrar en cada fila qué animal se vendió (nombre y código).
    private var animalesPorId: Map<Long, Animal> = emptyMap()

    fun setAnimales(animales: Map<Long, Animal>) {
        animalesPorId = animales
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemVentaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(venta: Venta) {
            val animal = animalesPorId[venta.animalId]
            binding.tvAnimalVenta.text =
                if (animal != null) "${animal.nombre} (${animal.codigo})" else "Animal #${venta.animalId}"
            binding.tvComprador.text = "Comprador: ${venta.comprador}"
            binding.tvFechaVenta.text = venta.fecha
            binding.tvPrecio.text = "$${String.format("%.2f", venta.precio)}"
            binding.btnEliminarVenta.setOnClickListener { onDelete(venta) }
            binding.root.setOnClickListener { onEdit(venta) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVentaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
