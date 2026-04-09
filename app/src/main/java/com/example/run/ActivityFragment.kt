package com.example.run

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class ActivityFragment : Fragment() {

    private lateinit var rvActivities: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var tvTotalActivities: TextView

    private lateinit var chipAll: CardView
    private lateinit var chipRunning: CardView
    private lateinit var chipWalking: CardView
    private lateinit var chipCycling: CardView

    private lateinit var apiInterface: ApiInterface
    private lateinit var adapter: ActivityAdapter

    private var fullList = mutableListOf<ActivityItem>()
    private var currentFilter = "ALL"

    // ── Color palette from ChatbotFragment ───────────────────────────────────
    private val colorBgWhite       = "#FAF9FF"   // BgWhite  — screen background
    private val colorBgLavender    = "#ECE8F5"   // BgLavender — subtle surface
    private val colorDeepBlack     = "#0A0A0A"   // DeepBlack — header / user bubble / send btn
    private val colorPureWhite     = "#FFFFFF"   // PureWhite — card / bot bubble bg
    private val colorAccentViolet  = "#9B8FD4"   // AccentViolet — selected chip accent
    private val colorHoloPink      = "#E8B4D8"   // HoloPink — decorative blob
    private val colorHoloMint      = "#AEE8D8"   // HoloMint — online dot / decorative
    private val colorSubtleGrey    = "#DDD8EE"   // SubtleGrey — borders / dividers
    private val colorTextPrimary   = "#0A0A0A"   // TextPrimary — main labels
    private val colorTextSecondary = "#7A7490"   // TextSecondary — muted / count text
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_activity, container, false)

        initViews(view)
        initRetrofit()
        setupRecycler()
        loadActivities()

        return view
    }

    private fun initViews(view: View) {

        rvActivities      = view.findViewById(R.id.rvActivities)
        emptyState        = view.findViewById(R.id.emptyState)
        tvTotalActivities = view.findViewById(R.id.tvTotalActivities)

        chipAll     = view.findViewById(R.id.chipAll)
        chipRunning = view.findViewById(R.id.chipRunning)
        chipWalking = view.findViewById(R.id.chipWalking)
        chipCycling = view.findViewById(R.id.chipCycling)

        // Apply BgWhite background to root view
        view.setBackgroundColor(Color.parseColor(colorBgWhite))

        // Style the total-activities label with TextSecondary colour
        tvTotalActivities.setTextColor(Color.parseColor(colorTextSecondary))

        // Reset chips to unselected state, then mark default
        resetChipColors()
        selectFilter("ALL")

        chipAll.setOnClickListener {
            selectFilter("ALL")
            filterList("ALL")
        }
        chipRunning.setOnClickListener {
            selectFilter("RUNNING")
            filterList("RUNNING")
        }
        chipWalking.setOnClickListener {
            selectFilter("WALKING")
            filterList("WALKING")
        }
        chipCycling.setOnClickListener {
            selectFilter("CYCLING")
            filterList("CYCLING")
        }

        // "Start First Workout" button — DeepBlack background (mirrors send button)
        view.findViewById<CardView>(R.id.btnStartFirstWorkout)?.apply {
            setCardBackgroundColor(Color.parseColor(colorDeepBlack))
            setOnClickListener {
                Toast.makeText(requireContext(), "Start workout from Home tab", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initRetrofit() {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://running-app-backend-p48y.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiInterface = retrofit.create(ApiInterface::class.java)
    }

    private fun setupRecycler() {
        adapter = ActivityAdapter(mutableListOf())
        rvActivities.layoutManager = LinearLayoutManager(requireContext())
        rvActivities.adapter = adapter
    }

    // 🔥 LOAD ACTIVITIES FROM BACKEND
    private fun loadActivities() {

        val sharedPref = requireContext()
            .getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE)

        val email = sharedPref.getString("email", null)

        if (email == null) {
            showEmptyState()
            Toast.makeText(requireContext(), "Please sign in to view activities", Toast.LENGTH_SHORT).show()
            return
        }

        val call = apiInterface.getActivities(email)

        call.enqueue(object : Callback<ActivityListResponse> {

            override fun onResponse(
                call: Call<ActivityListResponse>,
                response: Response<ActivityListResponse>
            ) {

                if (response.isSuccessful && response.body() != null) {

                    fullList = response.body()!!.data.toMutableList()
                    tvTotalActivities.text = "${fullList.size} total workouts"

                    if (fullList.isEmpty()) {
                        showEmptyState()
                    } else {
                        filterList(currentFilter)
                    }

                } else {
                    showEmptyState()
                    Toast.makeText(
                        requireContext(),
                        "Failed to load activities: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ActivityListResponse>, t: Throwable) {
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ✅ FILTER LOGIC — unchanged
    private fun filterList(type: String) {

        currentFilter = type

        if (type == "ALL") {
            showRecycler(fullList)
            return
        }

        val filtered = fullList.filter {
            it.workout_mode.equals(type, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            rvActivities.visibility = View.GONE
            emptyState.visibility   = View.VISIBLE
        } else {
            showRecycler(filtered)
        }
    }

    // 🎨 UPDATE FILTER CHIP UI — Chatbot colour scheme
    private fun selectFilter(type: String) {
        resetChipColors()   // all chips → unselected (BgLavender)

        // Selected chip: AccentViolet background (mirrors the Lavender/Violet accent)
        when (type) {
            "ALL"     -> chipAll.setCardBackgroundColor(Color.parseColor(colorAccentViolet))
            "RUNNING" -> chipRunning.setCardBackgroundColor(Color.parseColor(colorAccentViolet))
            "WALKING" -> chipWalking.setCardBackgroundColor(Color.parseColor(colorAccentViolet))
            "CYCLING" -> chipCycling.setCardBackgroundColor(Color.parseColor(colorAccentViolet))
        }
    }

    /** Reset every chip to SubtleGrey — the unselected surface used in chatbot borders/cards */
    private fun resetChipColors() {
        val unselected = Color.parseColor(colorSubtleGrey)
        chipAll.setCardBackgroundColor(unselected)
        chipRunning.setCardBackgroundColor(unselected)
        chipWalking.setCardBackgroundColor(unselected)
        chipCycling.setCardBackgroundColor(unselected)
    }

    private fun showRecycler(list: List<ActivityItem>) {
        rvActivities.visibility = View.VISIBLE
        emptyState.visibility   = View.GONE
        adapter.updateData(list)
    }

    private fun showEmptyState() {
        rvActivities.visibility = View.GONE
        emptyState.visibility   = View.VISIBLE
    }
}