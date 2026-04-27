package com.traveling.app.travelshare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.traveling.app.databinding.ActivityLoginBinding
import com.traveling.app.travelshare.data.DatabaseHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener { tenterConnexion() }
    }

    private fun tenterConnexion() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = DatabaseHelper.getInstance(this)
        val userName = db.verifierConnexion(email, password)

        if (userName == null) {
            Toast.makeText(this, "Email ou mot de passe incorrect.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, FeedActivity::class.java)
        intent.putExtra("IS_ANONYMOUS", false)
        intent.putExtra("USER_NAME", userName)
        startActivity(intent)
        finishAffinity()
    }
}
