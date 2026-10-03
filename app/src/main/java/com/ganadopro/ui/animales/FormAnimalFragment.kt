package com.ganadopro.ui.animales

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ganadopro.R
import com.ganadopro.data.entities.Animal
import com.ganadopro.databinding.FragmentFormAnimalBinding
import com.ganadopro.util.PhotoUtils
import com.ganadopro.util.aDecimalOrNull
import com.ganadopro.util.mostrarSelectorFecha
import com.ganadopro.util.sinCerosDeMas
import com.ganadopro.viewmodel.AnimalViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FormAnimalFragment : Fragment() {

    private var _binding: FragmentFormAnimalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnimalViewModel by viewModels()
    private val args: FormAnimalFragmentArgs by navArgs()
    private var animalEditando: Animal? = null
    private var photoFile: File? = null
    private var fotoPath: String? = null
    private var guardando = false
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    companion object {
        private const val ESTADO_FOTO_PATH = "estado_foto_path"
        private const val ESTADO_ARCHIVO_CAMARA = "estado_archivo_camara"

        // Estos textos se guardan tal cual en la base de datos. Otras partes
        // de la app dependen de ellos (conteos de "Activo", gráficas por sexo
        // y raza, partos solo para "Hembra"): no renombrar sin revisarlas.
        private val RAZAS = listOf("Brahman", "Holstein", "Jersey", "Angus", "Simmental", "Pardo Suizo", "Otra")
        private val SEXOS = listOf("Macho", "Hembra")
        private val ESTADOS = listOf("Activo", "Vendido", "Fallecido")
    }

    // Pedir permiso cámara
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            abrirCamara()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador cámara
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Guardamos la ruta del archivo interno, no el URI temporal:
            // el archivo ya vive en almacenamiento propio de la app (ver PhotoUtils),
            // así que sigue siendo válido aunque el sistema limpie cachés o el
            // teléfono se reinicie.
            fotoPath = photoFile?.absolutePath
            PhotoUtils.load(binding.ivFotoPreview, fotoPath, R.drawable.vaca)
        } else {
            // El usuario canceló la cámara: se borra el archivo vacío que se
            // había reservado para la foto, para no acumular basura.
            photoFile?.delete()
        }
        photoFile = null
    }

    // Lanzador galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Copiamos la imagen a almacenamiento interno de inmediato: el URI que
            // entrega la galería puede dejar de ser válido tras un reinicio,
            // según el proveedor (Fotos, Descargas, Drive, etc.).
            val rutaInterna = PhotoUtils.copyToInternalStorage(requireContext(), it)
            if (rutaInterna != null) {
                fotoPath = rutaInterna
                PhotoUtils.load(binding.ivFotoPreview, fotoPath, R.drawable.vaca)
            } else {
                Toast.makeText(requireContext(), "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Mientras la cámara está abierta, Android puede destruir esta pantalla
    // (poca memoria, o al girar el teléfono). Se guarda dónde iba a quedar la
    // foto y cuál era la foto elegida, para no perderlas al volver.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { estado ->
            fotoPath = estado.getString(ESTADO_FOTO_PATH)
            photoFile = estado.getString(ESTADO_ARCHIVO_CAMARA)?.let { File(it) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ESTADO_FOTO_PATH, fotoPath)
        outState.putString(ESTADO_ARCHIVO_CAMARA, photoFile?.absolutePath)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupDatePicker()
        setupFotoButtons()

        if (args.animalId > 0) {
            // El mismo formulario sirve para crear y para editar.
            (requireActivity() as AppCompatActivity).supportActionBar?.title = "Editar Animal"
            binding.btnGuardar.text = "Guardar cambios"
            // savedInstanceState != null: la pantalla se está recreando (giro
            // del teléfono) y los campos ya traen lo que el usuario había
            // escrito; no se pisan con los valores de la base de datos.
            cargarAnimal(args.animalId, rellenarCampos = savedInstanceState == null)
        }
        if (fotoPath != null) {
            PhotoUtils.load(binding.ivFotoPreview, fotoPath, R.drawable.vaca)
        }

        binding.btnGuardar.setOnClickListener { guardar() }
    }

    private fun setupFotoButtons() {
        binding.btnTomarFoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                abrirCamara()
            } else {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.btnGaleria.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun abrirCamara() {
        val (archivo, uri) = PhotoUtils.createCameraDestination(requireContext())
        photoFile = archivo
        try {
            takePictureLauncher.launch(uri)
        } catch (e: android.content.ActivityNotFoundException) {
            // Teléfono o emulador sin ninguna app de cámara instalada.
            archivo.delete()
            photoFile = null
            Toast.makeText(requireContext(), "No se encontró una app de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        val razaAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            RAZAS
        )
        razaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRaza.adapter = razaAdapter

        val sexoAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            SEXOS
        )
        sexoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spSexo.adapter = sexoAdapter

        val estadoAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ESTADOS
        )
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spEstado.adapter = estadoAdapter
    }

    private fun setupDatePicker() {
        // Una fecha de nacimiento no puede ser futura: el calendario no deja elegirla.
        binding.etFechaNacimiento.setOnClickListener {
            mostrarSelectorFecha(binding.etFechaNacimiento, permitirFuturo = false)
        }
    }

    private fun cargarAnimal(id: Long, rellenarCampos: Boolean) {
        // viewLifecycleOwner: si el usuario sale de la pantalla antes de que
        // termine la consulta, la corrutina se cancela y no toca vistas ya
        // destruidas.
        viewLifecycleOwner.lifecycleScope.launch {
            val animal = viewModel.getById(id)
            if (animal == null) {
                Toast.makeText(requireContext(), "Ese animal ya no existe en el registro", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return@launch
            }
            animalEditando = animal

            // Foto actual, salvo que el usuario ya haya elegido una nueva.
            if (fotoPath == null && animal.foto != null) {
                fotoPath = animal.foto
                PhotoUtils.load(binding.ivFotoPreview, animal.foto, R.drawable.vaca)
            }
            if (!rellenarCampos) return@launch

            binding.etCodigo.setText(animal.codigo)
            binding.etNombre.setText(animal.nombre)
            binding.etFechaNacimiento.setText(animal.fechaNacimiento)
            binding.etPeso.setText(animal.pesoInicial.sinCerosDeMas())

            binding.spRaza.setSelection(RAZAS.indexOf(animal.raza).coerceAtLeast(0))
            binding.spSexo.setSelection(SEXOS.indexOf(animal.sexo).coerceAtLeast(0))
            binding.spEstado.setSelection(ESTADOS.indexOf(animal.estado).coerceAtLeast(0))
        }
    }

    private fun guardar() {
        // Evita que un doble toque en "Guardar" registre el animal dos veces.
        if (guardando) return

        val codigo = binding.etCodigo.text.toString().trim()
        val nombre = binding.etNombre.text.toString().trim()
        val fecha = binding.etFechaNacimiento.text.toString().trim()
        val pesoStr = binding.etPeso.text.toString().trim()

        // Limpiar errores previos
        binding.etCodigo.error = null
        binding.etNombre.error = null
        binding.etFechaNacimiento.error = null
        binding.etPeso.error = null

        var valido = true

        if (codigo.isBlank()) {
            binding.etCodigo.error = "Requerido"
            valido = false
        }
        if (nombre.isBlank()) {
            binding.etNombre.error = "Requerido"
            valido = false
        }
        if (fecha.isBlank()) {
            binding.etFechaNacimiento.error = "Requerido"
            valido = false
        } else {
            val fechaNac = runCatching { sdf.parse(fecha) }.getOrNull()
            if (fechaNac == null) {
                binding.etFechaNacimiento.error = "Fecha inválida"
                valido = false
            } else if (fechaNac.after(Date())) {
                binding.etFechaNacimiento.error = "No puede ser una fecha futura"
                valido = false
            }
        }

        // El peso es obligatorio, debe ser numérico y mayor a 0. Nunca se
        // guarda 0.0 como "valor por defecto" cuando el usuario escribió
        // algo inválido: si no es un número válido, se muestra el error y
        // no se guarda nada.
        val peso: Double
        if (pesoStr.isBlank()) {
            binding.etPeso.error = "Requerido"
            valido = false
            peso = 0.0
        } else {
            val pesoParseado = pesoStr.aDecimalOrNull()
            when {
                pesoParseado == null -> {
                    binding.etPeso.error = "Debe ser un número válido"
                    valido = false
                    peso = 0.0
                }
                pesoParseado <= 0.0 -> {
                    binding.etPeso.error = "Debe ser mayor a 0"
                    valido = false
                    peso = 0.0
                }
                else -> peso = pesoParseado
            }
        }

        if (!valido) return

        // Código no duplicado: solo es un problema si el código pertenece a
        // OTRO animal (al editar, el propio animal conserva su código).
        guardando = true
        viewLifecycleOwner.lifecycleScope.launch {
            val existente = viewModel.getByCodigo(codigo)
            if (existente != null && existente.id != (animalEditando?.id ?: -1L)) {
                binding.etCodigo.error = "Ya existe un animal con este código"
                binding.etCodigo.requestFocus()
                guardando = false
                return@launch
            }

            val animal = Animal(
                id = animalEditando?.id ?: 0,
                codigo = codigo,
                nombre = nombre,
                raza = binding.spRaza.selectedItem.toString(),
                sexo = binding.spSexo.selectedItem.toString(),
                fechaNacimiento = fecha,
                pesoInicial = peso,
                estado = binding.spEstado.selectedItem.toString(),
                foto = fotoPath ?: animalEditando?.foto
            )

            // join(): se espera a que el guardado termine ANTES de cerrar la
            // pantalla. Al cerrarla se destruye el ViewModel y se cancelan sus
            // tareas pendientes; sin esperar, el guardado podría cancelarse.
            if (animalEditando == null) viewModel.insert(animal).join()
            else viewModel.update(animal).join()

            Toast.makeText(
                requireContext(),
                if (animalEditando == null) "Animal registrado" else "Cambios guardados",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}