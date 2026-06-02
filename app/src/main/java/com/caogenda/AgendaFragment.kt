package com.caogenda

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.caogenda.databinding.FragmentAgendaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class AgendaFragment : Fragment() {

    private val TAG = "AgendaFragment"
    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    private lateinit var agendaAdapter: AgendaAdapter
    private val petListeners = mutableListOf<ListenerRegistration>()
    private var petsMainListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        agendaAdapter = AgendaAdapter(emptyList()) { vacina ->
            val intent = Intent(requireContext(), VacinaDetailsActivity::class.java).apply {
                putExtra("PET_ID", vacina.petId)
                putExtra("VACINA_ID", vacina.id)
                putExtra("PET_NAME", vacina.nomePet)
            }
            startActivity(intent)
        }
        binding.rvAgenda.apply {
            adapter = agendaAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        binding.btnAddVaccineEmpty.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_pets)
        }
    }

    private fun observeData() {
        val uid = auth.currentUser?.uid ?: return
        
        clearListeners()
        
        Log.d(TAG, "Observando pets para a Agenda...")
        petsMainListener = db.collection("usuarios").document(uid).collection("pets")
            .addSnapshotListener { snapshot, e ->
                if (!isAdded || isDetached) return@addSnapshotListener
                if (e != null) {
                    Log.e(TAG, "Erro ao carregar pets na Agenda", e)
                    return@addSnapshotListener
                }
                
                val pets = snapshot?.toObjects(Pet::class.java) ?: emptyList()
                observeAllVacinas(pets)
            }
    }

    private fun observeAllVacinas(pets: List<Pet>) {
        val uid = auth.currentUser?.uid ?: return
        val petVacinasMap = mutableMapOf<String, List<Vacina>>()
        
        // Limpar listeners de vacinas antigos
        petListeners.forEach { it.remove() }
        petListeners.clear()

        if (pets.isEmpty()) {
            updateUI(emptyList())
            return
        }

        pets.forEach { pet ->
            val listener = db.collection("usuarios").document(uid)
                .collection("pets").document(pet.id)
                .collection("vacinas")
                .whereEqualTo("status", "Pendente")
                .addSnapshotListener { vSnapshot, ve ->
                    if (!isAdded || isDetached) return@addSnapshotListener
                    if (ve != null) {
                        Log.e(TAG, "Erro ao carregar vacinas do pet ${pet.id}", ve)
                        return@addSnapshotListener
                    }

                    if (vSnapshot != null) {
                        val vacinas = vSnapshot.toObjects(Vacina::class.java)
                        vacinas.forEach { 
                            it.petId = pet.id
                            it.nomePet = pet.nome
                            it.fotoPetPath = pet.fotoPath
                        }
                        petVacinasMap[pet.id] = vacinas
                        
                        val allVacinas = petVacinasMap.values.flatten()
                            .filter { it.proximaDose > 0 }
                            .sortedBy { it.proximaDose }
                        
                        updateUI(allVacinas)
                    }
                }
            petListeners.add(listener)
        }
    }

    private fun updateUI(vacinas: List<Vacina>) {
        if (_binding == null) return
        
        agendaAdapter.updateData(vacinas)
        
        val isEmpty = vacinas.isEmpty()
        binding.layoutEmpty.isVisible = isEmpty
        binding.rvAgenda.isVisible = !isEmpty
    }

    private fun clearListeners() {
        Log.d(TAG, "Limpando todos os listeners da Agenda")
        petsMainListener?.remove()
        petsMainListener = null
        petListeners.forEach { it.remove() }
        petListeners.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearListeners()
        _binding = null
    }
}