package com.ganadopro.ui.dashboard

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.ganadopro.R
import com.ganadopro.databinding.FragmentDashboardBinding
import com.ganadopro.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarSaludo()
        observeData()
        setupClicks()
    }

    private fun configurarSaludo() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val saludo = when {
            hora < 12 -> "Buenos días 👋"
            hora < 18 -> "Buenas tardes ☀️"
            else -> "Buenas noches 🌙"
        }

        val formato = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "PA"))
        val fechaHoy = formato.format(Date()).replaceFirstChar { it.uppercase() }

        binding.tvSaludo.text = saludo
        binding.tvFecha.text = fechaHoy
    }

    private fun animarContador(textView: android.widget.TextView, valorFinal: Int) {
        ValueAnimator.ofInt(0, valorFinal).apply {
            duration = 800
            addUpdateListener {
                textView.text = (it.animatedValue as Int).toString()
            }
            start()
        }
    }

    private fun observeData() {
        viewModel.totalAnimales.observe(viewLifecycleOwner) { total ->
            animarContador(binding.tvTotalAnimales, total ?: 0)
        }

        viewModel.vacunasPendientes.observe(viewLifecycleOwner) { pendientes ->
            animarContador(binding.tvVacunasPendientes, pendientes ?: 0)
            binding.tvVacunasPendientes.setTextColor(
                if ((pendientes ?: 0) > 0)
                    android.graphics.Color.parseColor("#D32F2F")
                else
                    android.graphics.Color.parseColor("#1F5E3B")
            )
        }

        viewModel.produccionMes.observe(viewLifecycleOwner) { litros ->
            binding.tvProduccionMes.text = String.format("%.1f L", litros ?: 0.0)
        }

        viewModel.proximosPartos.observe(viewLifecycleOwner) { alertas ->
            animarContador(binding.tvProximosPartos, alertas.size)
            renderActividadReciente(alertas)
        }
    }

    private fun renderActividadReciente(alertas: List<com.ganadopro.viewmodel.AlertaParto>) {
        binding.containerActividad.removeAllViews()

        if (alertas.isEmpty()) {
            val tv = android.widget.TextView(requireContext()).apply {
                text = "No hay partos próximos registrados"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            }
            binding.containerActividad.addView(tv)
            return
        }

        alertas.forEach { alerta ->
            val tv = android.widget.TextView(requireContext()).apply {
                text = when {
                    alerta.diasRestantes < 0 -> "🐄 ${alerta.nombreAnimal}: parto atrasado (${-alerta.diasRestantes} días)"
                    alerta.diasRestantes == 0 -> "🐄 ${alerta.nombreAnimal}: ¡puede parir HOY!"
                    else -> "🐄 ${alerta.nombreAnimal}: a ${alerta.diasRestantes} días de parir"
                }
                textSize = 14f
                setTextColor(
                    if (alerta.diasRestantes <= 3) android.graphics.Color.parseColor("#D32F2F")
                    else android.graphics.Color.parseColor("#374151")
                )
                setPadding(0, 12, 0, 12)
                // Tocar una alerta abre la ficha de ese animal.
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    findNavController().navigate(
                        R.id.detalleAnimalFragment,
                        bundleOf("animalId" to alerta.animalId)
                    )
                }
            }
            binding.containerActividad.addView(tv)
        }
    }

    private fun setupClicks() {
        binding.cardAnimales.setOnClickListener { irAPestana(R.id.animalesFragment) }
        binding.cardReportes.setOnClickListener { irAPestana(R.id.reportesFragment) }
        // Vacunas no es una pestaña: abre la lista de vacunas pendientes de
        // toda la finca (VacunasFragment con animalId = 0, su valor por defecto).
        binding.cardVacunas.setOnClickListener {
            findNavController().navigate(R.id.vacunasFragment)
        }
        binding.fabAgregarAnimal.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_formAnimal)
        }
    }

    /**
     * Cambia a otra pestaña de la barra inferior exactamente igual que si el
     * usuario la hubiera tocado (mismas opciones que usa BottomNavigationView
     * con setupWithNavController). Con un navigate() simple la pantalla se
     * apilaría encima del Inicio y la barra inferior quedaría desincronizada.
     */
    private fun irAPestana(destino: Int) {
        val navController = findNavController()
        navController.navigate(destino, null, navOptions {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}