package com.example.massagecentr.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.massagecentr.R
import com.example.massagecentr.databinding.FragmentPhoneAuthBinding

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
