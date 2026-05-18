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
        userName = intent.getStringExtra("USER_NAME")
            ?: intent.getStringExtra("CURRENT_USER_NAME")
            ?: "Voyageur"

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
            
            val tvInitial = binding.root.findViewById<android.widget.TextView>(com.traveling.app.R.id.tvFeedProfileInitials)
            if (tvInitial != null) {
                tvInitial.text = userName.firstOrNull()?.uppercase() ?: "V"
            }

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
            if (binding.etSearch.visibility == View.VISIBLE) {
                binding.etSearch.visibility = View.GONE
                binding.etSearch.text.clear()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            } else {
                binding.etSearch.visibility = View.VISIBLE
                binding.etSearch.requestFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterFeed(s?.toString()?.trim()?.lowercase() ?: "")
            }
        })

        binding.btnLoginFeed.setOnClickListener {
            finish() // Retour au menu principal
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private val publishLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            setupFeed()
        }
    }

    private var postsList = mutableListOf<PhotoPost>()
    private var currentDisplayList = mutableListOf<PhotoPost>()
    private lateinit var adapter: PhotoPostAdapter

    private fun setupFeed() {
        val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)
        // Seuls les posts PUBLIC sont visibles dans le fil public
        var savedPosts = dbHelper.recupererPostsPublics()

        if (savedPosts.isEmpty()) {
            val mockPhotos = generateMockPhotos()
            mockPhotos.forEach { dbHelper.insererPost(it) }
            savedPosts = dbHelper.recupererPostsPublics()
        }

        postsList.clear()
        postsList.addAll(savedPosts)

        currentDisplayList.clear()
        currentDisplayList.addAll(postsList)

        adapter = PhotoPostAdapter(
            posts = currentDisplayList,
            onLikeClicked = { post ->
                if (isAnonymous) {
                    Toast.makeText(this, "Connectez-vous pour liker.", Toast.LENGTH_SHORT).show()
                } else {
                    val dbHelper = com.traveling.app.travelshare.data.DatabaseHelper.getInstance(this)
                    val wasLiked = dbHelper.toggleLike(userName, post.id)
                    val action = if (wasLiked) "aimé" else "retiré"
                    Toast.makeText(this, "❤️ Vous avez $action la photo de ${post.autheur.nomComplet} !", Toast.LENGTH_SHORT).show()
                    
                    // Rafraîchir les données pour mettre à jour le compteur (publics uniquement)
                    val updatedPosts = dbHelper.recupererPostsPublics()
                    postsList.clear()
                    postsList.addAll(updatedPosts)
                    
                    // Mettre à jour la liste affichée
                    currentDisplayList.clear()
                    currentDisplayList.addAll(postsList)
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

    private fun filterFeed(query: String) {
        currentDisplayList.clear()
        if (query.isEmpty()) {
            currentDisplayList.addAll(postsList)
        } else {
            currentDisplayList.addAll(postsList.filter {
                it.lieuNom.lowercase().contains(query) ||
                it.descriptionText.lowercase().contains(query) ||
                it.autheur.nomComplet.lowercase().contains(query)
            })
        }
        adapter.notifyDataSetChanged()
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
