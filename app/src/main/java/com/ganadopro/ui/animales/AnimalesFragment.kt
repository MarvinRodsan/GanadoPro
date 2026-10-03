package com.ganadopro.ui.animales

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ganadopro.R
import com.ganadopro.databinding.FragmentAnimalesBinding
import com.ganadopro.viewmodel.AnimalViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AnimalesFragment : Fragment() {

    private var _binding: FragmentAnimalesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnimalViewModel by viewModels()
    private lateinit var adapter: AnimalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMenu()
        observeAnimales()

        binding.fabAgregar.setOnClickListener {
            findNavController().navigate(R.id.action_animales_to_formAnimal)
        }
    }

    private fun setupRecyclerView() {
        adapter = AnimalAdapter(
            onEdit = { animal ->
                val action = AnimalesFragmentDirections.actionAnimalestoDetalleAnimal(animal.id)
                findNavController().navigate(action)
            },
            onDelete = { animal ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar animal")
                    .setMessage(
                        "¿Seguro que deseas eliminar a ${animal.nombre}?\n\n" +
                            "También se borrará todo su historial: vacunas, pesos, producción, " +
                            "reproducción, alimentación y ventas. Esta acción no se puede deshacer.\n\n" +
                            "Si el animal se vendió o murió, es mejor editarlo y cambiar su estado."
                    )
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.delete(animal)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_animales, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(q: String?) = false
                    override fun onQueryTextChange(q: String?): Boolean {
                        viewModel.setSearchQuery(q ?: "")
                        return true
                    }
                })
            }
            override fun onMenuItemSelected(item: MenuItem) = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeAnimales() {
        viewModel.animales.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}