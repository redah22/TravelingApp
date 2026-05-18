package com.traveling.app.travelshare.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.traveling.app.R
import com.traveling.app.databinding.ActivityMapFeedBinding
import com.traveling.app.travelshare.data.DatabaseHelper
import com.traveling.app.travelshare.models.PhotoPost

class MapFeedActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapFeedBinding
    private var mMap: GoogleMap? = null
    private var postsList: List<PhotoPost> = emptyList()
    private var isAnonymous = true
    private var currentUserName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isAnonymous = intent.getBooleanExtra("IS_ANONYMOUS", true)
        currentUserName = intent.getStringExtra("CURRENT_USER_NAME") ?: ""

        // Charger les vrais posts depuis la BDD
        postsList = DatabaseHelper.getInstance(this)
            .recupererPostsPublics()
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFeed) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSwitchToFeed.setOnClickListener { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (postsList.isEmpty()) {
            Toast.makeText(this, "Aucune publication géolocalisée pour le moment.", Toast.LENGTH_SHORT).show()
            return
        }

        for (post in postsList) {
            val latLng = LatLng(post.latitude, post.longitude)
            mMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(post.autheur.nomComplet)
                    .snippet(post.lieuNom)
            )?.tag = post.id
        }

        // Click sur un pin → ouvrir le détail du post
        mMap?.setOnInfoWindowClickListener { marker ->
            val postId = marker.tag as? String ?: return@setOnInfoWindowClickListener
            val post = postsList.find { it.id == postId } ?: return@setOnInfoWindowClickListener
            val intent = Intent(this, PhotoDetailActivity::class.java).apply {
                putExtra("POST_ID", post.id)
                putExtra("IS_ANONYMOUS", isAnonymous)
                putExtra("AUTHOR_NAME", post.autheur.nomComplet)
                putExtra("LOCATION", post.lieuNom)
                putExtra("DESCRIPTION", post.descriptionText)
                putExtra("LIKES", post.likesCount)
                putExtra("COMMENTS", post.commentsCount)
                putExtra("PHOTO_URL", post.photoUrl)
                putExtra("LATITUDE", post.latitude)
                putExtra("LONGITUDE", post.longitude)
                putExtra("CURRENT_USER_NAME", currentUserName)
            }
            startActivity(intent)
        }

        // Centrer la carte sur le premier post
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(postsList[0].latitude, postsList[0].longitude), 5f
        ))
    }
}
