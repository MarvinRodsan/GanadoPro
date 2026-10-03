package com.ganadopro.ui.animales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ganadopro.R
import com.ganadopro.data.entities.Animal
import com.ganadopro.databinding.ItemAnimalBinding
import com.ganadopro.util.PhotoUtils

class AnimalAdapter(
    private val onEdit: (Animal) -> Unit,
    private val onDelete: (Animal) -> Unit
) : ListAdapter<Animal, AnimalAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Animal>() {
            override fun areItemsTheSame(a: Animal, b: Animal) = a.id == b.id
            override fun areContentsTheSame(a: Animal, b: Animal) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(animal: Animal) {
            binding.tvNombre.text = animal.nombre
            binding.tvCodigo.text = "Cód: ${animal.codigo}"
            binding.tvRaza.text = animal.raza
            binding.tvEstado.text = animal.estado
            binding.tvSexo.text = animal.sexo

            PhotoUtils.load(binding.ivFoto, animal.foto, R.drawable.vaca)

            binding.root.setOnClickListener { onEdit(animal) }
            binding.btnEliminar.setOnClickListener { onDelete(animal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}