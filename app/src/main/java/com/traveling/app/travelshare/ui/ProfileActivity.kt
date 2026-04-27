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
        
        if (isAnonymous) {
            binding.tvProfileEmail.text = "Mode découverte (Non connecté)"
        } else {
            binding.tvProfileEmail.text = "${userName.lowercase().replace(" ",".")}@travelshare.com"
        }

        val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)
        val allPosts = dbHelper.recupererTousLesPosts()
        val userPosts = allPosts.filter { it.autheur.nomComplet == userName }

        binding.tvPhotosCount.text = userPosts.size.toString()
        
        val totalLikes = userPosts.sumOf { it.likesCount }
        binding.tvLikesCount.text = totalLikes.toString()

        binding.tvFollowersCount.text = (userPosts.size * 5 + 12).toString() 
        binding.tvFollowingCount.text = "18"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnTabPhotos.setOnClickListener {
            updateTabStyles(showFavoris = false)
            loadPosts(showFavoris = false)
        }

        binding.btnTabLikes.setOnClickListener {
            updateTabStyles(showFavoris = true)
            loadPosts(showFavoris = true)
        }

        binding.btnGroupes.setOnClickListener {
            Toast.makeText(this, "Vos cercles de voyage (Bientôt disponible)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            // Revenir à l'écran de connexion de TravelPath
            val intent = Intent(this, com.traveling.app.travelpath.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateTabStyles(showFavoris: Boolean) {
        if (showFavoris) {
            binding.btnTabLikes.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6C63FF")))
            binding.btnTabLikes.setTextColor(android.graphics.Color.WHITE)
            binding.btnTabPhotos.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
            binding.btnTabPhotos.setTextColor(android.graphics.Color.parseColor("#A0A0C0"))
        } else {
            binding.btnTabPhotos.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#6C63FF")))
            binding.btnTabPhotos.setTextColor(android.graphics.Color.WHITE)
            binding.btnTabLikes.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT))
            binding.btnTabLikes.setTextColor(android.graphics.Color.parseColor("#A0A0C0"))
        }
    }

    private fun loadPosts(showFavoris: Boolean) {
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
