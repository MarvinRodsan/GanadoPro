package com.ganadopro.ui.produccion

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.data.entities.Produccion
import com.ganadopro.databinding.FragmentProduccionBinding
import com.ganadopro.util.aDecimalOrNull
import com.ganadopro.util.confirmarEliminacion
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.util.sinCerosDeMas
import com.ganadopro.viewmodel.ProduccionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class ProduccionFragment : Fragment() {

    private var _binding: FragmentProduccionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProduccionViewModel by viewModels()
    private val args: ProduccionFragmentArgs by navArgs()
    private lateinit var adapter: ProduccionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProduccionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAnimalId(args.animalId)

        adapter = ProduccionAdapter(
            onDelete = { item ->
                confirmarEliminacion("¿Eliminar el registro de ${item.litros.sinCerosDeMas()} L del ${item.fecha}?") {
                    viewModel.delete(item)
                }
            },
            onEdit = { mostrarDialogo(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.tvTotalMes.visibility = View.VISIBLE

        viewModel.produccionAnimal.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

            // Esta pantalla es la producción de UN animal, así que el total
            // del mes también es el de este animal (el total de toda la finca
            // está en el Dashboard y en Reportes). Las fechas son
            // "yyyy-MM-dd": las del mes actual empiezan por "yyyy-MM".
            val mesActual = fechaHoy().substring(0, 7)
            val totalMes = list.filter { it.fecha.startsWith(mesActual) }.sumOf { it.litros }
            binding.tvTotalMes.text = "Este animal, este mes: ${String.format("%.1f", totalMes)} L"
        }

        binding.fabAgregar.setOnClickListener { mostrarDialogo(null) }
    }

    private fun mostrarDialogo(produccionEditando: Produccion?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_produccion, null)
        val etLitros = dialogView.findViewById<TextInputEditText>(R.id.et_litros)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha)

        if (produccionEditando != null) {
            etLitros.setText(produccionEditando.litros.sinCerosDeMas())
            etFecha.setText(produccionEditando.fecha)
        } else {
            etFecha.setText(fechaHoy())
        }
        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (produccionEditando == null) "Registrar Producción" else "Editar Producción")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val litrosStr = etLitros.text.toString().trim()
                    val fecha = etFecha.text.toString().trim()
                    etLitros.error = null
                    etFecha.error = null

                    // litros > 0 obligatorio; nunca se guarda 0.0 cuando el
                    // usuario escribió algo inválido, se muestra el error.
                    val litros = litrosStr.aDecimalOrNull()
                    var valido = true
                    when {
                        litrosStr.isBlank() -> {
                            etLitros.error = "Requerido"
                            valido = false
                        }
                        litros == null -> {
                            etLitros.error = "Debe ser un número válido"
                            valido = false
                        }
                        litros <= 0.0 -> {
                            etLitros.error = "Debe ser mayor a 0"
                            valido = false
                        }
                    }
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }
                    if (!valido || litros == null) return@setOnClickListener

                    if (produccionEditando == null) {
                        viewModel.insert(Produccion(animalId = args.animalId, litros = litros, fecha = fecha))
                    } else {
                        viewModel.update(produccionEditando.copy(litros = litros, fecha = fecha))
                    }
                    dismiss()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
