package com.example.massagecentr.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.massagecentr.databinding.ItemMessageAdminBinding
import com.example.massagecentr.databinding.ItemMessageClientBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val currentUserId: String,
    private var items: List<ChatMessage> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CLIENT = 1
        private const val VIEW_TYPE_ADMIN = 0
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale("ru"))

    fun submitList(newItems: List<ChatMessage>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].role == "client") VIEW_TYPE_CLIENT else VIEW_TYPE_ADMIN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_CLIENT) {
            val binding = ItemMessageClientBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ClientViewHolder(binding)
        } else {
            val binding = ItemMessageAdminBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            AdminViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        val time = msg.timestamp?.toDate()?.let { timeFormat.format(it) }.orEmpty()
        when (holder) {
            is ClientViewHolder -> holder.bind(msg, time)
            is AdminViewHolder -> holder.bind(msg, time)
        }
    }

    override fun getItemCount() = items.size

    class ClientViewHolder(private val binding: ItemMessageClientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage, time: String) {
            binding.tvMessage.text = msg.text
            binding.tvTime.text = time
        }
    }

    class AdminViewHolder(private val binding: ItemMessageAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage, time: String) {
            binding.tvMessage.text = msg.text
            binding.tvTime.text = time
        }
    }
}
