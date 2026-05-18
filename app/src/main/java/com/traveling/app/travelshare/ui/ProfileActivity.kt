package com.traveling.app.travelshare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.traveling.app.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private var userName = "Voyageur"
    private var isAnonymous = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra("USER_NAME") ?: "Voyageur"
        isAnonymous = intent.getBooleanExtra("IS_ANONYMOUS", true)

        setupListeners()
        loadProfileInfos()
        loadPosts(showFavoris = false) // Par défaut : Mes publications
    }

    private fun loadProfileInfos() {
        binding.tvProfileName.text = userName
        binding.tvProfileInitials.text = userName.firstOrNull()?.uppercase() ?: "V"

        val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)

        if (isAnonymous) {
            binding.tvProfileEmail.text = "Mode découverte (Non connecté)"
        } else {
            val realEmail = dbHelper.getEmailByName(userName)
            binding.tvProfileEmail.text = realEmail ?: "$userName @travelshare"
        }

        val allPosts = dbHelper.recupererTousLesPosts()
        val userPosts = allPosts.filter { it.autheur.nomComplet == userName }

        binding.tvPhotosCount.text = userPosts.size.toString()

        val totalLikes = userPosts.sumOf { it.likesCount }
        binding.tvLikesCount.text = totalLikes.toString()

        binding.tvFollowersCount.text = dbHelper.getNbFollowers(userName).toString()
        binding.tvFollowingCount.text = dbHelper.getNbFollowing(userName).toString()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnTabPhotos.setOnClickListener {
            updateTabStyles(0)
            loadPosts(showFavoris = false)
        }

        binding.btnTabLikes.setOnClickListener {
            updateTabStyles(1)
            loadPosts(showFavoris = true)
        }

        binding.btnTabItineraries.setOnClickListener {
            updateTabStyles(2)
            loadItineraries()
        }

        binding.btnGroupes.setOnClickListener {
            val intent = Intent(this, GroupsActivity::class.java)
            intent.putExtra("USER_NAME", userName)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            // Revenir à l'écran de connexion de TravelPath
            val intent = Intent(this, com.traveling.app.travelpath.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateTabStyles(tabIndex: Int) {
        val activeBg = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6C63FF"))
        val activeColor = android.graphics.Color.WHITE
        val inactiveBg = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        val inactiveColor = android.graphics.Color.parseColor("#A0A0C0")

        binding.btnTabPhotos.setBackgroundTintList(if (tabIndex == 0) activeBg else inactiveBg)
        binding.btnTabPhotos.setTextColor(if (tabIndex == 0) activeColor else inactiveColor)
        
        binding.btnTabLikes.setBackgroundTintList(if (tabIndex == 1) activeBg else inactiveBg)
        binding.btnTabLikes.setTextColor(if (tabIndex == 1) activeColor else inactiveColor)

        binding.btnTabItineraries.setBackgroundTintList(if (tabIndex == 2) activeBg else inactiveBg)
        binding.btnTabItineraries.setTextColor(if (tabIndex == 2) activeColor else inactiveColor)
    }

    private fun loadItineraries() {
        binding.rvProfilePhotos.visibility = android.view.View.GONE
        
        val prefs = getSharedPreferences("saved_itineraries", android.content.Context.MODE_PRIVATE)
        val existing = prefs.getString("itineraries_list", "[]") ?: "[]"
        
        try {
            val jsonArray = org.json.JSONArray(existing)
            val listItems = mutableListOf<String>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val dest = obj.optString("destination", "Inconnu")
                val budget = obj.optInt("totalBudget", 0)
                val hours = obj.optInt("totalHours", 0)
                listItems.add("🗺️ $dest - $budget € - ${hours}h")
            }
            
            if (listItems.isEmpty()) {
                binding.lvProfileItineraries.visibility = android.view.View.GONE
                binding.tvEmptyProfile.visibility = android.view.View.VISIBLE
                binding.tvEmptyProfile.text = "Aucun itinéraire sauvegardé."
            } else {
                binding.lvProfileItineraries.visibility = android.view.View.VISIBLE
                binding.tvEmptyProfile.visibility = android.view.View.GONE
                
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, listItems)
                binding.lvProfileItineraries.adapter = adapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPosts(showFavoris: Boolean) {
        binding.lvProfileItineraries.visibility = android.view.View.GONE

        val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)
        
        val displayPosts = if (showFavoris) {
            dbHelper.recupererPostsLikesPar(userName)
        } else {
            val allPosts = dbHelper.recupererTousLesPosts()
            allPosts.filter { it.autheur.nomComplet == userName }
        }

        if (displayPosts.isEmpty()) {
            binding.rvProfilePhotos.visibility = android.view.View.GONE
            binding.tvEmptyProfile.visibility = android.view.View.VISIBLE
            binding.tvEmptyProfile.text = if (showFavoris) "Aucun coup de cœur pour le moment." else "Aucune publication pour le moment."
        } else {
            binding.rvProfilePhotos.visibility = android.view.View.VISIBLE
            binding.tvEmptyProfile.visibility = android.view.View.GONE
            
            val adapter = PhotoPostAdapter(
                posts = displayPosts,
                onLikeClicked = { },
                onCommentClicked = { },
                onPostClicked = { post ->
                    val intent = Intent(this, PhotoDetailActivity::class.java)
                    intent.putExtra("POST_ID", post.id)
                    intent.putExtra("IS_ANONYMOUS", isAnonymous)
                    intent.putExtra("AUTHOR_NAME", post.autheur.nomComplet)
                    intent.putExtra("LOCATION", post.lieuNom)
                    intent.putExtra("DESCRIPTION", post.descriptionText)
                    intent.putExtra("LIKES", post.likesCount)
                    intent.putExtra("COMMENTS", post.commentsCount)
                    intent.putExtra("PHOTO_URL", post.photoUrl)
                    intent.putExtra("LATITUDE", post.latitude)
                    intent.putExtra("LONGITUDE", post.longitude)
                    intent.putExtra("CURRENT_USER_NAME", userName)
                    startActivity(intent)
                }
            )
            binding.rvProfilePhotos.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            binding.rvProfilePhotos.adapter = adapter
        }
    }
}
