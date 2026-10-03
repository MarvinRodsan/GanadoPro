package com.ganadopro.ui.alimentacion

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.data.entities.Alimentacion
import com.ganadopro.databinding.FragmentAlimentacionBinding
import com.ganadopro.util.aDecimalOrNull
import com.ganadopro.util.confirmarEliminacion
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.util.sinCerosDeMas
import com.ganadopro.viewmodel.AlimentacionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class AlimentacionFragment : Fragment() {

    private var _binding: FragmentAlimentacionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlimentacionViewModel by viewModels()
    private val args: AlimentacionFragmentArgs by navArgs()
    private lateinit var adapter: AlimentacionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlimentacionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAnimalId(args.animalId)

        adapter = AlimentacionAdapter(
            onDelete = { item ->
                confirmarEliminacion("¿Eliminar el registro de ${item.tipoAlimento} del ${item.fecha}?") {
                    viewModel.delete(item)
                }
            },
            onEdit = { mostrarDialogo(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.alimentacion.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAgregar.setOnClickListener { mostrarDialogo(null) }
    }

    private fun mostrarDialogo(alimentacionEditando: Alimentacion?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_alimentacion, null)
        val spTipo = dialogView.findViewById<Spinner>(R.id.sp_tipo_alimento)
        val etCantidad = dialogView.findViewById<TextInputEditText>(R.id.et_cantidad)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha)
        val etObservaciones = dialogView.findViewById<TextInputEditText>(R.id.et_observaciones)

        val tipos = listOf("Pasto", "Concentrado", "Ensilaje", "Suplementos")
        spTipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)

        if (alimentacionEditando != null) {
            val idx = tipos.indexOf(alimentacionEditando.tipoAlimento).coerceAtLeast(0)
            spTipo.setSelection(idx)
            etCantidad.setText(alimentacionEditando.cantidad.sinCerosDeMas())
            etFecha.setText(alimentacionEditando.fecha)
            etObservaciones.setText(alimentacionEditando.observaciones)
        } else {
            etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (alimentacionEditando == null) "Registrar Alimentación" else "Editar Alimentación")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val cantidadStr = etCantidad.text.toString().trim()
                    val fecha = etFecha.text.toString().trim()
                    etCantidad.error = null
                    etFecha.error = null

                    // cantidad > 0 obligatoria; nunca se guarda 0.0 por
                    // defecto cuando el valor escrito es inválido.
                    val cantidad = cantidadStr.aDecimalOrNull()
                    var valido = true
                    when {
                        cantidadStr.isBlank() -> {
                            etCantidad.error = "Requerido"
                            valido = false
                        }
                        cantidad == null -> {
                            etCantidad.error = "Debe ser un número válido"
                            valido = false
                        }
                        cantidad <= 0.0 -> {
                            etCantidad.error = "Debe ser mayor a 0"
                            valido = false
                        }
                    }
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }
                    if (!valido || cantidad == null) return@setOnClickListener

                    val tipoAlimento = spTipo.selectedItem.toString()
                    val observaciones = etObservaciones.text.toString().trim().ifBlank { null }

                    if (alimentacionEditando == null) {
                        viewModel.insert(
                            Alimentacion(animalId = args.animalId, tipoAlimento = tipoAlimento, cantidad = cantidad, fecha = fecha, observaciones = observaciones)
                        )
                    } else {
                        viewModel.update(
                            alimentacionEditando.copy(tipoAlimento = tipoAlimento, cantidad = cantidad, fecha = fecha, observaciones = observaciones)
                        )
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
