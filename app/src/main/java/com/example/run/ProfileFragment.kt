package com.example.run

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import UserResponse

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

        // 🔥 Bind XML Views
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvMemberSince = view.findViewById(R.id.tvMemberSince)
        tvHeight = view.findViewById(R.id.tvHeight)
        tvWeight = view.findViewById(R.id.tvWeight)
        tvAge = view.findViewById(R.id.tvAge)

        initRetrofit()

        // ✅ FIRST TRY LOADING FROM CACHE
        if (!loadProfileFromCache()) {
            loadUserProfile()
        }

        return view
    }

    // 🔥 Retrofit Init (same base URL)
    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    // ✅ LOAD PROFILE FROM LOCAL CACHE
    private fun loadProfileFromCache(): Boolean {

        val pref = requireContext()
            .getSharedPreferences("USER_PROFILE", Context.MODE_PRIVATE)

        val name = pref.getString("name", null)

        // ❌ No cache found → call API
        if (name == null) return false

        // ✅ Set UI from cache
        tvUserName.text = name
        tvUserEmail.text = pref.getString("email", "")
        tvHeight.text = "${pref.getInt("height", 0)} cm"
        tvWeight.text = "${pref.getInt("weight", 0)} kg"
        tvAge.text = "${pref.getInt("age", 0)} years"
        tvMemberSince.text = pref.getString("member_since", "")

        return true
    }

    // 🚀 CALL FASTAPI USING STORED EMAIL
    private fun loadUserProfile() {

        val sessionPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

        val email = sessionPref.getString("email", null)

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

                    // ✅ Update UI
                    tvUserName.text = user.name
                    tvUserEmail.text = user.email
                    tvHeight.text = "${user.height} cm"
                    tvWeight.text = "${user.weight} kg"
                    tvAge.text = "${user.age} years"

                    val memberSince =
                        "Member since ${formatDate(user.created_at)}"

                    tvMemberSince.text = memberSince

                    // 🔥 SAVE TO CACHE (VERY IMPORTANT)
                    val pref = requireContext()
                        .getSharedPreferences("USER_PROFILE", Context.MODE_PRIVATE)

                    pref.edit().apply {
                        putString("name", user.name)
                        putString("email", user.email)
                        putInt("height", user.height)
                        putInt("weight", user.weight)
                        putInt("age", user.age)
                        putString("member_since", memberSince)
                        apply()
                    }

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

    // 📅 Format Date → Feb 2026
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
}
