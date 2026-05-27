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
import com.example.massagecentr.MassageCentrApp
import com.example.massagecentr.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()

    private lateinit var msgAdapter: ChatAdapter
    private lateinit var convAdapter: ConversationAdapter

    private val isAdmin: Boolean get() = MassageCentrApp.session.isAdmin

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (isAdmin) setupAdminMode() else setupClientMode()
    }

    // ─── Режим клиента ────────────────────────────────────────────────────────

    private fun setupClientMode() {
        msgAdapter = ChatAdapter(viewModel.userId)
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerView.adapter = msgAdapter

        binding.etMessage.filters = arrayOf(InputFilter.LengthFilter(300))

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            val nearBottom = isNearBottom()
            msgAdapter.submitList(messages)
            binding.tvEmpty.isVisible = messages.isEmpty()
            if (nearBottom && messages.isNotEmpty()) {
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    // ─── Режим администратора ─────────────────────────────────────────────────

    private fun setupAdminMode() {
        // Показываем левую панель
        binding.panelConversations.isVisible = true
        binding.dividerPanels.isVisible      = true

        // Показываем «Выберите клиента» до выбора
        binding.tvSelectClient.isVisible = true
        binding.recyclerView.isVisible   = false
        binding.layoutInput.isVisible    = false
        binding.tvEmpty.isVisible        = false

        // Адаптер сообщений (senderId = emailKey администратора)
        msgAdapter = ChatAdapter(viewModel.userId)
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.recyclerView.adapter = msgAdapter

        // Адаптер списка клиентов
        convAdapter = ConversationAdapter { conv -> openConversation(conv) }
        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = convAdapter

        // Подписываемся на список переписок
        viewModel.conversations.observe(viewLifecycleOwner) { list ->
            convAdapter.submitList(list)
        }

        // Подписываемся на сообщения открытой переписки
        viewModel.adminMessages.observe(viewLifecycleOwner) { messages ->
            val nearBottom = isNearBottom()
            msgAdapter.submitList(messages)
            if (nearBottom && messages.isNotEmpty()) {
                binding.recyclerView.scrollToPosition(messages.size - 1)
            }
        }

        // Кнопка отправки (администраторский ответ)
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendAdminReply(text)
                binding.etMessage.setText("")
            }
        }

        binding.etMessage.filters = arrayOf(InputFilter.LengthFilter(500))
    }

    private fun openConversation(conv: ChatConversation) {
        // Обновляем выделение в списке
        convAdapter.setActiveKey(conv.chatKey)

        // Показываем переписку
        binding.tvSelectClient.isVisible = false
        binding.recyclerView.isVisible   = true
        binding.layoutInput.isVisible    = true
        binding.tvEmpty.isVisible        = false

        // Меняем заголовок тулбара
        binding.toolbar.title = conv.userName.ifBlank { conv.userEmail }

        // Загружаем сообщения
        viewModel.openConversation(conv.chatKey)
    }

    // ─── Вспомогательные ─────────────────────────────────────────────────────

    private fun isNearBottom(): Boolean {
        val lm = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return true
        val lastVisible = lm.findLastCompletelyVisibleItemPosition()
        val total = msgAdapter.itemCount
        return total == 0 || lastVisible >= total - 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
