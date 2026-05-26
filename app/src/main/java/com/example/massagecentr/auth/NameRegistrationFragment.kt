package com.example.massagecentr.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.massagecentr.databinding.FragmentNameRegistrationBinding

class NameRegistrationFragment : Fragment() {

    private var _binding: FragmentNameRegistrationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNameRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvPhone.text = "Номер: ${viewModel.getCurrentPhone()}"

        binding.btnRegister.setOnClickListener {
            viewModel.registerUser(binding.etName.text.toString())
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state is AuthState.Loading
            binding.btnRegister.isEnabled = state !is AuthState.Loading

            when (state) {
                is AuthState.LoggedIn -> (requireActivity() as AuthActivity).openMain()
                is AuthState.Error -> {
                    binding.tvStatus.text = state.message
                    binding.tvStatus.isVisible = true
                }
                else -> binding.tvStatus.isVisible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
