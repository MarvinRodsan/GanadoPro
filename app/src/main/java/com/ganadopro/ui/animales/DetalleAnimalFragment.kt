package com.ganadopro.ui.animales

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ganadopro.R
import com.ganadopro.databinding.FragmentDetalleAnimalBinding
import com.ganadopro.util.PhotoUtils
import com.ganadopro.util.sinCerosDeMas
import com.ganadopro.viewmodel.AnimalViewModel
import kotlinx.coroutines.launch

class DetalleAnimalFragment : Fragment() {

    private var _binding: FragmentDetalleAnimalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnimalViewModel by viewModels()
    private val args: DetalleAnimalFragmentArgs by navArgs()
    private var animalId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        animalId = args.animalId
        cargarAnimal()
        setupAccesosRapidos()
    }

    // Se vuelve a ejecutar cada vez que se regresa a esta pantalla (por
    // ejemplo después de editar el animal), así la ficha siempre está al día.
    private fun cargarAnimal() {
        // viewLifecycleOwner: si se sale de la pantalla antes de que termine
        // la consulta, la corrutina se cancela y no toca vistas ya destruidas.
        viewLifecycleOwner.lifecycleScope.launch {
            val animal = viewModel.getById(animalId)
            if (animal == null) {
                Toast.makeText(requireContext(), "Ese animal ya no existe en el registro", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@launch
            }

            binding.tvNombre.text = animal.nombre
            binding.tvCodigo.text = "Código: ${animal.codigo}"
            binding.tvRaza.text = "Raza: ${animal.raza}"
            binding.tvSexo.text = "Sexo: ${animal.sexo}"
            binding.tvFecha.text = "Nacimiento: ${animal.fechaNacimiento}"
            binding.tvEstado.text = animal.estado

            // Peso inicial y, si ya hay pesajes en Control de Peso, el más reciente.
            val ultimoPeso = viewModel.getUltimoPeso(animalId)
            binding.tvPeso.text = if (ultimoPeso != null) {
                "Peso inicial: ${animal.pesoInicial.sinCerosDeMas()} kg\n" +
                    "Peso actual: ${ultimoPeso.peso.sinCerosDeMas()} kg (${ultimoPeso.fecha})"
            } else {
                "Peso inicial: ${animal.pesoInicial.sinCerosDeMas()} kg"
            }

            // La producción de leche solo aplica a hembras.
            binding.btnProduccion.isEnabled = animal.sexo == "Hembra"

            PhotoUtils.load(binding.ivFoto, animal.foto, R.drawable.vaca)
        }
    }

    private fun setupAccesosRapidos() {
        binding.btnHistorial.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToHistorial(animalId)
            findNavController().navigate(action)
        }
        binding.btnVacunas.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToVacunas(animalId)
            findNavController().navigate(action)
        }
        binding.btnAlimentacion.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToAlimentacion(animalId)
            findNavController().navigate(action)
        }
        binding.btnPeso.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToPeso(animalId)
            findNavController().navigate(action)
        }
        binding.btnProduccion.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToProduccion(animalId)
            findNavController().navigate(action)
        }
        binding.btnReproduccion.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToReproduccion(animalId)
            findNavController().navigate(action)
        }
        binding.btnEditar.setOnClickListener {
            val action = DetalleAnimalFragmentDirections.actionDetalleToFormAnimal(animalId)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
