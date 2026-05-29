package com.caogenda

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.caogenda.databinding.LoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializa o ViewBinding
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Ação do botão de Login
        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }

        // Ação do botão "Criar nova conta"
        binding.btnCreateAccount.setOnClickListener {
            val intent = Intent(this, NovaContaActivity::class.java)
            startActivity(intent)
        }

        // Ação do link "Esqueci minha senha"
        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidade em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndLogin() {
        val email = binding.tilEmail.editText?.text.toString().trim()
        val password = binding.tilPassword.editText?.text.toString().trim()

        if (email.isEmpty()) {
            binding.tilEmail.error = "Campo obrigatório"
            return
        } else {
            binding.tilEmail.error = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "E-mail inválido"
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Campo obrigatório"
            return
        } else {
            binding.tilPassword.error = null
        }

        // Realiza o login com Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Erro ao autenticar: ${task.exception?.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Verifica se já está logado
        if (auth.currentUser != null) {
            navigateToMain()
        }
    }
}