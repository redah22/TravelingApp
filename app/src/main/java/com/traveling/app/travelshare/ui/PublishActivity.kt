package com.traveling.app.travelshare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.traveling.app.databinding.ActivityPublishBinding

class PublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishBinding
    private var selectedImageUri: Uri? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            binding.ivPreview.visibility = View.VISIBLE
            binding.llPickerPlaceholder.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.cardImagePicker.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnPublish.setOnClickListener {
            val description = binding.etDescription.text.toString()
            if (description.isBlank()) {
                Toast.makeText(this, "Veuillez ajouter une description.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUri == null) {
                Toast.makeText(this, "Veuillez ajouter une photo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Copier l'image en local pour la persistance
            val localUri = saveImageToInternalStorage(selectedImageUri!!)
            if (localUri == null) {
                Toast.makeText(this, "Erreur lors de la sauvegarde de l'image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val visibility = when (binding.rgVisibility.checkedRadioButtonId) {
                com.traveling.app.R.id.rbGroup -> "PRIVATE_GROUP"
                com.traveling.app.R.id.rbPrivate -> "PRIVATE"
                else -> "PUBLIC"
            }
            
            val resultIntent = Intent().apply {
                putExtra("DESCRIPTION", description)
                putExtra("IMAGE_URI", localUri.toString())
                putExtra("VISIBILITY", visibility)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Publication réussie !", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnRecordAudio.setOnClickListener {
            Toast.makeText(this, "Note vocale de 0:05 enregistrée.", Toast.LENGTH_SHORT).show()
        }

        binding.btnGenerateAITags.setOnClickListener {
            generateAITags()
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateAITags() {
        binding.chipGroupAITags.removeAllViews()
        val tags = listOf("#Nature", "#Explore", "#Travel")
        for (tag in tags) {
            val chip = Chip(this)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener { binding.chipGroupAITags.removeView(chip) }
            binding.chipGroupAITags.addView(chip)
        }
        Toast.makeText(this, "Tags IA générés !", Toast.LENGTH_SHORT).show()
    }
}
