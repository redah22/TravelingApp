package com.traveling.app.travelshare.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.traveling.app.R
import com.traveling.app.databinding.ActivityPhotoDetailBinding
import com.traveling.app.travelshare.data.Commentaire
import com.traveling.app.travelshare.data.DatabaseHelper

class PhotoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding
    private var isAnonymous = true
    private var postId = ""
    private var currentUserName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAnonymous = intent.getBooleanExtra("IS_ANONYMOUS", true)
        postId = intent.getStringExtra("POST_ID") ?: ""
        currentUserName = intent.getStringExtra("CURRENT_USER_NAME") ?: ""

        setupUI()
        loadPost()
        loadComments()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        if (isAnonymous) {
            binding.llAddComment.visibility = View.GONE
        }

        binding.btnLikeDetail.setOnClickListener {
            if (isAnonymous) {
                Toast.makeText(this, "Connectez-vous pour liker.", Toast.LENGTH_SHORT).show()
            } else {
                val db = DatabaseHelper.getInstance(this)
                db.toggleLike(currentUserName, postId)
                loadPost() // Rafraîchir
            }
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                val db = DatabaseHelper.getInstance(this)
                db.insererCommentaire(postId, currentUserName, text)
                binding.etComment.setText("")
                loadComments()
                loadPost() // Pour mettre à jour le compteur
                Toast.makeText(this, "Commentaire ajouté !", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnReportDetail.setOnClickListener {
            Toast.makeText(this, "Signalement envoyé.", Toast.LENGTH_SHORT).show()
        }

        val latitude = intent.getDoubleExtra("LATITUDE", 48.8584)
        val longitude = intent.getDoubleExtra("LONGITUDE", 2.2945)

        binding.btnNavigate.setOnClickListener { navigateToLocation(latitude, longitude) }

        val authorName = intent.getStringExtra("AUTHOR_NAME") ?: ""
        binding.btnSubscribeAuthor.setOnClickListener {
            if (isAnonymous) {
                Toast.makeText(this, "Connectez-vous pour vous abonner.", Toast.LENGTH_SHORT).show()
            } else if (currentUserName == authorName) {
                Toast.makeText(this, "Vous ne pouvez pas vous abonner à vous-même.", Toast.LENGTH_SHORT).show()
            } else {
                val db = DatabaseHelper.getInstance(this)
                if (db.estAbonne(currentUserName, authorName)) {
                    db.seDesabonner(currentUserName, authorName)
                    binding.btnSubscribeAuthor.text = "S'abonner"
                    Toast.makeText(this, "Désabonné de $authorName", Toast.LENGTH_SHORT).show()
                } else {
                    db.suivre(currentUserName, authorName)
                    binding.btnSubscribeAuthor.text = "✓ Abonné"
                    Toast.makeText(this, "✅ Abonné à $authorName !", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Init du label
        if (!isAnonymous && authorName.isNotEmpty() && currentUserName != authorName) {
            val db = DatabaseHelper.getInstance(this)
            if (db.estAbonne(currentUserName, authorName)) {
                binding.btnSubscribeAuthor.text = "✓ Abonné"
            }
        }
    }

    private fun loadPost() {
        val authorName = intent.getStringExtra("AUTHOR_NAME") ?: "Auteur Inconnu"
        val location = intent.getStringExtra("LOCATION") ?: "Lieu Inconnu"
        val description = intent.getStringExtra("DESCRIPTION") ?: ""
        val photoUrl = intent.getStringExtra("PHOTO_URL") ?: ""

        // Récupérer les likes/comments frais de la BDD
        val db = DatabaseHelper.getInstance(this)
        val allPosts = db.recupererTousLesPosts()
        val post = allPosts.find { it.id == postId }

        binding.tvDetailLocation.text = "📍 $location"
        binding.tvDetailDescription.text = description
        binding.tvDetailAuthor.text = authorName
        binding.tvDetailLikes.text = "${post?.likesCount ?: 0} J'aime"
        binding.tvDetailComments.text = "${post?.commentsCount ?: 0} commentaires"

        if (authorName == currentUserName) {
            binding.btnSubscribeAuthor.visibility = View.GONE
            binding.btnDeleteDetail.visibility = View.VISIBLE
        }

        binding.btnDeleteDetail.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Supprimer la publication")
                .setMessage("Es-tu sûr de vouloir supprimer cette photo et tous ses commentaires ?")
                .setPositiveButton("Supprimer") { _, _ ->
                    val db = DatabaseHelper.getInstance(this)
                    db.supprimerPost(postId)
                    Toast.makeText(this, "Publication supprimée.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        // --- Utilisation de GLIDE ---
        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(binding.ivDetailPhoto)
        } else {
            binding.ivDetailPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun loadComments() {
        binding.llCommentsList.removeAllViews()
        val db = DatabaseHelper.getInstance(this)
        val comments = db.recupererCommentaires(postId)

        if (comments.isEmpty()) {
            binding.tvNoComments.visibility = View.VISIBLE
        } else {
            binding.tvNoComments.visibility = View.GONE
            val inflater = LayoutInflater.from(this)
            for (cmt in comments) {
                val view = inflater.inflate(R.layout.item_comment, binding.llCommentsList, false)
                view.findViewById<TextView>(R.id.tvCommentUser).text = cmt.userName
                view.findViewById<TextView>(R.id.tvCommentText).text = cmt.text
                binding.llCommentsList.addView(view)
            }
        }
    }

    private fun navigateToLocation(lat: Double, lng: Double) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
