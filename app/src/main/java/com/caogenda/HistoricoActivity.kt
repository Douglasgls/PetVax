package com.caogenda

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.caogenda.databinding.ActivityHistoricoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class HistoricoActivity : AppCompatActivity() {

    private val TAG = "HistoricoActivity"
    private lateinit var binding: ActivityHistoricoBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var petAdapter: PetSelectorAdapter
    private lateinit var historyAdapter: HistoricoAdapter
    
    private var selectedPet: Pet? = null
    private var allVacinas = listOf<Vacina>()
    private var currentFilter = "Todos"
    
    private var petsListener: ListenerRegistration? = null
    private var vacinasListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoricoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAdapters()
        setupFilters()
        observePets()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupAdapters() {
        petAdapter = PetSelectorAdapter(emptyList()) { pet ->
            selectedPet = pet
            observeVacinas(pet.id)
        }
        binding.rvPetsSelector.adapter = petAdapter

        historyAdapter = HistoricoAdapter(emptyList()) { vacina ->
            val intent = Intent(this, VacinaDetailsActivity::class.java).apply {
                putExtra("PET_ID", selectedPet?.id)
                putExtra("VACINA_ID", vacina.id)
                putExtra("PET_NAME", selectedPet?.nome)
            }
            startActivity(intent)
        }
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipApplied -> "Aplicada"
                R.id.chipPending -> "Pendente"
                else -> "Todos"
            }
            applyFilters()
        }

        binding.btnAddVaccineEmpty.setOnClickListener {
            selectedPet?.let { pet ->
                val intent = Intent(this, AddVacinaActivity::class.java).apply {
                    putExtra("PET_ID", pet.id)
                    putExtra("PET_NAME", pet.nome)
                }
                startActivity(intent)
            }
        }
    }

    private fun observePets() {
        val uid = auth.currentUser?.uid ?: return
        petsListener = db.collection("usuarios").document(uid).collection("pets")
            .orderBy("nome")
            .addSnapshotListener { snapshot, e ->
                if (isDestroyed || isFinishing) return@addSnapshotListener
                if (e != null) {
                    Log.e(TAG, "Erro ao ouvir pets", e)
                    return@addSnapshotListener
                }
                
                val pets = snapshot?.toObjects(Pet::class.java) ?: emptyList()
                petAdapter.updateList(pets)
                if (selectedPet == null && pets.isNotEmpty()) {
                    selectedPet = pets[0]
                    observeVacinas(pets[0].id)
                }
            }
    }

    private fun observeVacinas(petId: String) {
        vacinasListener?.remove()
        val uid = auth.currentUser?.uid ?: return

        vacinasListener = db.collection("usuarios").document(uid)
            .collection("pets").document(petId)
            .collection("vacinas")
            .orderBy("dataAplicacao", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (isDestroyed || isFinishing) return@addSnapshotListener
                if (e != null) {
                    Log.e(TAG, "Erro ao ouvir vacinas do pet $petId", e)
                    return@addSnapshotListener
                }
                
                allVacinas = snapshot?.toObjects(Vacina::class.java) ?: emptyList()
                updateSummary()
                applyFilters()
            }
    }

    private fun updateSummary() {
        val applied = allVacinas.count { it.status == "Aplicada" }
        val pending = allVacinas.count { it.status == "Pendente" }
        val next = allVacinas.filter { it.status == "Pendente" && it.proximaDose > 0 }
            .minByOrNull { it.proximaDose }

        binding.tvSummaryApplied.text = getString(R.string.summary_applied, applied)
        binding.tvSummaryPending.text = getString(R.string.summary_pending_count, pending)
        
        if (next != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvSummaryNext.text = getString(R.string.summary_next_vaccine, "${next.nome} em ${sdf.format(next.proximaDose)}")
        } else {
            binding.tvSummaryNext.text = "Nenhuma vacina pendente agendada."
        }
    }

    private fun applyFilters() {
        val filtered = if (currentFilter == "Todos") {
            allVacinas
        } else {
            allVacinas.filter { it.status == currentFilter }
        }

        historyAdapter.updateData(filtered)
        
        val isEmpty = filtered.isEmpty()
        binding.layoutEmpty.isVisible = isEmpty
        binding.rvHistory.isVisible = !isEmpty
        binding.cardSummary.isVisible = allVacinas.isNotEmpty()
        binding.chipGroupFilters.isVisible = allVacinas.isNotEmpty()
    }

    override fun onDestroy() {
        super.onDestroy()
        // BUG FIX: Clean up listeners
        petsListener?.remove()
        vacinasListener?.remove()
    }
}