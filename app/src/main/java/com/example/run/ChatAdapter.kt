package com.example.run

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_BOT = 0
        private const val VIEW_TYPE_USER = 1

        // Move formatTime to companion object as @JvmStatic
        @JvmStatic
        fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
                else -> {
                    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isBot) VIEW_TYPE_BOT else VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_BOT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_bot, parent, false)
            BotMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message_user, parent, false)
            UserMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is BotMessageViewHolder) {
            holder.bind(message)
        } else if (holder is UserMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvBotMessage)
        private val timeText: TextView = itemView.findViewById(R.id.tvBotTime)

        fun bind(message: ChatMessage) {
            messageText.text = message.message
            timeText.text = formatTime(message.timestamp)  // Now works!
        }
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val timeText: TextView = itemView.findViewById(R.id.tvUserTime)

        fun bind(message: ChatMessage) {
            messageText.text = message.message
            timeText.text = formatTime(message.timestamp)  // Now works!
        }
    }
}