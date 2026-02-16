package com.example.run

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var selectedMode = "RUNNING"

    // ✅ Map variables
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ---------------- MAP SETUP ----------------
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", 0)
        )

        map = view.findViewById(R.id.map)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        locationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            map
        )

        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()

        map.overlays.add(locationOverlay)
        locationOverlay.runOnFirstFix {
            requireActivity().runOnUiThread {

                val myLoc = locationOverlay.myLocation
                if (myLoc != null) {
                    map.controller.setZoom(18.0)   // zoom level (17–19 best for running)
                    map.controller.animateTo(myLoc)
                }
            }
        }

        // ---------------- MAP SETUP END ----------------


        // ---------------- YOUR ORIGINAL CODE ----------------
        val btnRunning = view.findViewById<CardView>(R.id.btnRunning)
        val btnWalking = view.findViewById<CardView>(R.id.btnWalking)
        val btnCycling = view.findViewById<CardView>(R.id.btnCycling)
        val btnStart = view.findViewById<View>(R.id.btnStart)

        fun setGreen(card: CardView) {
            ViewCompat.setBackgroundTintList(
                card,
                ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            )
        }

        fun setDefault(card: CardView) {
            ViewCompat.setBackgroundTintList(
                card,
                ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))
            )
        }

        fun selectMode(mode: String) {
            selectedMode = mode

            setDefault(btnRunning)
            setDefault(btnWalking)
            setDefault(btnCycling)

            when (mode) {
                "RUNNING" -> setGreen(btnRunning)
                "WALKING" -> setGreen(btnWalking)
                "CYCLING" -> setGreen(btnCycling)
            }
        }

        selectMode("RUNNING")

        btnRunning.setOnClickListener { selectMode("RUNNING") }
        btnWalking.setOnClickListener { selectMode("WALKING") }
        btnCycling.setOnClickListener { selectMode("CYCLING") }

        btnStart.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutActivity::class.java)
            intent.putExtra("MODE", selectedMode)
            startActivity(intent)
        }
        // ---------------- END ORIGINAL CODE ----------------
    }

    // ✅ Fragment lifecycle for MapView
    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }
}
