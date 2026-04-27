package com.traveling.app.travelshare.ui

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
import com.traveling.app.travelshare.models.PhotoPost

class MapFeedActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapFeedBinding
    private var mMap: GoogleMap? = null

    // Simuler quelques données partagées depuis le flux
    private val fakePhotoLocations = listOf(
        LatLng(48.8566, 2.3522),  // Paris
        LatLng(-8.4095, 115.1889), // Bali
        LatLng(45.9237, 6.8694)   // Chamonix
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init la carte
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFeed) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSwitchToFeed.setOnClickListener {
            // Retour au feed Liste/Grille
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Placer les photos sur la carte sous forme de Pins
        for ((index, latLng) in fakePhotoLocations.withIndex()) {
            mMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Photo de voyage #\${index + 1}")
                    .snippet("Cliquez pour voir la photo...")
            )
        }

        // Action quand on clique sur le résumé d'un Pin
        mMap?.setOnInfoWindowClickListener { marker ->
            Toast.makeText(this, "Ouverture du détail de \${marker.title}", Toast.LENGTH_SHORT).show()
        }

        // Focus caméra (Global)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(fakePhotoLocations[0], 2f))
    }
}
