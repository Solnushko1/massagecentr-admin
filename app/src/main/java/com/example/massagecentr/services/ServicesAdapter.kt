package com.example.massagecentr.services

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.massagecentr.databinding.ItemServiceBinding
import com.example.massagecentr.databinding.ItemServiceHeaderBinding

class ServicesAdapter(private var items: List<Any> = emptyList()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    fun submitList(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemServiceHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            CategoryHeaderViewHolder(binding)
        } else {
            val binding = ItemServiceBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ServiceItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryHeaderViewHolder -> holder.bind(items[position] as String)
            is ServiceItemViewHolder -> holder.bind(items[position] as ServiceItem)
        }
    }

    override fun getItemCount() = items.size

    class CategoryHeaderViewHolder(private val binding: ItemServiceHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: String) {
            binding.tvCategory.text = category
        }
    }

    class ServiceItemViewHolder(private val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ServiceItem) {
            binding.tvName.text = item.name
            binding.tvPrice.text = if (item.price.isNotBlank()) "${item.price} руб." else ""
            binding.tvDuration.text = if (item.duration.isNotBlank()) "${item.duration} мин." else ""
        }
    }
}
