package com.example.run

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
import UserResponse
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
        loadUserProfile()

        //about section intent
        val cardNotification = view.findViewById<CardView>(R.id.cardNotifications)

        cardNotification.setOnClickListener {

            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)

        }

        //help and support intent
        val cardHelp = view.findViewById<CardView>(R.id.cardHelp)

        cardHelp.setOnClickListener {

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // only email apps open
                putExtra(Intent.EXTRA_EMAIL, arrayOf("dhokaneprasad6@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "CaloriX App Support")
                putExtra(Intent.EXTRA_TEXT, "Hello Prasad,\n\nI need help with CaloriX app.")
            }

            startActivity(intent)
        }

        //notification intent
        val cardAbout = view.findViewById<CardView>(R.id.cardAbout)

        cardAbout.setOnClickListener {

            val intent = Intent(requireContext(), AboutActivity::class.java)
            startActivity(intent)

        }

        return view
    }

    // ✅ SAME BASE URL AS YOUR OTHER SCREENS
    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    // 🚀 CALL BACKEND USING STORED EMAIL
    private fun loadUserProfile() {

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

                    tvUserName.text = user.name
                    tvUserEmail.text = user.email
                    tvHeight.text = "${user.height} cm"
                    tvWeight.text = "${user.weight} kg"
                    tvAge.text = "${user.age} years"

                    tvMemberSince.text =
                        "Member since ${formatDate(user.created_at)}"

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
}