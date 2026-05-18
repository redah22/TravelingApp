package com.traveling.app.travelshare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.traveling.app.R
import com.traveling.app.databinding.ActivityPublishBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishBinding
    private var selectedImageUri: Uri? = null
    private var selectedLat: Double = 0.0
    private var selectedLon: Double = 0.0
    private var selectedLieuNom: String = "Lieu inconnu"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            binding.ivPreview.visibility = View.VISIBLE
            binding.llPickerPlaceholder.visibility = View.GONE
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            val fileName = "img_cam_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            val uri = Uri.fromFile(file)
            selectedImageUri = uri
            binding.ivPreview.setImageBitmap(bitmap)
            binding.ivPreview.visibility = View.VISIBLE
            binding.llPickerPlaceholder.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefillDesc = intent.getStringExtra("PREFILL_DESC")
        val prefillLocation = intent.getStringExtra("PREFILL_LOCATION")
        
        if (!prefillDesc.isNullOrEmpty()) {
            binding.etDescription.setText(prefillDesc)
        }
        if (!prefillLocation.isNullOrEmpty()) {
            binding.etLocation.setText(prefillLocation)
            // Lancer la recherche pour géocoder automatiquement
            searchLocation(prefillLocation)
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.cardImagePicker.setOnClickListener {
            val options = arrayOf("Prendre une photo", "Choisir dans la galerie")
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Ajouter une photo")
                .setItems(options) { _, which ->
                    if (which == 0) {
                        takePictureLauncher.launch(null)
                    } else {
                        selectImageLauncher.launch("image/*")
                    }
                }
                .show()
        }

        // Autocomplétion du lieu via Photon API
        binding.etLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: return
                if (query.length >= 2) searchLocation(query)
            }
        })

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

            val localUri = saveImageToInternalStorage(selectedImageUri!!)
            if (localUri == null) {
                Toast.makeText(this, "Erreur lors de la sauvegarde de l'image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val visibility = when (binding.rgVisibility.checkedRadioButtonId) {
                R.id.rbGroup -> "PRIVATE_GROUP"
                R.id.rbPrivate -> "PRIVATE"
                else -> "PUBLIC"
            }

            val scope = try {
                com.traveling.app.travelshare.models.ShareScope.valueOf(visibility)
            } catch (e: Exception) {
                com.traveling.app.travelshare.models.ShareScope.PUBLIC
            }

            // Récupérer l'utilisateur courant
            val prefs = getSharedPreferences("user_auth", android.content.Context.MODE_PRIVATE)
            val email = prefs.getString("current_user", "anonymous") ?: "anonymous"
            val userName = if (email.contains("@")) email.split("@")[0] else email

            val newPost = com.traveling.app.travelshare.models.PhotoPost(
                id = "p${System.currentTimeMillis()}",
                autheur = com.traveling.app.travelshare.models.User("u_me", userName, "@me", ""),
                photoUrl = localUri.toString(),
                descriptionText = description,
                lieuNom = selectedLieuNom.ifEmpty { "Localisation inconnue" },
                latitude = selectedLat,
                longitude = selectedLon,
                datePublicationMillis = System.currentTimeMillis(),
                scope = scope
            )

            com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this).insererPost(newPost)

            Toast.makeText(this, "Publication réussie !", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }



        binding.btnGenerateAITags.setOnClickListener {
            generateAITags()
        }
    }

    private fun searchLocation(query: String) {
        executor.execute {
            try {
                val url = URL("https://photon.komoot.io/api/?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=5&lang=fr")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0")
                conn.connectTimeout = 5000
                val reader = conn.inputStream.bufferedReader()
                val json = JSONObject(reader.readText())
                val features = json.getJSONArray("features")

                val suggestions = mutableListOf<String>()
                val coords = mutableListOf<Pair<Double, Double>>()

                for (i in 0 until features.length()) {
                    val props = features.getJSONObject(i).getJSONObject("properties")
                    val geom = features.getJSONObject(i).getJSONObject("geometry")
                    val name = props.optString("name", "")
                    val city = props.optString("city", "")
                    val country = props.optString("country", "")
                    val lon = geom.getJSONArray("coordinates").getDouble(0)
                    val lat = geom.getJSONArray("coordinates").getDouble(1)
                    val label = listOf(name, city, country).filter { it.isNotEmpty() }.joinToString(", ")
                    suggestions.add(label)
                    coords.add(Pair(lat, lon))
                }

                mainHandler.post {
                    if (suggestions.isEmpty()) return@post
                    val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggestions)
                    binding.etLocation.setAdapter(adapter)
                    binding.etLocation.showDropDown()
                    binding.etLocation.setOnItemClickListener { _, _, position, _ ->
                        selectedLat = coords[position].first
                        selectedLon = coords[position].second
                        selectedLieuNom = suggestions[position]
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        val description = binding.etDescription.text.toString().lowercase()
        val tagMap = mapOf(
            listOf("plage", "mer", "ocean", "surf") to listOf("#Plage", "#Summer", "#Ocean"),
            listOf("montagne", "alpe", "neige", "ski", "randon") to listOf("#Montagne", "#Nature", "#Hiking"),
            listOf("paris", "ville", "urban", "city") to listOf("#City", "#Urban", "#Travel"),
            listOf("japan", "tokyo", "asie", "asia") to listOf("#Asia", "#Japan", "#Culture"),
            listOf("food", "restaurant", "cuisine", "gastr") to listOf("#FoodTravel", "#Gastronomie")
        )
        val tags = mutableSetOf("#Voyage", "#TravelShare")
        for ((keywords, associatedTags) in tagMap) {
            if (keywords.any { description.contains(it) }) tags.addAll(associatedTags)
        }
        for (tag in tags) {
            val chip = Chip(this)
            chip.text = tag
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener { binding.chipGroupAITags.removeView(chip) }
            binding.chipGroupAITags.addView(chip)
        }
        Toast.makeText(this, "${tags.size} tags générés !", Toast.LENGTH_SHORT).show()
    }
}
