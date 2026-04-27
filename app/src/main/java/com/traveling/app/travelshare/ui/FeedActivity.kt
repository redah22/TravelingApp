package com.traveling.app.travelshare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.traveling.app.databinding.ActivityFeedBinding
import com.traveling.app.travelshare.models.PhotoPost
import com.traveling.app.travelshare.models.ShareScope
import com.traveling.app.travelshare.models.User

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private var isAnonymous = true
    private var userName = "Voyageur"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAnonymous = intent.getBooleanExtra("IS_ANONYMOUS", true)
        userName = intent.getStringExtra("USER_NAME") ?: "Voyageur"

        setupUI()
        setupFeed()
    }

    private fun setupUI() {
        if (isAnonymous) {
            binding.bannerAnonymous.visibility = View.VISIBLE
            binding.fabPublish.visibility = View.GONE
            binding.btnProfile.visibility = View.GONE
            binding.btnLoginFeed.visibility = View.VISIBLE

            binding.btnSignupBanner.setOnClickListener {
                finish() // Retour au main activity
            }
            binding.btnLoginBanner.setOnClickListener {
                finish() // Retour au main activity
            }
        } else {
            binding.bannerAnonymous.visibility = View.GONE
            binding.fabPublish.visibility = View.VISIBLE
            binding.btnProfile.visibility = View.VISIBLE
            binding.btnLoginFeed.visibility = View.GONE

            binding.btnProfile.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("IS_ANONYMOUS", isAnonymous)
                intent.putExtra("USER_NAME", userName)
                startActivity(intent)
            }

            binding.fabPublish.setOnClickListener {
                publishLauncher.launch(Intent(this, PublishActivity::class.java))
            }
        }

        binding.btnSearch.setOnClickListener {
            Toast.makeText(this, "Recherche (Lieu, auteur, tag...)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLoginFeed.setOnClickListener {
            finish() // Retour au menu principal
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private val publishLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val description = result.data?.getStringExtra("DESCRIPTION") ?: ""
            val imageUri = result.data?.getStringExtra("IMAGE_URI") ?: ""
            val visibilityStr = result.data?.getStringExtra("VISIBILITY") ?: "PUBLIC"
            
            val scope = try {
                ShareScope.valueOf(visibilityStr)
            } catch (e: Exception) {
                ShareScope.PUBLIC
            }

            if (description.isNotEmpty()) {
                val newPost = PhotoPost(
                    id = "p${System.currentTimeMillis()}",
                    autheur = User("u_me", userName, "@me", ""),
                    photoUrl = imageUri,
                    descriptionText = description,
                    lieuNom = "Localisation actuelle",
                    latitude = 0.0,
                    longitude = 0.0,
                    datePublicationMillis = System.currentTimeMillis(),
                    scope = scope
                )
                
                com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this).insererPost(newPost)
                
                postsList.add(0, newPost)
                adapter.notifyItemInserted(0)
                binding.rvPhotoFeed.scrollToPosition(0)
            }
        }
    }

    private var postsList = mutableListOf<PhotoPost>()
    private lateinit var adapter: PhotoPostAdapter

    private fun setupFeed() {
        val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)
        var savedPosts = dbHelper.recupererTousLesPosts()

        if (savedPosts.isEmpty()) {
            val mockPhotos = generateMockPhotos()
            mockPhotos.forEach { dbHelper.insererPost(it) }
            savedPosts = dbHelper.recupererTousLesPosts()
        }

        postsList.clear()
        postsList.addAll(savedPosts)

        adapter = PhotoPostAdapter(
            posts = postsList,
            onLikeClicked = { post ->
                if (isAnonymous) {
                    Toast.makeText(this, "Connectez-vous pour liker.", Toast.LENGTH_SHORT).show()
                } else {
                    val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)
                    val wasLiked = dbHelper.toggleLike(userName, post.id)
                    val action = if (wasLiked) "aimé" else "retiré"
                    Toast.makeText(this, "❤️ Vous avez $action la photo de ${post.autheur.nomComplet} !", Toast.LENGTH_SHORT).show()
                    
                    // Rafraîchir les données pour mettre à jour le compteur
                    val updatedPosts = dbHelper.recupererTousLesPosts()
                    postsList.clear()
                    postsList.addAll(updatedPosts)
                    adapter.notifyDataSetChanged()
                }
            },
            onCommentClicked = { post ->
                if (isAnonymous) {
                    Toast.makeText(this, "Connectez-vous pour commenter.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Commenter la photo de ${post.autheur.nomComplet}...", Toast.LENGTH_SHORT).show()
                }
            },
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
        binding.rvPhotoFeed.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvPhotoFeed.adapter = adapter
    }

    private fun generateMockPhotos(): List<PhotoPost> {
        return listOf(
            PhotoPost(
                id = "p1",
                autheur = User("u1", "Sophie Martin", "@sophie_m", ""),
                photoUrl = "",
                descriptionText = "Tour Eiffel, Paris",
                lieuNom = "Tour Eiffel, Paris",
                latitude = 48.8584,
                longitude = 2.2945,
                datePublicationMillis = System.currentTimeMillis() - 86400000,
                likesCount = 245,
                commentsCount = 18,
                scope = ShareScope.PUBLIC
            ),
            PhotoPost(
                id = "p2",
                autheur = User("u2", "Lucas Dupont", "@lucas_d", ""),
                photoUrl = "",
                descriptionText = "Coucher de soleil magique 🌅 #Tokyo",
                lieuNom = "Shibuya, Tokyo",
                latitude = 35.6598,
                longitude = 139.7006,
                datePublicationMillis = System.currentTimeMillis() - 172800000,
                likesCount = 312,
                commentsCount = 34,
                scope = ShareScope.PUBLIC
            ),
            PhotoPost(
                id = "p3",
                autheur = User("u3", "Emma Lefebvre", "@emma_voyage", ""),
                photoUrl = "",
                descriptionText = "Les ruines de Machu Picchu, inoubliable 🏔️ #Pérou",
                lieuNom = "Machu Picchu, Pérou",
                latitude = -13.1631,
                longitude = -72.5450,
                datePublicationMillis = System.currentTimeMillis() - 259200000,
                likesCount = 589,
                commentsCount = 62,
                scope = ShareScope.PUBLIC
            )
        )
    }
}
