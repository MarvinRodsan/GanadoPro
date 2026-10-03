package com.ganadopro.ui.reportes

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.ganadopro.databinding.FragmentReportesBinding
import com.ganadopro.viewmodel.AnimalViewModel
import com.ganadopro.viewmodel.ProduccionViewModel
import com.ganadopro.viewmodel.VentaViewModel

class ReportesFragment : Fragment() {

    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!
    private val animalVM: AnimalViewModel by viewModels()
    private val produccionVM: ProduccionViewModel by viewModels()
    private val ventaVM: VentaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeReportes()
    }

    /**
     * Deja una gráfica vacía con un mensaje en español. Sin esto, cuando los
     * datos se quedan en cero (se borra el último animal, empieza un mes
     * nuevo) la gráfica seguiría mostrando los datos viejos, o el texto por
     * defecto de la librería en inglés ("No chart data available").
     */
    private fun vaciar(chart: Chart<*>, mensaje: String) {
        chart.setNoDataText(mensaje)
        chart.setNoDataTextColor(Color.parseColor("#9CA3AF"))
        chart.clear()
    }

    private fun observeReportes() {

        // Total animales + gráfica sexo (mismo LiveData)
        animalVM.animalesActivos.observe(viewLifecycleOwner) { animales ->

            // Total
            binding.tvTotalAnimales.text = "Total animales activos: ${animales.size}"

            val machos = animales.count { it.sexo == "Macho" }
            val hembras = animales.count { it.sexo == "Hembra" }

            binding.tvMachos.text = "Machos: $machos"
            binding.tvHembras.text = "Hembras: $hembras"

            // Gráfica sexo
            if (animales.isEmpty()) {
                vaciar(binding.pieChartSexo, "Sin animales activos")
                return@observe
            }

            // Solo se dibujan las porciones que tienen animales (una porción
            // de 0 se vería como una etiqueta suelta encima de la otra).
            val entries = mutableListOf<PieEntry>()
            val colores = mutableListOf<Int>()
            if (machos > 0) {
                entries.add(PieEntry(machos.toFloat(), "Machos"))
                colores.add(Color.parseColor("#1565C0"))
            }
            if (hembras > 0) {
                entries.add(PieEntry(hembras.toFloat(), "Hembras"))
                colores.add(Color.parseColor("#E91E63"))
            }

            val dataSet = PieDataSet(entries, "").apply {
                colors = colores
                valueTextColor = Color.WHITE
                valueTextSize = 13f
                sliceSpace = 3f
            }

            binding.pieChartSexo.apply {
                data = PieData(dataSet).apply { setValueFormatter(PercentFormatter(binding.pieChartSexo)) }
                description.isEnabled = false
                centerText = "Por Sexo"
                setUsePercentValues(true)
                legend.isEnabled = false
                animateY(800)
                invalidate()
            }
        }

        // Gráfica pie por raza
        animalVM.countByRaza.observe(viewLifecycleOwner) { razas ->
            if (razas.isNullOrEmpty()) {
                vaciar(binding.pieChartRazas, "Sin animales activos")
                return@observe
            }
            val entries = razas.map { PieEntry(it.total.toFloat(), it.raza) }
            val dataSet = PieDataSet(entries, "Razas").apply {
                // Dos paletas seguidas: hay más razas (7) que colores en una sola (4).
                colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.COLORFUL_COLORS.toList()
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }
            binding.pieChartRazas.apply {
                data = PieData(dataSet).apply { setValueFormatter(PercentFormatter(binding.pieChartRazas)) }
                description.isEnabled = false
                centerText = "Por Raza"
                setUsePercentValues(true)
                animateY(800)
                invalidate()
            }
        }

        // Producción mensual
        produccionVM.produccionDiaria.observe(viewLifecycleOwner) { dias ->
            if (dias.isNullOrEmpty()) {
                vaciar(binding.barChartProduccion, "Sin producción registrada este mes")
                return@observe
            }
            val entries = dias.mapIndexed { i, d ->
                BarEntry(i.toFloat(), d.litros.toFloat())
            }
            val dataSet = BarDataSet(entries, "Litros/día").apply {
                color = Color.parseColor("#4CAF50")
                valueTextColor = Color.BLACK
            }
            // Etiquetas del eje X: el día (dd) de cada fecha "yyyy-MM-dd", para
            // que se pueda identificar el período en vez de solo ver 0,1,2,3...
            val etiquetas = dias.map { d -> d.fecha.takeLast(2) }
            binding.barChartProduccion.apply {
                data = BarData(dataSet)
                description.isEnabled = false
                setFitBars(true)
                xAxis.apply {
                    valueFormatter = IndexAxisValueFormatter(etiquetas)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    labelRotationAngle = -45f
                }
                axisLeft.axisMinimum = 0f
                axisRight.isEnabled = false
                legend.isEnabled = false
                animateY(800)
                invalidate()
            }
        }

        // Ventas, Gastos y Utilidad real del mes (Utilidad = Ventas - Gastos,
        // nunca solo la suma de ventas)
        ventaVM.totalVentasMes.observe(viewLifecycleOwner) {
            binding.tvTotalVentas.text = "Ventas del mes: $${String.format("%.2f", it ?: 0.0)}"
        }
        ventaVM.totalGastosMes.observe(viewLifecycleOwner) {
            binding.tvTotalGastos.text = "Gastos del mes: $${String.format("%.2f", it ?: 0.0)}"
        }
        ventaVM.utilidadMes.observe(viewLifecycleOwner) {
            // Una pérdida se escribe "-$50.00", no "$-50.00".
            val signo = if (it < 0) "-" else ""
            binding.tvGanancias.text = "Utilidad del mes: $signo$${String.format("%.2f", Math.abs(it))}"
        }
        ventaVM.countVentasMes.observe(viewLifecycleOwner) {
            binding.tvAnimalesVendidos.text = "Animales vendidos este mes: ${it ?: 0}"
        }

        // Gastos por categoría
        ventaVM.gastosPorCategoriaMes.observe(viewLifecycleOwner) { categorias ->
            if (categorias.isNullOrEmpty()) {
                binding.pieChartGastos.isVisible = false
                binding.tvSinGastos.isVisible = true
                return@observe
            }
            binding.pieChartGastos.isVisible = true
            binding.tvSinGastos.isVisible = false

            val entries = categorias.map { PieEntry(it.total.toFloat(), it.concepto) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.VORDIPLOM_COLORS.toList()
                valueTextColor = Color.WHITE
                valueTextSize = 12f
                sliceSpace = 2f
            }
            binding.pieChartGastos.apply {
                data = PieData(dataSet).apply { setValueFormatter(PercentFormatter(binding.pieChartGastos)) }
                description.isEnabled = false
                centerText = "Gastos"
                setUsePercentValues(true)
                animateY(800)
                invalidate()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}