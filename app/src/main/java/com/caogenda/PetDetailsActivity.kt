package com.caogenda

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.caogenda.databinding.ActivityPetDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.File

class PetDetailsActivity : AppCompatActivity() {

    private val TAG = "PetVax_Details"
    private lateinit var binding: ActivityPetDetailsBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private var petId: String? = null
    private var nomePet: String? = null
    private lateinit var vacinaAdapter: VacinaAdapter
    
    private var petListener: ListenerRegistration? = null
    private var vacinasListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getStringExtra("PET_ID")
        
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadPetDetails()
        observeVacinas()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        vacinaAdapter = VacinaAdapter(emptyList())
        binding.rvVacinas.layoutManager = LinearLayoutManager(this)
        binding.rvVacinas.adapter = vacinaAdapter
    }

    private fun setupListeners() {
        binding.btnAdicionarVacina.setOnClickListener { navigateToAddVacina() }
        binding.btnAdicionarHistorico.setOnClickListener { navigateToAddVacina() }
    }

    private fun navigateToAddVacina() {
        val intent = Intent(this, AddVacinaActivity::class.java).apply {
            putExtra("PET_ID", petId)
            putExtra("PET_NAME", nomePet)
        }
        startActivity(intent)
    }

    private fun loadPetDetails() {
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return

        petListener = db.collection("usuarios").document(uid).collection("pets").document(pid)
            .addSnapshotListener { snapshot, e ->
                if (isDestroyed || isFinishing) return@addSnapshotListener
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val pet = snapshot.toObject(Pet::class.java) ?: return@addSnapshotListener
                nomePet = pet.nome
                
                with(binding) {
                    tvPetNameHeader.text = pet.nome
                    tvPetBreedHeader.text = pet.raca
                    tvEspecieTag.text = pet.especie
                    tvIdade.text = pet.calcularIdade()
                    tvPeso.text = "${pet.peso} kg"

                    val fileName = pet.fotoPath
                    if (fileName != null && fileName.isNotEmpty()) {
                        // Resolve caminho relativo
                        val file = File(File(filesDir, "pets"), fileName)

                        Log.d("PetImageLog", "Caminho resolvido: ${file.absolutePath} | Existe no disco? ${file.exists()}")


                        if (file.exists() && !isDestroyed && !isFinishing) {


                            ivPetHeader.colorFilter = null
                            Glide.with(this@PetDetailsActivity)
                                .load(file)
                                .placeholder(R.drawable.ic_pet_placeholder)
                                .error(R.drawable.ic_pet_placeholder)
                                .centerCrop()
                                .override(1080,660)
                                .into(ivPetHeader)

                            Log.d(
                              "PET_IMAGE",
                             "pet details: width=${ivPetHeader.width} height=${ivPetHeader.height}"
                            )

                        } else {
                            ivPetHeader.setImageResource(R.drawable.ic_pet_placeholder)
                            ivPetHeader.setColorFilter(null)
                        }
                    } else {
                        ivPetHeader.setImageResource(R.drawable.ic_pet_placeholder)
                        ivPetHeader.setColorFilter(null)
                    }
                }
            }
    }

    private fun observeVacinas() {
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return

        vacinasListener = db.collection("usuarios").document(uid).collection("pets").document(pid)
            .collection("vacinas")
            .orderBy("criadoEm", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (isDestroyed || isFinishing) return@addSnapshotListener
                
                val vacinasList = snapshot?.toObjects(Vacina::class.java) ?: emptyList()
                vacinasList.forEach { 
                    it.petId = pid
                    it.nomePet = nomePet ?: ""
                }
                vacinaAdapter.updateList(vacinasList)
                
                binding.tvVacinasCount.text = vacinasList.size.toString()
                val aplicadas = vacinasList.count { it.status == "Aplicada" }
                val pendentes = vacinasList.count { it.status == "Pendente" }
                
                binding.tvAplicadasCount.text = "• $aplicadas aplicadas"
                binding.tvPendentesCount.text = "• $pendentes pendentes"
            }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.pet_details_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_pet -> {
                val intent = Intent(this, EditPetActivity::class.java).apply {
                    putExtra("PET_ID", petId)
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        petListener?.remove()
        vacinasListener?.remove()
    }
}