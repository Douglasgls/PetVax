package com.caogenda

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caogenda.databinding.ActivityAddVacinaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddVacinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVacinaBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private var petId: String? = null
    private var dataAplicacaoTimestamp: Long = 0
    private var proximaDoseTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVacinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getStringExtra("PET_ID")
        val nomePet = intent.getStringExtra("PET_NAME")
        binding.tvParaPet.text = "para $nomePet"

        setupToolbar()
        setupDatePickers()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDatePickers() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        binding.etDataAplicacao.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                dataAplicacaoTimestamp = cal.timeInMillis
                binding.etDataAplicacao.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.etProximaDose.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                proximaDoseTimestamp = cal.timeInMillis
                binding.etProximaDose.setText(sdf.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupListeners() {
        binding.btnSalvarVacina.setOnClickListener { salvarVacina() }
    }

    private fun salvarVacina() {
        val nome = binding.etVacinaNome.text.toString().trim()
        val veterinario = binding.etVeterinario.text.toString().trim()
        val observacoes = binding.etObservacoes.text.toString().trim()
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return

        if (nome.isEmpty()) {
            Toast.makeText(this, "Preencha o nome da vacina", Toast.LENGTH_SHORT).show()
            return
        }
        if (dataAplicacaoTimestamp == 0L) {
            Toast.makeText(this, "Preencha a data de aplicação", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSalvarVacina.isEnabled = false

        val batch = db.batch()
        val petRef = db.collection("usuarios").document(uid).collection("pets").document(pid)
        
        // Registro 1: Aplicada
        val vacinaAplicadaRef = petRef.collection("vacinas").document()
        val vacinaAplicada = Vacina(
            id = vacinaAplicadaRef.id,
            nome = nome,
            dataAplicacao = dataAplicacaoTimestamp,
            veterinario = veterinario,
            observacoes = observacoes,
            proximaDose = 0,
            status = "Aplicada"
        )
        batch.set(vacinaAplicadaRef, vacinaAplicada)

        // Registro 2: Pendente (se houver próxima dose)
        if (proximaDoseTimestamp > 0) {
            val vacinaPendenteRef = petRef.collection("vacinas").document()
            val vacinaPendente = Vacina(
                id = vacinaPendenteRef.id,
                nome = nome,
                dataAplicacao = 0,
                veterinario = veterinario,
                observacoes = observacoes,
                proximaDose = proximaDoseTimestamp,
                status = "Pendente"
            )
            batch.set(vacinaPendenteRef, vacinaPendente)
        }

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "Vacina cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            binding.btnSalvarVacina.isEnabled = true
            Toast.makeText(this, "Erro ao salvar: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}