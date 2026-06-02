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
import com.caogenda.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    
    private lateinit var petAdapter: PetAdapter
    private lateinit var nextVacinaAdapter: NextVacinaAdapter
    
    private val petListeners = mutableListOf<ListenerRegistration>()
    private var petsMainListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupListeners()
        loadUserData()
        observeData()
    }

    private fun setupRecyclerViews() {
        petAdapter = PetAdapter(emptyList())
        binding.rvMyPets.apply {
            adapter = petAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = false
        }

        nextVacinaAdapter = NextVacinaAdapter(emptyList())
        binding.rvNextVaccines.apply {
            adapter = nextVacinaAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        val addPetIntent = Intent(requireContext(), AddPetActivity::class.java)
        binding.btnActionAddPet.setOnClickListener { startActivity(addPetIntent) }
        binding.btnAddPetEmpty.setOnClickListener { startActivity(addPetIntent) }

        binding.btnViewAllVaccines.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_agenda)
        }

        binding.btnViewAllPets.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_pets)
        }

        binding.btnActionHistory.setOnClickListener {
            val intent = Intent(requireContext(), HistoricoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        val name = user?.displayName ?: user?.email?.split("@")?.get(0) ?: "Usuário"
        binding.tvGreeting.text = getString(R.string.home_greeting, name)
    }

    private fun observeData() {
        val userId = auth.currentUser?.uid ?: return

        clearListeners()

        Log.d(TAG, "Observando pets para o usuário: $userId")
        petsMainListener = db.collection("usuarios").document(userId)
            .collection("pets")
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Erro ao ouvir pets", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener
                
                val pets = snapshot.toObjects(Pet::class.java)
                Log.d(TAG, "${pets.size} pets encontrados.")
                updatePetsUI(pets)
                observeAllVacinas(pets)
            }
    }

    private fun observeAllVacinas(pets: List<Pet>) {
        val userId = auth.currentUser?.uid ?: return
        val petVacinasMap = mutableMapOf<String, List<Vacina>>()
        
        if (pets.isEmpty()) {
            updateDashboard(0, 0, 0)
            nextVacinaAdapter.updateList(emptyList())
            updateVaccinesEmptyState(true)
            return
        }

        pets.forEach { pet ->
            val listener = db.collection("usuarios").document(userId)
                .collection("pets").document(pet.id)
                .collection("vacinas")
                .addSnapshotListener { vSnapshot, ve ->
                    if (ve != null) {
                        Log.e(TAG, "Erro ao ouvir vacinas do pet ${pet.id}", ve)
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
                        aggregateAndRefresh(pets.size, petVacinasMap)
                    }
                }
            petListeners.add(listener)
        }
    }

    private fun aggregateAndRefresh(petsCount: Int, map: Map<String, List<Vacina>>) {
        val allVacinas = map.values.flatten()
        
        val pendingVaccines = allVacinas.filter { 
            it.status == "Pendente" && it.proximaDose > 0 
        }.sortedBy { it.proximaDose }

        nextVacinaAdapter.updateList(pendingVaccines)
        updateVaccinesEmptyState(pendingVaccines.isEmpty())

        val okCount = allVacinas.count { it.status == "Aplicada" }
        val pendingCount = allVacinas.count { it.status == "Pendente" }

        updateDashboard(petsCount, okCount, pendingCount)
    }

    private fun updateVaccinesEmptyState(isEmpty: Boolean) {
        binding.cardEmptyVaccines.isVisible = isEmpty
        binding.rvNextVaccines.isVisible = !isEmpty
        binding.btnViewAllVaccines.isVisible = !isEmpty
    }

    private fun updateDashboard(pets: Int, ok: Int, pending: Int) {
        binding.tvSummaryPetsCount.text = pets.toString()
        binding.tvSummaryVaccinesOkCount.text = ok.toString()
        binding.tvSummaryPendingCount.text = pending.toString()
    }

    private fun updatePetsUI(pets: List<Pet>) {
        petAdapter.updateList(pets)
        val hasPets = pets.isNotEmpty()
        binding.layoutEmptyPets.isVisible = !hasPets
        binding.rvMyPets.isVisible = hasPets
    }

    private fun clearListeners() {
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