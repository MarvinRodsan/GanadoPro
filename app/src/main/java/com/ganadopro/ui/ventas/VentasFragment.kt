package com.ganadopro.ui.ventas

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.data.entities.Animal
import com.ganadopro.data.entities.Gasto
import com.ganadopro.data.entities.Venta
import com.ganadopro.databinding.FragmentVentasBinding
import com.ganadopro.util.aDecimalOrNull
import com.ganadopro.util.fechaHoy
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.util.sinCerosDeMas
import com.ganadopro.viewmodel.VentaGastoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText

class VentasFragment : Fragment() {

    private var _binding: FragmentVentasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VentaGastoViewModel by viewModels()

    private lateinit var ventaAdapter: VentaAdapter
    private lateinit var gastoAdapter: GastoAdapter
    private var animalesActivos: List<Animal> = emptyList()
    private var animalesPorId: Map<Long, Animal> = emptyMap()
    private var totalVentasMes = 0.0
    private var totalGastosMes = 0.0
    private var tabSeleccionada = 0 // 0 = ventas, 1 = gastos

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVentasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupTabs()
        observeData()

        binding.fabAgregar.setOnClickListener {
            if (tabSeleccionada == 0) mostrarDialogoVenta()
            else mostrarDialogoGasto()
        }
    }

    private fun setupRecyclerViews() {
        ventaAdapter = VentaAdapter(
            onDelete = { venta ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar venta")
                    .setMessage("¿Eliminar la venta a ${venta.comprador}? El animal volverá a quedar como Activo.")
                    .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteVenta(venta) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onEdit = { venta -> mostrarDialogoVenta(venta) }
        )
        binding.recyclerVentas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerVentas.adapter = ventaAdapter

        gastoAdapter = GastoAdapter(
            onDelete = { gasto ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar gasto")
                    .setMessage("¿Eliminar el gasto de ${gasto.concepto}?")
                    .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteGasto(gasto) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onEdit = { gasto -> mostrarDialogoGasto(gasto) }
        )
        binding.recyclerGastos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGastos.adapter = gastoAdapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabSeleccionada = tab.position
                if (tab.position == 0) {
                    binding.containerVentas.visibility = View.VISIBLE
                    binding.containerGastos.visibility = View.GONE
                } else {
                    binding.containerVentas.visibility = View.GONE
                    binding.containerGastos.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun observeData() {
        viewModel.ventas.observe(viewLifecycleOwner) { list ->
            ventaAdapter.submitList(list)
            binding.tvEmptyVentas.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.gastos.observe(viewLifecycleOwner) { list ->
            gastoAdapter.submitList(list)
            binding.tvEmptyGastos.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.animalesActivos.observe(viewLifecycleOwner) { animales ->
            animalesActivos = animales.sortedBy { it.nombre.lowercase() }
        }

        viewModel.animales.observe(viewLifecycleOwner) { animales ->
            animalesPorId = animales.associateBy { it.id }
            ventaAdapter.setAnimales(animalesPorId)
        }

        viewModel.totalGanancias.observe(viewLifecycleOwner) { ventas ->
            totalVentasMes = ventas ?: 0.0
            binding.tvTotalVentas.text = "$${String.format("%.2f", totalVentasMes)}"
            actualizarBalance()
        }

        viewModel.totalGastosMes.observe(viewLifecycleOwner) { gastos ->
            totalGastosMes = gastos ?: 0.0
            binding.tvTotalGastos.text = "$${String.format("%.2f", totalGastosMes)}"
            actualizarBalance()
        }
    }

    private fun actualizarBalance() {
        val balance = totalVentasMes - totalGastosMes
        // Un balance negativo se escribe "-$50.00", no "$-50.00".
        val signo = if (balance < 0) "-" else ""
        binding.tvBalance.text = "$signo$${String.format("%.2f", Math.abs(balance))}"
    }

    private fun mostrarDialogoVenta(ventaEditando: Venta? = null) {
        if (ventaEditando == null && animalesActivos.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sin animales disponibles")
                .setMessage("No hay animales activos para vender. Registra un animal primero.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_venta, null)
        val spAnimal = dialogView.findViewById<Spinner>(R.id.sp_animal)
        val etComprador = dialogView.findViewById<TextInputEditText>(R.id.et_comprador)
        val etPrecio = dialogView.findViewById<TextInputEditText>(R.id.et_precio)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha_venta)

        // Venta nueva: se elige entre los animales activos.
        // Venta existente: el animal ya está vendido (no aparece entre los
        // activos) y no se cambia; el selector solo lo muestra, bloqueado.
        // Así editar el precio de una venta nunca la reasigna a otro animal.
        val animalesDelSelector = animalesActivos
        val opciones: List<String> = if (ventaEditando == null) {
            animalesDelSelector.map { "${it.nombre} (${it.codigo})" }
        } else {
            val vendido = animalesPorId[ventaEditando.animalId]
            listOf(if (vendido != null) "${vendido.nombre} (${vendido.codigo})" else "Animal #${ventaEditando.animalId}")
        }
        spAnimal.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            opciones
        )
        spAnimal.isEnabled = ventaEditando == null

        if (ventaEditando != null) {
            etComprador.setText(ventaEditando.comprador)
            etPrecio.setText(ventaEditando.precio.sinCerosDeMas())
            etFecha.setText(ventaEditando.fecha)
        } else {
            etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (ventaEditando == null) "Registrar Venta" else "Editar Venta")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                // Se sobreescribe el listener del botón positivo para poder
                // validar y, si algo falla, dejar el diálogo abierto en vez
                // de cerrarlo con datos inválidos.
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val comprador = etComprador.text.toString().trim()
                    val precioStr = etPrecio.text.toString().trim()
                    val fecha = etFecha.text.toString().trim()

                    etComprador.error = null
                    etPrecio.error = null
                    etFecha.error = null

                    var valido = true
                    if (comprador.isBlank()) {
                        etComprador.error = "Requerido"
                        valido = false
                    }

                    // El precio es obligatorio, debe ser un número válido y > 0.
                    // Nunca se convierte silenciosamente "abc" o vacío en 0.0.
                    val precio = precioStr.aDecimalOrNull()
                    when {
                        precioStr.isBlank() -> {
                            etPrecio.error = "Requerido"
                            valido = false
                        }
                        precio == null -> {
                            etPrecio.error = "Debe ser un número válido"
                            valido = false
                        }
                        precio <= 0.0 -> {
                            etPrecio.error = "Debe ser mayor a 0"
                            valido = false
                        }
                    }

                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }

                    if (!valido || precio == null) return@setOnClickListener

                    if (ventaEditando == null) {
                        val animal = animalesDelSelector.getOrNull(spAnimal.selectedItemPosition)
                            ?: return@setOnClickListener
                        // Al guardar, el animal pasa a estado "Vendido"
                        // (ver GanaderaRepository.insertVenta).
                        viewModel.insertVenta(
                            Venta(animalId = animal.id, comprador = comprador, precio = precio, fecha = fecha)
                        )
                    } else {
                        viewModel.updateVenta(
                            ventaEditando.copy(comprador = comprador, precio = precio, fecha = fecha)
                        )
                    }
                    dismiss()
                }
            }
    }

    private fun mostrarDialogoGasto(gastoEditando: Gasto? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_gasto, null)
        val spConcepto = dialogView.findViewById<Spinner>(R.id.sp_concepto)
        val etMonto = dialogView.findViewById<TextInputEditText>(R.id.et_monto)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.et_fecha_gasto)
        val etNotas = dialogView.findViewById<TextInputEditText>(R.id.et_notas)

        // El concepto es la categoría del gasto: Reportes agrupa por este texto.
        val conceptos = listOf("Alimento", "Veterinario", "Medicinas", "Mantenimiento", "Transporte", "Otro")
        spConcepto.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            conceptos
        )

        if (gastoEditando != null) {
            val idx = conceptos.indexOf(gastoEditando.concepto).coerceAtLeast(0)
            spConcepto.setSelection(idx)
            etMonto.setText(gastoEditando.monto.sinCerosDeMas())
            etFecha.setText(gastoEditando.fecha)
            etNotas.setText(gastoEditando.notas)
        } else {
            etFecha.setText(fechaHoy())
        }

        etFecha.setOnClickListener { mostrarSelectorFecha(etFecha, permitirFuturo = false) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (gastoEditando == null) "Registrar Gasto" else "Editar Gasto")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val montoStr = etMonto.text.toString().trim()
                    val fecha = etFecha.text.toString().trim()
                    etMonto.error = null
                    etFecha.error = null

                    // El monto es obligatorio, numérico y > 0. Nunca se guarda
                    // 0 cuando el usuario en realidad escribió algo inválido.
                    val monto = montoStr.aDecimalOrNull()
                    var valido = true
                    when {
                        montoStr.isBlank() -> {
                            etMonto.error = "Requerido"
                            valido = false
                        }
                        monto == null -> {
                            etMonto.error = "Debe ser un número válido"
                            valido = false
                        }
                        monto <= 0.0 -> {
                            etMonto.error = "Debe ser mayor a 0"
                            valido = false
                        }
                    }
                    if (fecha.isBlank()) {
                        etFecha.error = "Requerido"
                        valido = false
                    }
                    if (!valido || monto == null) return@setOnClickListener

                    val concepto = spConcepto.selectedItem.toString()
                    val notas = etNotas.text.toString().trim().ifBlank { null }

                    if (gastoEditando == null) {
                        viewModel.insertGasto(Gasto(concepto = concepto, monto = monto, fecha = fecha, notas = notas))
                    } else {
                        viewModel.updateGasto(gastoEditando.copy(concepto = concepto, monto = monto, fecha = fecha, notas = notas))
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