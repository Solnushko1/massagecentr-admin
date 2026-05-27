package com.example.massagecentr.chat

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.massagecentr.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChatAdapter(viewModel.userId)

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        // Лимит 300 символов в поле ввода
        binding.etMessage.filters = arrayOf(InputFilter.LengthFilter(300))

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            val wasNearBottom = isNearBottom()
            adapter.submitList(messages)
            binding.tvEmpty.isVisible = messages.isEmpty()
            // Прокручиваем вниз только если уже были внизу
            if (wasNearBottom && messages.isNotEmpty()) {
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun isNearBottom(): Boolean {
        val lm = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return true
        val lastVisible = lm.findLastCompletelyVisibleItemPosition()
        val total = adapter.itemCount
        return total == 0 || lastVisible >= total - 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
