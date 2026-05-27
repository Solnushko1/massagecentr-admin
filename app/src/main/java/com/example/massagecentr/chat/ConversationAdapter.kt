package com.example.massagecentr.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.massagecentr.R
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationAdapter(
    private val onClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.VH>() {

    private var items: List<ChatConversation> = emptyList()
    private var activeKey: String = ""
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun submitList(list: List<ChatConversation>) {
        items = list
        notifyDataSetChanged()
    }

    fun setActiveKey(key: String) {
        activeKey = key
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName:    TextView = view.findViewById(R.id.tvConvName)
        private val tvEmail:   TextView = view.findViewById(R.id.tvConvEmail)
        private val tvPreview: TextView = view.findViewById(R.id.tvConvPreview)
        private val tvTime:    TextView = view.findViewById(R.id.tvConvTime)

        fun bind(item: ChatConversation) {
            tvName.text    = item.userName.ifBlank { "Клиент" }
            tvEmail.text   = item.userEmail.ifBlank { item.chatKey }
            tvPreview.text = item.lastMessage.ifBlank { "Нет сообщений" }
            tvTime.text    = item.lastTimestamp?.toDate()?.let { timeFmt.format(it) } ?: ""

            val active = item.chatKey == activeKey
            itemView.setBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (active) R.color.primary_container else android.R.color.transparent
                )
            )
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
