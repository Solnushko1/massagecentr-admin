package com.example.massagecentr.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.massagecentr.R
import com.example.massagecentr.databinding.ItemHomeHeaderBinding
import com.example.massagecentr.databinding.ItemNewsBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(private var items: List<NewsItem> = emptyList()) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NEWS   = 1
    }

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"))

    /**
     * Подбирает локальную картинку по ключевым словам в заголовке новости.
     * Порядок соответствует пожеланиям:
     *  "Добро пожаловать"  → зал ожидания (news_welcome)
     *  "10 января" / "центр" → врач (news_clinic)  — если есть отдельная новость
     *  "Детский"            → детский массаж (news_baby)
     *  "Коррекция" / "фигур"→ news_photo2 (массаж спины)
     *  "Выезд"              → news_photo1 (массаж тёмный фон)
     *  иначе                → чередование photo1/photo2
     */
    private fun localPhotoFor(title: String, index: Int): Int {
        val t = title.lowercase()
        return when {
            t.contains("добро пожаловать") && !t.contains("детск") -> R.drawable.news_welcome
            t.contains("центр") || t.contains("января") || t.contains("врач") -> R.drawable.news_clinic
            t.contains("детск") || t.contains("малыш") || t.contains("ребён") -> R.drawable.news_baby
            t.contains("коррекц") || t.contains("фигур") || t.contains("лимфо") -> R.drawable.news_photo2
            t.contains("выезд") || t.contains("дом") -> R.drawable.news_photo1
            else -> if (index % 2 == 0) R.drawable.news_photo1 else R.drawable.news_photo2
        }
    }

    fun submitList(newItems: List<NewsItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = if (position == 0) TYPE_HEADER else TYPE_NEWS
    override fun getItemCount() = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(
                ItemHomeHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            NewsViewHolder(
                ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NewsViewHolder) {
            val newsIndex = position - 1
            holder.bind(items[newsIndex], newsIndex)
        }
    }

    inner class HeaderViewHolder(binding: ItemHomeHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class NewsViewHolder(private val b: ItemNewsBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: NewsItem, index: Int) {
            b.tvTitle.text = item.title
            b.tvBody.text  = item.body
            b.tvDate.text  = item.date?.toDate()?.let { dateFormat.format(it) }.orEmpty()

            val localPhoto = localPhotoFor(item.title, index)

            if (item.imageUrl.isNotBlank()) {
                Glide.with(b.root.context)
                    .load(item.imageUrl)
                    .placeholder(localPhoto)
                    .error(localPhoto)
                    .centerCrop()
                    .into(b.ivNewsImage)
            } else {
                b.ivNewsImage.setImageResource(localPhoto)
                b.ivNewsImage.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }
}
