package com.ganadopro.ui.peso

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.data.entities.ControlPeso
import com.ganadopro.databinding.FragmentPesoBinding
import com.ganadopro.util.aDecimalOrNull
import com.ganadopro.util.confirmarEliminacion
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.util.sinCerosDeMas
import com.ganadopro.viewmodel.PesoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class PesoFragment : Fragment() {

    private var _binding: FragmentPesoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PesoViewModel by viewModels()
    private val args: PesoFragmentArgs by navArgs()
    private lateinit var adapter: PesoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPesoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAnimalId(args.animalId)

        adapter = PesoAdapter(
            onDelete = { item ->
                confirmarEliminacion("¿Eliminar el pesaje de ${item.peso.sinCerosDeMas()} kg del ${item.fecha}?") {
                    viewModel.delete(item)
                }
            },
            onEdit = { mostrarDialogo(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // La consulta devuelve los pesajes del más antiguo al más nuevo
        // (ver ControlPesoDao). En pantalla se muestran al revés, con el más
        // reciente arriba, igual que el resto de los listados.
        viewModel.pesos.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.asReversed().toList())
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

            val ultimo = list.lastOrNull()
            if (ultimo == null) {
                binding.tvUltimoPeso.visibility = View.GONE
            } else {
                var texto = "Último peso: ${ultimo.peso.sinCerosDeMas()} kg (${ultimo.fecha})"
                // Con al menos dos pesajes se muestra cuánto ganó o perdió
                // respecto al anterior.
                if (list.size >= 2) {
                    val diferencia = ultimo.peso - list[list.size - 2].peso
                    val signo = if (diferencia >= 0) "+" else "−"
                    texto += "  ·  $signo${String.format("%.1f", Math.abs(diferencia))} kg vs. anterior"
                }
                binding.tvUltimoPeso.text = texto
                binding.tvUltimoPeso.visibility = View.VISIBLE
            }
        }

        binding.fabAgregar.setOnClickListener { mostrarDialogo(null) }
    }

    private fun mostrarDialogo(pesoEditando: ControlPeso?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_peso, null)
        val etPeso = dialogView.findViewById<TextInputEditText>(R.id.et_peso)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha)
        val etObservaciones = dialogView.findViewById<TextInputEditText>(R.id.et_observaciones)

        if (pesoEditando != null) {
            etPeso.setText(pesoEditando.peso.sinCerosDeMas())
            etFecha.setText(pesoEditando.fecha)
            etObservaciones.setText(pesoEditando.observaciones)
        } else {
            etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (pesoEditando == null) "Registrar Peso" else "Editar Peso")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val pesoStr = etPeso.text.toString().trim()
                    val fecha = etFecha.text.toString().trim()
                    etPeso.error = null
                    etFecha.error = null

                    // peso > 0 obligatorio; nunca se guarda 0.0 por defecto
                    // cuando el valor escrito es inválido.
                    val peso = pesoStr.aDecimalOrNull()
                    var valido = true
                    when {
                        pesoStr.isBlank() -> {
                            etPeso.error = "Requerido"
                            valido = false
                        }
                        peso == null -> {
                            etPeso.error = "Debe ser un número válido"
                            valido = false
                        }
                        peso <= 0.0 -> {
                            etPeso.error = "Debe ser mayor a 0"
                            valido = false
                        }
                    }
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }
                    if (!valido || peso == null) return@setOnClickListener

                    val observaciones = etObservaciones.text.toString().trim().ifBlank { null }

                    if (pesoEditando == null) {
                        viewModel.insert(ControlPeso(animalId = args.animalId, peso = peso, fecha = fecha, observaciones = observaciones))
                    } else {
                        viewModel.update(pesoEditando.copy(peso = peso, fecha = fecha, observaciones = observaciones))
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
