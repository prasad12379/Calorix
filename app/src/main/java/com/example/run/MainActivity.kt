package com.example.run

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.run.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // ✅ Handle system navigation bar padding (VERY IMPORTANT)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navBarInsets.bottom + 8   // adds space above system nav
            )
            insets
        }

        // Default fragment
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navHome -> loadFragment(HomeFragment())
                R.id.navProfile -> loadFragment(ProfileFragment())
                R.id.navActivity -> loadFragment(ActivityFragment())
                R.id.navProgress -> loadFragment(ProgressFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
