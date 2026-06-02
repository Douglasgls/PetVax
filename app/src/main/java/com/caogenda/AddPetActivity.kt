package com.caogenda

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.caogenda.databinding.ActivityAddPetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddPetActivity : AppCompatActivity() {

    private val TAG = "PetVax_AddPet"
    private lateinit var binding: ActivityAddPetBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private var birthDateTimestamp: Long = 0
    private var imageUri: Uri? = null

    // Utilizando PickVisualMedia para seleção moderna de imagens
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d(TAG, "Imagem selecionada: $uri")
            imageUri = uri
            
            if (!isDestroyed && !isFinishing) {
                binding.ivPetPreview.colorFilter = null
                Glide.with(this)
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .centerCrop()
                    .into(binding.ivPetPreview)
            }
        } else {
            Log.d(TAG, "Nenhuma imagem selecionada")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDatePicker()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(year, month, day)
            birthDateTimestamp = calendar.timeInMillis
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.etBirthDate.setText(format.format(calendar.time))
        }

        binding.etBirthDate.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupListeners() {
        binding.btnSelectPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnSave.setOnClickListener {
            savePet()
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun savePet() {
        val nome = binding.etName.text.toString().trim()
        val raca = binding.etBreed.text.toString().trim()
        val pesoStr = binding.etWeight.text.toString().trim()
        val especieId = binding.rgEspecie.checkedRadioButtonId

        if (nome.isEmpty() || raca.isEmpty() || pesoStr.isEmpty() || especieId == -1) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        val especie = when (especieId) {
            R.id.rbDog -> getString(R.string.especie_dog)
            R.id.rbCat -> getString(R.string.especie_cat)
            else -> getString(R.string.especie_other)
        }

        val userId = auth.currentUser?.uid ?: return
        val petRef = db.collection("usuarios").document(userId).collection("pets").document()
        
        binding.btnSave.isEnabled = false
        
        var fileName: String? = null
        imageUri?.let { uri ->
            fileName = saveImageToInternalStorage(uri, petRef.id)
        }

        val pet = Pet(
            id = petRef.id,
            nome = nome,
            especie = especie,
            raca = raca,
            dataNascimento = birthDateTimestamp,
            peso = pesoStr.toDoubleOrNull() ?: 0.0,
            fotoPath = fileName // Salva apenas o nome do arquivo
        )

        Log.d(TAG, "Persistindo no Firestore: $pet")
        petRef.set(pet)
            .addOnSuccessListener {
                if (!isDestroyed && !isFinishing) {
                    Toast.makeText(this, "Pet cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar pet", e)
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Falha ao salvar pet", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageToInternalStorage(uri: Uri, petId: String): String? {
        return try {
            val petsDir = File(filesDir, "pets")
            if (!petsDir.exists()) petsDir.mkdirs()

            val fileName = "pet_$petId.jpg"
            val destFile = File(petsDir, fileName)
            
            Log.d(TAG, "Copiando imagem para: ${destFile.absolutePath}")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (destFile.exists()) {
                Log.d(TAG, "Arquivo gravado com sucesso (${destFile.length()} bytes)")
                fileName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro na gravação local", e)
            null
        }
    }
}