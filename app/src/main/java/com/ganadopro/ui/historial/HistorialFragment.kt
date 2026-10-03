package com.ganadopro.ui.historial

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.data.entities.HistorialMedico
import com.ganadopro.databinding.FragmentHistorialBinding
import com.ganadopro.util.confirmarEliminacion
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.viewmodel.HistorialViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistorialViewModel by viewModels()
    private val args: HistorialFragmentArgs by navArgs()
    private lateinit var adapter: HistorialAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAnimalId(args.animalId)

        adapter = HistorialAdapter(
            onDelete = { item ->
                confirmarEliminacion("¿Eliminar el registro médico del ${item.fecha} (${item.diagnostico})?") {
                    viewModel.delete(item)
                }
            },
            onEdit = { mostrarDialogoAgregar(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.historial.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAgregar.setOnClickListener { mostrarDialogoAgregar(null) }
    }

    private fun mostrarDialogoAgregar(historialEditando: HistorialMedico?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_historial, null)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha)
        val etDiagnostico = dialogView.findViewById<TextInputEditText>(R.id.et_diagnostico)
        val etTratamiento = dialogView.findViewById<TextInputEditText>(R.id.et_tratamiento)
        val etObservaciones = dialogView.findViewById<TextInputEditText>(R.id.et_observaciones)

        if (historialEditando != null) {
            etFecha.setText(historialEditando.fecha)
            etDiagnostico.setText(historialEditando.diagnostico)
            etTratamiento.setText(historialEditando.tratamiento)
            etObservaciones.setText(historialEditando.observaciones)
        } else {
            etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (historialEditando == null) "Nuevo Registro Médico" else "Editar Registro Médico")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val fecha = etFecha.text.toString().trim()
                    val diagnostico = etDiagnostico.text.toString().trim()
                    val tratamiento = etTratamiento.text.toString().trim()
                    etFecha.error = null
                    etDiagnostico.error = null
                    etTratamiento.error = null

                    // Diagnóstico, tratamiento y fecha son obligatorios.
                    var valido = true
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }
                    if (diagnostico.isBlank()) {
                        etDiagnostico.error = "Requerido"
                        valido = false
                    }
                    if (tratamiento.isBlank()) {
                        etTratamiento.error = "Requerido"
                        valido = false
                    }
                    if (!valido) return@setOnClickListener

                    val observaciones = etObservaciones.text.toString().trim().ifBlank { null }

                    if (historialEditando == null) {
                        viewModel.insert(
                            HistorialMedico(animalId = args.animalId, fecha = fecha, diagnostico = diagnostico, tratamiento = tratamiento, observaciones = observaciones)
                        )
                    } else {
                        viewModel.update(
                            historialEditando.copy(fecha = fecha, diagnostico = diagnostico, tratamiento = tratamiento, observaciones = observaciones)
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
