package com.example.run
import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout  // ← new
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        // ✅ NEW — start neon glow pulse on the wrapper
        val glowWrapper = findViewById<FrameLayout>(R.id.glowWrapper)
        glowWrapper.startNeonPulse()
        // ✅ Handle system navigation bar padding
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navBarInsets.bottom + 8
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