package com.caogenda

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caogenda.databinding.ActivityVacinaDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class VacinaDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVacinaDetailsBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private var petId: String? = null
    private var vacinaId: String? = null
    private var currentStatus: String = "Aplicada"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVacinaDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getStringExtra("PET_ID")
        vacinaId = intent.getStringExtra("VACINA_ID")
        
        setupToolbar()
        observeVacina()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeVacina() {
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return
        val vid = vacinaId ?: return

        db.collection("usuarios").document(uid)
            .collection("pets").document(pid)
            .collection("vacinas").document(vid)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val vacina = snapshot.toObject(Vacina::class.java) ?: return@addSnapshotListener
                
                currentStatus = vacina.status
                updateUI(vacina)
            }
    }

    private fun updateUI(vacina: Vacina) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        with(binding) {
            tvVacinaNome.text = vacina.nome
            tvPetNameSub.text = "para ${intent.getStringExtra("PET_NAME") ?: "seu pet"}"
            tvVeterinario.text = vacina.veterinario.ifEmpty { "-" }
            tvDataAplicacao.text = if (vacina.dataAplicacao != 0L) sdf.format(vacina.dataAplicacao) else "-"
            tvProximaDose.text = if (vacina.proximaDose != 0L) sdf.format(vacina.proximaDose) else "-"
            tvCriadoEm.text = sdf.format(vacina.criadoEm.toDate())
            tvObservacoes.text = vacina.observacoes.ifEmpty { "Nenhuma observação." }

            chipStatus.text = vacina.status
            if (vacina.status == "Pendente") {
                chipStatus.setTextColor(android.graphics.Color.parseColor("#EA580C"))
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                btnToggleStatus.text = "Marcar como Aplicada"
            } else {
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                chipStatus.setTextColor(android.graphics.Color.parseColor("#22C55E"))
                btnToggleStatus.text = "Marcar como Pendente"
            }
        }
    }

    private fun setupListeners() {
        binding.btnToggleStatus.setOnClickListener { toggleStatus() }
    }

    private fun toggleStatus() {
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return
        val vid = vacinaId ?: return

        val newStatus = if (currentStatus == "Aplicada") "Pendente" else "Aplicada"
        
        binding.btnToggleStatus.isEnabled = false
        db.collection("usuarios").document(uid)
            .collection("pets").document(pid)
            .collection("vacinas").document(vid)
            .update("status", newStatus)
            .addOnSuccessListener {
                binding.btnToggleStatus.isEnabled = true
                Toast.makeText(this, "Status atualizado!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                binding.btnToggleStatus.isEnabled = true
                Toast.makeText(this, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
            }
    }
}