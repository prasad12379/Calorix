package com.example.run

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ActivityAdapter(private var activities: MutableList<ActivityItem>) :
    RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_card, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount() = activities.size

    fun updateData(newList: List<ActivityItem>) {
        activities.clear()
        activities.addAll(newList)
        notifyDataSetChanged()
    }

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cvActivityIcon: CardView = itemView.findViewById(R.id.cvActivityIcon)
        private val ivActivityIcon: ImageView = itemView.findViewById(R.id.ivActivityIcon)
        private val tvActivityType: TextView = itemView.findViewById(R.id.tvActivityType)
        private val tvActivityDate: TextView = itemView.findViewById(R.id.tvActivityDate)
        private val tvActivityDuration: TextView = itemView.findViewById(R.id.tvActivityDuration)
        private val tvActivityDistance: TextView = itemView.findViewById(R.id.tvActivityDistance)
        private val tvActivityPace: TextView = itemView.findViewById(R.id.tvActivityPace)
        private val tvActivityCalories: TextView = itemView.findViewById(R.id.tvActivityCalories)
        private val tvBestPace: TextView = itemView.findViewById(R.id.tvBestPace)
        private val tvSteps: TextView = itemView.findViewById(R.id.tvSteps)

        private val btnViewDetails: CardView = itemView.findViewById(R.id.btnViewDetails)
        private val btnShare: CardView = itemView.findViewById(R.id.btnShare)
        private val btnDelete: CardView = itemView.findViewById(R.id.btnDelete)

        fun bind(activity: ActivityItem) {
            // Set workout type and icon
            when (activity.workout_mode.uppercase()) {
                "RUNNING" -> {
                    tvActivityType.text = "Running"
                    ivActivityIcon.setImageResource(R.drawable.ic_running)
                    cvActivityIcon.setCardBackgroundColor(Color.parseColor("#4CAF50"))
                }
                "WALKING" -> {
                    tvActivityType.text = "Walking"
                    ivActivityIcon.setImageResource(R.drawable.ic_walking)
                    cvActivityIcon.setCardBackgroundColor(Color.parseColor("#2196F3"))
                }
                "CYCLING" -> {
                    tvActivityType.text = "Cycling"
                    ivActivityIcon.setImageResource(R.drawable.ic_cycling)
                    cvActivityIcon.setCardBackgroundColor(Color.parseColor("#FF9800"))
                }
                else -> {
                    tvActivityType.text = activity.workout_mode
                    ivActivityIcon.setImageResource(R.drawable.ic_running)
                    cvActivityIcon.setCardBackgroundColor(Color.parseColor("#9E9E9E"))
                }
            }

            // Set date and time
            tvActivityDate.text = activity.date

            // Set duration
            tvActivityDuration.text = activity.duration

            // Set distance
            tvActivityDistance.text = activity.distance

            // Set pace
            tvActivityPace.text = activity.pace

            // Set calories
            tvActivityCalories.text = activity.calories

            // Set best pace
            tvBestPace.text = activity.best_pace

            // Set steps with formatting
            tvSteps.text = formatNumber(activity.steps)

            // Click listeners
            btnViewDetails.setOnClickListener {
                // TODO: Show workout details
            }

            btnShare.setOnClickListener {
                // TODO: Share workout
            }

            btnDelete.setOnClickListener {
                // TODO: Delete workout
            }
        }

        private fun formatNumber(number: Int): String {
            return String.format("%,d", number)
        }
    }
}