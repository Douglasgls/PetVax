package com.caogenda

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.caogenda.databinding.FragmentPetsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PetsFragment : Fragment() {

    private var _binding: FragmentPetsBinding? = null
    private val binding get() = _binding!!
    
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private lateinit var petAdapter: PetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observePets()
    }

    private fun setupRecyclerView() {
        petAdapter = PetAdapter(emptyList())
        binding.rvPets.adapter = petAdapter
    }

    private fun setupListeners() {
        val intent = Intent(requireContext(), AddPetActivity::class.java)
        binding.btnNewPet.setOnClickListener { startActivity(intent) }
        binding.btnNewPetEmpty.setOnClickListener { startActivity(intent) }
    }

    private fun observePets() {
        val userId = auth.currentUser?.uid ?: return
        
        binding.progressBar.isVisible = true
        
        // Escuta em tempo real as mudanças na sub-coleção "pets" do usuário
        db.collection("usuarios").document(userId)
            .collection("pets")
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                binding.progressBar.isVisible = false
                
                if (e != null) {
                    Toast.makeText(requireContext(), "Erro ao carregar pets: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val petsList = snapshot.toObjects(Pet::class.java)
                    updateUI(petsList)
                }
            }
    }

    private fun updateUI(pets: List<Pet>) {
        petAdapter.updateList(pets)
        
        binding.tvPetsCount.text = getString(R.string.pets_count, pets.size)
        
        val isEmpty = pets.isEmpty()
        binding.layoutEmpty.isVisible = isEmpty
        binding.rvPets.isVisible = !isEmpty
        binding.tvPetsCount.isVisible = !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}