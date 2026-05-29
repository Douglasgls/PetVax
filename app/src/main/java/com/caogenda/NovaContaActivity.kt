package com.caogenda

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caogenda.databinding.NovaContaBinding
import com.google.firebase.auth.FirebaseAuth

class NovaContaActivity : AppCompatActivity() {

    private lateinit var binding: NovaContaBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = NovaContaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Ação do botão "Criar conta"
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }

        // Ação do botão "Já tenho uma conta"
        binding.btnAlreadyHaveAccount.setOnClickListener {
            finish() // Volta para a tela de login
        }
    }

    private fun validateAndRegister() {
        val name = binding.tilName.editText?.text.toString().trim()
        val email = binding.tilEmail.editText?.text.toString().trim()
        val password = binding.tilPassword.editText?.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Campo obrigatório"
            return
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Campo obrigatório"
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "E-mail inválido"
            return
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Campo obrigatório"
            return
        } else if (password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 caracteres"
            return
        } else {
            binding.tilPassword.error = null
        }

        // Cria o usuário no Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Usuário criado com sucesso!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Erro ao cadastrar: ${task.exception?.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}