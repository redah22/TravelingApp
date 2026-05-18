package com.traveling.app.travelshare.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.traveling.app.databinding.ActivityGroupsBinding
import com.traveling.app.travelshare.data.DatabaseHelper

class GroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsBinding
    private lateinit var userName: String
    private lateinit var adapter: ArrayAdapter<String>
    private val groupItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("USER_NAME") ?: "Voyageur"

        setupRecyclerView()
        loadGroups()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnCreateGroup.setOnClickListener {
            showCreateGroupDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, groupItems)
        binding.lvGroups.adapter = adapter
        binding.lvGroups.setOnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, "Groupe : ${groupItems[position]}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGroups() {
        val db = DatabaseHelper.getInstance(this)
        val groups = db.recupererGroupes()
        groupItems.clear()
        if (groups.isEmpty()) {
            groupItems.add("Aucun groupe pour l'instant. Créez-en un !")
        } else {
            groups.forEach { (_, name, creator) ->
                groupItems.add("$name  (créé par $creator)")
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showCreateGroupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null)
        val etName = EditText(this).apply { hint = "Nom du groupe" }
        val etDesc = EditText(this).apply { hint = "Description (optionnel)" }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(etName)
            addView(etDesc)
        }
        AlertDialog.Builder(this)
            .setTitle("Créer un nouveau groupe")
            .setView(container)
            .setPositiveButton("Créer") { _, _ ->
                val name = etName.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Le nom est requis.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val db = DatabaseHelper.getInstance(this)
                val result = db.creerGroupe(name, desc, userName)
                if (result != -1L) {
                    Toast.makeText(this, "Groupe \"$name\" créé !", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(this, "Erreur lors de la création.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
