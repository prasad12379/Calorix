package com.example.run

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProgressFragment : Fragment() {

    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: CardView
    private lateinit var btnVoice: CardView
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyStateContainer: View
    private lateinit var typingIndicator: LinearLayout
    private lateinit var chipSuggestion1: CardView
    private lateinit var chipSuggestion2: CardView
    private lateinit var chipSuggestion3: CardView

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var apiInterface: ApiInterface

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_progress, container, false)

        initViews(view)
        initRetrofit()
        setupRecyclerView()
        setupListeners()

        return view
    }

    private fun initViews(view: View) {
        rvChatMessages = view.findViewById(R.id.rvChatMessages)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnVoice = view.findViewById(R.id.btnVoice)
        emptyState = view.findViewById(R.id.emptyState)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        typingIndicator = view.findViewById(R.id.typingIndicator)
        chipSuggestion1 = view.findViewById(R.id.chipSuggestion1)
        chipSuggestion2 = view.findViewById(R.id.chipSuggestion2)
        chipSuggestion3 = view.findViewById(R.id.chipSuggestion3)
    }

    private fun initRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            sendMessage()
        }

        etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        btnVoice.setOnClickListener {
            Toast.makeText(requireContext(), "Voice input coming soon", Toast.LENGTH_SHORT).show()
        }

        chipSuggestion1.setOnClickListener {
            sendPredefinedMessage("How to improve my running speed?")
        }

        chipSuggestion2.setOnClickListener {
            sendPredefinedMessage("What should I eat after a workout?")
        }

        chipSuggestion3.setOnClickListener {
            sendPredefinedMessage("Create a custom workout plan for me")
        }
    }

    private fun sendMessage() {
        val userMessage = etMessage.text.toString().trim()

        if (userMessage.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        // Hide empty state, show chat
        if (emptyStateContainer.visibility == View.VISIBLE) {
            emptyStateContainer.visibility = View.GONE
            rvChatMessages.visibility = View.VISIBLE
        }

        // Add user message to chat
        val userChatMessage = ChatMessage(userMessage, isBot = false)
        chatAdapter.addMessage(userChatMessage)
        scrollToBottom()

        // Clear input
        etMessage.text.clear()

        // Show typing indicator
        showTypingIndicator()

        // Call API
        getAIResponse(userMessage)
    }

    private fun sendPredefinedMessage(message: String) {
        etMessage.setText(message)
        sendMessage()
    }

    private fun getAIResponse(userMessage: String) {
        val call = apiInterface.getFitnessResponse(userMessage)

        call.enqueue(object : Callback<MyData> {
            override fun onResponse(call: Call<MyData>, response: Response<MyData>) {
                hideTypingIndicator()

                if (response.isSuccessful && response.body() != null) {
                    val botResponse = response.body()?.response ?: "No response from server"

                    // Add bot message to chat
                    val botChatMessage = ChatMessage(botResponse, isBot = true)
                    chatAdapter.addMessage(botChatMessage)
                    scrollToBottom()

                } else {
                    // Handle error response
                    val errorMessage = ChatMessage(
                        "Sorry, I couldn't get a response. Error code: ${response.code()}",
                        isBot = true
                    )
                    chatAdapter.addMessage(errorMessage)
                    scrollToBottom()
                }
            }

            override fun onFailure(call: Call<MyData>, t: Throwable) {
                hideTypingIndicator()

                // Handle network failure
                val errorMessage = ChatMessage(
                    "Connection error: ${t.message ?: "Please check your internet connection"} 🔌",
                    isBot = true
                )
                chatAdapter.addMessage(errorMessage)
                scrollToBottom()

                t.printStackTrace()
            }
        })
    }

    private fun showTypingIndicator() {
        typingIndicator.visibility = View.VISIBLE

        // Animate typing dots
        val dot1 = typingIndicator.findViewById<View>(R.id.dot1)
        val dot2 = typingIndicator.findViewById<View>(R.id.dot2)
        val dot3 = typingIndicator.findViewById<View>(R.id.dot3)

        val anim = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        anim.duration = 500

        dot1?.postDelayed({ dot1.startAnimation(anim) }, 0)
        dot2?.postDelayed({ dot2.startAnimation(anim) }, 200)
        dot3?.postDelayed({ dot3.startAnimation(anim) }, 400)
    }

    private fun hideTypingIndicator() {
        typingIndicator.visibility = View.GONE
    }

    private fun scrollToBottom() {
        rvChatMessages.postDelayed({
            if (messages.isNotEmpty()) {
                rvChatMessages.smoothScrollToPosition(messages.size - 1)
            }
        }, 100)
    }
}