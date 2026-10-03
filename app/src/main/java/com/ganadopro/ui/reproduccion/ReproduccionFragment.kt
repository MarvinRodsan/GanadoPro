package com.ganadopro.ui.reproduccion

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
import com.ganadopro.data.entities.Reproduccion
import com.ganadopro.databinding.FragmentReproduccionBinding
import com.ganadopro.util.confirmarEliminacion
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.viewmodel.ReproduccionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class ReproduccionFragment : Fragment() {

    private var _binding: FragmentReproduccionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReproduccionViewModel by viewModels()
    private val args: ReproduccionFragmentArgs by navArgs()
    private lateinit var adapter: ReproduccionAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReproduccionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setAnimalId(args.animalId)

        adapter = ReproduccionAdapter(
            onDelete = { item ->
                confirmarEliminacion("¿Eliminar el evento \"${item.tipoEvento}\" del ${item.fecha}?") {
                    viewModel.delete(item)
                }
            },
            onEdit = { mostrarDialogo(it) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.eventos.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAgregar.setOnClickListener { mostrarDialogo(null) }
    }

    private fun mostrarDialogo(eventoEditando: Reproduccion?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reproduccion, null)
        val spEvento = dialogView.findViewById<Spinner>(R.id.sp_tipo_evento)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha)
        val etObservaciones = dialogView.findViewById<TextInputEditText>(R.id.et_observaciones)
        val etResultado = dialogView.findViewById<TextInputEditText>(R.id.et_resultado)

        // Estos nombres se guardan tal cual en la base de datos y los usa el
        // cálculo de partos próximos (ReproduccionUtils / ReproduccionDao):
        // si se renombra uno aquí, hay que renombrarlo también allá.
        val eventos = listOf("Celo", "Inseminación", "Monta", "Diagnóstico Preñez", "Parto")
        spEvento.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, eventos)

        if (eventoEditando != null) {
            val idx = eventos.indexOf(eventoEditando.tipoEvento).coerceAtLeast(0)
            spEvento.setSelection(idx)
            etFecha.setText(eventoEditando.fecha)
            etObservaciones.setText(eventoEditando.observaciones)
            etResultado.setText(eventoEditando.resultado)
        } else {
            etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (eventoEditando == null) "Registrar Evento Reproductivo" else "Editar Evento Reproductivo")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val fecha = etFecha.text.toString().trim()
                    etFecha.error = null

                    // La fecha debe existir y ser real en formato yyyy-MM-dd:
                    // de ella depende el cálculo de la fecha probable de parto.
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.isLenient = false
                    var valido = true
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    } else if (runCatching { sdf.parse(fecha) }.getOrNull() == null) {
                        etFecha.error = "Fecha inválida"
                        valido = false
                    }
                    if (!valido) return@setOnClickListener

                    val observaciones = etObservaciones.text.toString().trim().ifBlank { null }
                    val resultado = etResultado.text.toString().trim().ifBlank { null }
                    val tipoEvento = spEvento.selectedItem.toString()

                    if (eventoEditando == null) {
                        viewModel.insert(
                            Reproduccion(animalId = args.animalId, tipoEvento = tipoEvento, fecha = fecha, observaciones = observaciones, resultado = resultado)
                        )
                    } else {
                        viewModel.update(
                            eventoEditando.copy(tipoEvento = tipoEvento, fecha = fecha, observaciones = observaciones, resultado = resultado)
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
