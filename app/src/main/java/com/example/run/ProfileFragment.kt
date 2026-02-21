package com.example.run

import UserResponse
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.net.Uri
import androidx.cardview.widget.CardView

class ProfileFragment : Fragment() {

    private lateinit var apiInterface: ApiInterface

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvHeight: TextView
    private lateinit var tvWeight: TextView
    private lateinit var tvAge: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // 🔥 Bind Views from XML
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        tvHeight = view.findViewById(R.id.tvHeight)
        tvWeight = view.findViewById(R.id.tvWeight)
        tvAge = view.findViewById(R.id.tvAge)

        initRetrofit()

        // ✅ LOAD FROM CACHE FIRST
        loadUserProfile()

        // notification section intent
        val cardNotification = view.findViewById<CardView>(R.id.cardNotifications)
        cardNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }

        // Help and support intent
        val cardHelp = view.findViewById<CardView>(R.id.cardHelp)
        cardHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("dhokaneprasad6@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "CaloriX App Support")
                putExtra(Intent.EXTRA_TEXT, "Hello Prasad,\n\nI need help with CaloriX app.")
            }
            startActivity(intent)
        }

        // about intent
        val cardAbout = view.findViewById<CardView>(R.id.cardAbout)
        cardAbout.setOnClickListener {
            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)
        }

        //logout
        val btnLogout = view.findViewById<CardView>(R.id.btnLogout) // Add this button to your XML

        btnLogout.setOnClickListener {
            logoutUser()
        }

        return view
    }

    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    // 🚀 SMART LOADING: Cache First, API as Fallback
    private fun loadUserProfile() {
        val sharedPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

        val isCached = sharedPref.getBoolean("profile_cached", false)

        if (isCached) {
            // ✅ LOAD FROM CACHE (INSTANT, NO API CALL)
            loadFromCache()
        } else {
            // ❌ CACHE EMPTY, FETCH FROM API
            fetchFromAPI()
        }
    }

    // 💾 LOAD DATA FROM SHARED PREFERENCES (INSTANT)
    private fun loadFromCache() {
        val sharedPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

        val name = sharedPref.getString("name", "N/A") ?: "N/A"
        val email = sharedPref.getString("email", "N/A") ?: "N/A"
        val height = sharedPref.getString("height", "0") ?: "0"
        val weight = sharedPref.getString("weight", "0") ?: "0"
        val age = sharedPref.getString("age", "0") ?: "0"
        val createdAt = sharedPref.getString("created_at", "") ?: ""

        tvUserName.text = name
        tvUserEmail.text = email
        tvHeight.text = "$height cm"
        tvWeight.text = "$weight kg"
        tvAge.text = "$age years"
        tvMemberSince.text = "Member since ${formatDate(createdAt)}"
    }

    // 🌐 FETCH FROM API (ONLY IF CACHE IS EMPTY)
    private fun fetchFromAPI() {
        val sharedPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

        val email = sharedPref.getString("email", null)

        if (email == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val call = apiInterface.getUser(email)

        call.enqueue(object : Callback<UserResponse> {
            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.data

                    // 💾 SAVE TO CACHE
                    sharedPref.edit().apply {
                        putString("name", user.name)
                        putString("email", user.email)
                        putString("height", user.height.toString())
                        putString("weight", user.weight.toString())
                        putString("age", user.age.toString())
                        putString("created_at", user.created_at)
                        putBoolean("profile_cached", true)
                        apply()
                    }

                    // 📱 DISPLAY DATA
                    loadFromCache()

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load user profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(
                    requireContext(),
                    "Network Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // 📅 Convert backend datetime → "Feb 2026"
    private fun formatDate(dateStr: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val date = input.parse(dateStr)
            output.format(date!!)
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun logoutUser() {
        // Clear all cached data
        val sharedPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

        sharedPref.edit().clear().apply()

        // Navigate to SignIn
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }
}