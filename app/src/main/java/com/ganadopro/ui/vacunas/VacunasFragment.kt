package com.ganadopro.ui.vacunas

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.data.entities.Animal
import com.ganadopro.data.entities.Vacuna
import com.ganadopro.databinding.FragmentVacunasBinding
import com.ganadopro.util.confirmarEliminacion
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.viewmodel.AnimalViewModel
import com.ganadopro.viewmodel.VacunaViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Dos modos según el argumento animalId:
 *  - animalId > 0: vacunas de ese animal (se entra desde su ficha).
 *  - animalId = 0: lista global de vacunas PENDIENTES de toda la finca (se
 *    entra desde la tarjeta "Vacunas" del Dashboard).
 */
class VacunasFragment : Fragment() {

    private var _binding: FragmentVacunasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VacunaViewModel by viewModels()
    private val animalViewModel: AnimalViewModel by viewModels()
    private val args: VacunasFragmentArgs by navArgs()
    private lateinit var adapter: VacunaAdapter

    private val modoGlobal get() = args.animalId <= 0L
    private var animalesActivos: List<Animal> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVacunasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = VacunaAdapter(
            onDelete = { vacuna ->
                confirmarEliminacion("¿Eliminar la vacuna \"${vacuna.nombre}\" aplicada el ${vacuna.fechaAplicacion}?") {
                    viewModel.delete(vacuna)
                }
            },
            onEdit = { vacuna ->
                if (modoGlobal) mostrarOpcionesPendiente(vacuna) else mostrarDialogo(vacunaEditando = vacuna)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        if (!modoGlobal) {
            // Vacunas de un animal específico (ya se sabe cuál es, no hace
            // falta mostrar la línea "Animal" en cada fila).
            viewModel.setAnimalId(args.animalId)
            viewModel.vacunas.observe(viewLifecycleOwner) {
                adapter.submitList(it)
                binding.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }
        } else {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = "Vacunas pendientes"
            binding.tvEmpty.text = "No hay vacunas pendientes.\nPresiona + para registrar una vacuna."

            // Se necesita el animal de cada vacuna para mostrar "Animal: nombre (código)".
            animalViewModel.animales.observe(viewLifecycleOwner) { animales ->
                adapter.setAnimales(animales.associateBy { it.id })
            }
            // Solo se pueden registrar vacunas nuevas a animales activos.
            animalViewModel.animalesActivos.observe(viewLifecycleOwner) { activos ->
                animalesActivos = activos.sortedBy { it.nombre.lowercase() }
            }
            viewModel.vacunasPendientes.observe(viewLifecycleOwner) {
                adapter.submitList(it)
                binding.tvEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabAgregar.setOnClickListener {
            if (modoGlobal && animalesActivos.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Sin animales")
                    .setMessage("Primero registra al menos un animal activo para poder vacunarlo.")
                    .setPositiveButton("Entendido", null)
                    .show()
            } else {
                mostrarDialogo()
            }
        }
    }

    /**
     * En la lista de pendientes lo más común no es corregir el registro
     * viejo sino anotar que ya se puso la dosis. Al registrar la dosis nueva
     * (misma vacuna, mismo animal), la pendiente desaparece sola de la lista
     * (ver VacunaDao.getPendientes).
     */
    private fun mostrarOpcionesPendiente(vacuna: Vacuna) {
        val opciones = arrayOf("Registrar dosis aplicada", "Editar este registro")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(vacuna.nombre)
            .setItems(opciones) { _, cual ->
                if (cual == 0) mostrarDialogo(refuerzoDe = vacuna)
                else mostrarDialogo(vacunaEditando = vacuna)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * vacunaEditando: registro existente a modificar.
     * refuerzoDe: vacuna pendiente de la que se va a registrar la dosis ya
     *   aplicada; el registro nuevo hereda su animal, nombre y veterinario.
     * Sin ninguno de los dos: vacuna nueva (en modo global se elige el animal).
     */
    private fun mostrarDialogo(vacunaEditando: Vacuna? = null, refuerzoDe: Vacuna? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_vacuna, null)
        val tvLabelAnimal = dialogView.findViewById<TextView>(R.id.tv_label_animal_vacuna)
        val spAnimal = dialogView.findViewById<Spinner>(R.id.sp_animal_vacuna)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.et_nombre_vacuna)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha_aplicacion)
        val tilProxima = dialogView.findViewById<TextInputLayout>(R.id.til_proxima_dosis)
        val etProxima = dialogView.findViewById<TextInputEditText>(R.id.et_proxima_dosis)
        val etVeterinario = dialogView.findViewById<TextInputEditText>(R.id.et_veterinario)

        // Solo hay que elegir animal al crear una vacuna desde la lista global.
        val elegirAnimal = modoGlobal && vacunaEditando == null && refuerzoDe == null
        // Copia fija de la lista: si la base de datos cambia con el diálogo
        // abierto, la posición elegida en el selector sigue apuntando al
        // mismo animal.
        val animalesDelSelector = animalesActivos
        if (elegirAnimal) {
            tvLabelAnimal.visibility = View.VISIBLE
            spAnimal.visibility = View.VISIBLE
            spAnimal.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                animalesDelSelector.map { "${it.nombre} (${it.codigo})" }
            )
        }

        when {
            vacunaEditando != null -> {
                etNombre.setText(vacunaEditando.nombre)
                etFecha.setText(vacunaEditando.fechaAplicacion)
                etProxima.setText(vacunaEditando.proximaDosis)
                etVeterinario.setText(vacunaEditando.veterinario)
            }
            refuerzoDe != null -> {
                etNombre.setText(refuerzoDe.nombre)
                etFecha.setText(fechaHoy())
                etVeterinario.setText(refuerzoDe.veterinario)
            }
            else -> etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }
        // Reprogramar la próxima dosis a una fecha distinta es justo el caso
        // que debe generar una alerta nueva (ver AlertaEnviada).
        etProxima.setOnClickListener { mostrarSelectorFecha(etProxima) }
        tilProxima.setEndIconOnClickListener {
            etProxima.setText("")
            etProxima.error = null
        }

        val titulo = when {
            vacunaEditando != null -> "Editar Vacuna"
            refuerzoDe != null -> "Registrar dosis aplicada"
            else -> "Registrar Vacuna"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val nombre = etNombre.text.toString().trim()
                    val fecha = etFecha.text.toString().trim()
                    val proxima = etProxima.text.toString().trim().ifBlank { null }
                    etNombre.error = null
                    etFecha.error = null
                    etProxima.error = null

                    var valido = true
                    if (nombre.isBlank()) {
                        etNombre.error = "Requerido"
                        valido = false
                    }
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }
                    // Ambas fechas son "yyyy-MM-dd", así que comparar el
                    // texto equivale a comparar las fechas.
                    if (proxima != null && fecha.isNotBlank() && proxima <= fecha) {
                        etProxima.error = "Debe ser posterior a la fecha de aplicación"
                        valido = false
                    }
                    if (!valido) return@setOnClickListener

                    val veterinario = etVeterinario.text.toString().trim().ifBlank { null }

                    if (vacunaEditando != null) {
                        viewModel.update(
                            vacunaEditando.copy(nombre = nombre, fechaAplicacion = fecha, proximaDosis = proxima, veterinario = veterinario)
                        )
                    } else {
                        val animalId = when {
                            refuerzoDe != null -> refuerzoDe.animalId
                            elegirAnimal -> animalesDelSelector.getOrNull(spAnimal.selectedItemPosition)?.id
                            else -> args.animalId
                        } ?: return@setOnClickListener

                        viewModel.insert(
                            Vacuna(animalId = animalId, nombre = nombre, fechaAplicacion = fecha, proximaDosis = proxima, veterinario = veterinario)
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
