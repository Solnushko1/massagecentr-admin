package com.example.massagecentr.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.massagecentr.MassageCentrApp
import com.example.massagecentr.R
import com.example.massagecentr.auth.AuthActivity
import com.example.massagecentr.databinding.FragmentHomeBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val adapter = NewsAdapter { item ->
        showNewsDetail(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Меню тулбара (три точки → Выйти)
        binding.toolbar.inflateMenu(R.menu.menu_home)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                confirmLogout()
                true
            } else false
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        viewModel.news.observe(viewLifecycleOwner) { newsList ->
            adapter.submitList(newsList)
            binding.tvEmpty.isVisible = newsList.isEmpty()
        }
    }

    /** BottomSheet с полным текстом новости */
    private fun showNewsDetail(item: NewsItem) {
        val sheet = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_2, null, false)

        // Используем кастомный View напрямую — просто AlertDialog для надёжности
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"))
        val dateStr = item.date?.toDate()?.let { dateFormat.format(it) } ?: ""

        AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setMessage(buildString {
                if (dateStr.isNotBlank()) append("📅 $dateStr\n\n")
                append(item.body)
            })
            .setPositiveButton("Закрыть", null)
            .show()
            .also { dialog ->
                // Увеличиваем размер текста в теле сообщения
                dialog.window?.decorView?.let { root ->
                    root.post {
                        try {
                            val msgView = root.findViewById<android.widget.TextView>(
                                android.R.id.message
                            )
                            msgView?.textSize = 15f
                            msgView?.setPadding(48, 16, 48, 16)
                        } catch (_: Exception) {}
                    }
                }
            }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выйти из аккаунта?")
            .setMessage("Вы сможете войти снова, введя email и код подтверждения.")
            .setPositiveButton("Выйти") { _, _ -> performLogout() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()
        MassageCentrApp.session.clear()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
