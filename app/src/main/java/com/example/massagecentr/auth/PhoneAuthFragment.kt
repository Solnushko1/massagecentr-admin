package com.example.massagecentr.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.massagecentr.R
import com.example.massagecentr.databinding.FragmentPhoneAuthBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class PhoneAuthFragment : Fragment() {

    private var _binding: FragmentPhoneAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhoneAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initCallbacks()

        binding.btnGetCode.setOnClickListener {
            viewModel.sendCode(binding.etEmail.text.toString(), requireActivity())
        }

        binding.btnVerify.setOnClickListener {
            viewModel.verifyCode(binding.etCode.text.toString())
        }

        binding.btnResend.setOnClickListener {
            viewModel.resendCode(requireActivity())
        }

        binding.btnAdminLogin.setOnClickListener {
            showAdminLoginDialog()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: AuthState) {
        binding.progressBar.isVisible = state is AuthState.Loading
        binding.btnGetCode.isEnabled = state !is AuthState.Loading
        binding.btnVerify.isEnabled = state !is AuthState.Loading

        when (state) {
            is AuthState.CodeSent -> {
                binding.divider.isVisible = true
                binding.tvCodeLabel.isVisible = true
                binding.layoutCode.isVisible = true
                binding.btnVerify.isVisible = true
                binding.btnResend.isVisible = true
                binding.tvStatus.text = "✓ Код отправлен на ${state.email}"
                binding.tvStatus.isVisible = true
                binding.btnGetCode.text = "Отправить повторно"
            }
            is AuthState.NeedRegistration -> {
                findNavController().navigate(R.id.action_phoneAuth_to_nameRegistration)
            }
            is AuthState.LoggedIn -> {
                (requireActivity() as AuthActivity).openMain()
            }
            is AuthState.Error -> {
                binding.tvStatus.text = state.message
                binding.tvStatus.isVisible = true
                binding.tvStatus.setTextColor(
                    requireContext().getColor(android.R.color.holo_red_dark)
                )
            }
            else -> {
                binding.tvStatus.isVisible = false
            }
        }
    }

    /** Диалог входа администратора (email + пароль) */
    private fun showAdminLoginDialog() {
        val ctx = requireContext()
        val dp16 = (16 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp16 * 3, dp16, dp16 * 3, dp16)
        }

        val tilEmail = TextInputLayout(ctx,null,
            com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Email администратора"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val etEmail = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        tilEmail.addView(etEmail)
        container.addView(tilEmail)

        val tilPass = TextInputLayout(ctx, null,
            com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = "Пароль"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            isPasswordVisibilityToggleEnabled = true
            setPadding(0, dp16, 0, 0)
        }
        val etPass = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine()
        }
        tilPass.addView(etPass)
        container.addView(tilPass)

        AlertDialog.Builder(ctx)
            .setTitle("Вход администратора")
            .setView(container)
            .setPositiveButton("Войти") { _, _ ->
                viewModel.loginAsAdmin(
                    etEmail.text.toString(),
                    etPass.text.toString()
                )
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
