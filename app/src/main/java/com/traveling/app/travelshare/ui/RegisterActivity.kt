package com.traveling.app.travelshare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.traveling.app.databinding.ActivityRegisterBinding
import com.traveling.app.travelshare.data.DatabaseHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener { tenterInscription() }
    }

    private fun tenterInscription() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Adresse email invalide.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = DatabaseHelper.getInstance(this)

        if (db.emailExiste(email)) {
            Toast.makeText(this, "Cet email est déjà utilisé.", Toast.LENGTH_LONG).show()
            return
        }

        val result = db.insererUtilisateur(name, email, password)
        if (result == -1L) {
            Toast.makeText(this, "Erreur lors de l'inscription.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- INTEG TRAVELPATH: Save session in SharedPreferences ---
        val prefs = getSharedPreferences("user_auth", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("pass_$email", password)
            .putString("current_user", email) // TravelPath utilisera le prefix de l'email mais l'UI Profile de TravelShare a le pseudo
            .apply()

        Toast.makeText(this, "Inscription réussie ! Bienvenue $name 🎉", Toast.LENGTH_LONG).show()

        val intent = Intent(this, com.traveling.app.travelpath.MainActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}
