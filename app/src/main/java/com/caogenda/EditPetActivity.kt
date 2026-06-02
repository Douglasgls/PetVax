package com.caogenda

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.caogenda.databinding.ActivityEditPetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditPetActivity : AppCompatActivity() {

    private val TAG = "PetVax_EditPet"
    private lateinit var binding: ActivityEditPetBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private var petId: String? = null
    private var birthDateTimestamp: Long = 0
    private var currentFotoFileName: String? = null
    private var imageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d(TAG, "Nova imagem selecionada: $uri")
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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petId = intent.getStringExtra("PET_ID")
        Log.d(TAG, "Editando petId: $petId")
        
        setupToolbar()
        setupDatePicker()
        setupListeners()
        loadPetData()
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
        binding.btnSave.setOnClickListener { saveChanges() }
        binding.btnDelete.setOnClickListener { showDeleteConfirmation() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun loadPetData() {
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return

        db.collection("usuarios").document(uid).collection("pets").document(pid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (isDestroyed || isFinishing) return@addOnSuccessListener

                val pet = snapshot.toObject(Pet::class.java) ?: return@addOnSuccessListener
                
                binding.etName.setText(pet.nome)
                binding.etBreed.setText(pet.raca)
                binding.etWeight.setText(pet.peso.toString())
                
                birthDateTimestamp = pet.dataNascimento
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.etBirthDate.setText(format.format(birthDateTimestamp))

                currentFotoFileName = pet.fotoPath
                currentFotoFileName?.let { fileName ->
                    // Resolve caminho absoluto dinamicamente
                    val file = File(File(filesDir, "pets"), fileName)
                    if (file.exists()) {
                        binding.ivPetPreview.colorFilter = null
                        Glide.with(this)
                            .load(file)
                            .centerCrop()
                            .into(binding.ivPetPreview)
                    }
                }

                when (pet.especie) {
                    getString(R.string.especie_dog) -> binding.rbDog.isChecked = true
                    getString(R.string.especie_cat) -> binding.rbCat.isChecked = true
                    else -> binding.rbOther.isChecked = true
                }
            }
    }

    private fun saveChanges() {
        val nome = binding.etName.text.toString().trim()
        val raca = binding.etBreed.text.toString().trim()
        val pesoStr = binding.etWeight.text.toString().trim()
        val especieId = binding.rgEspecie.checkedRadioButtonId

        if (nome.isEmpty() || raca.isEmpty() || pesoStr.isEmpty() || especieId == -1) {
            Toast.makeText(this, "Campos obrigatórios pendentes", Toast.LENGTH_SHORT).show()
            return
        }

        val especie = when (especieId) {
            R.id.rbDog -> getString(R.string.especie_dog)
            R.id.rbCat -> getString(R.string.especie_cat)
            else -> getString(R.string.especie_other)
        }

        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return
        
        binding.btnSave.isEnabled = false

        var updatedFileName = currentFotoFileName
        imageUri?.let { uri ->
            updatedFileName = saveImageToInternalStorage(uri, pid)
        }

        val petUpdate = mutableMapOf<String, Any>(
            "nome" to nome,
            "especie" to especie,
            "raca" to raca,
            "dataNascimento" to birthDateTimestamp,
            "peso" to (pesoStr.toDoubleOrNull() ?: 0.0),
            "fotoPath" to (updatedFileName ?: "")
        )

        db.collection("usuarios").document(uid).collection("pets").document(pid)
            .update(petUpdate)
            .addOnSuccessListener {
                if (!isDestroyed && !isFinishing) {
                    Toast.makeText(this, "Pet atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Falha ao salvar alterações", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageToInternalStorage(uri: Uri, petId: String): String? {
        return try {
            val petsDir = File(filesDir, "pets")
            if (!petsDir.exists()) petsDir.mkdirs()

            val fileName = "pet_$petId.jpg"
            val destFile = File(petsDir, fileName)
            
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Imagem substituída em: ${destFile.absolutePath}")
            fileName
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gravar imagem", e)
            null
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_pet_confirm_title)
            .setMessage(R.string.delete_pet_confirm_msg)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ -> deletePet() }
            .show()
    }

    private fun deletePet() {
        val uid = auth.currentUser?.uid ?: return
        val pid = petId ?: return

        binding.btnDelete.isEnabled = false
        
        db.collection("usuarios").document(uid).collection("pets").document(pid)
            .collection("vacinas")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.delete(db.collection("usuarios").document(uid).collection("pets").document(pid))
                
                batch.commit().addOnSuccessListener {
                    // Limpar arquivo físico
                    currentFotoFileName?.let { fileName ->
                        val file = File(File(filesDir, "pets"), fileName)
                        if (file.exists()) file.delete()
                    }
                    if (!isDestroyed && !isFinishing) {
                        Toast.makeText(this, "Pet removido", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
    }
}