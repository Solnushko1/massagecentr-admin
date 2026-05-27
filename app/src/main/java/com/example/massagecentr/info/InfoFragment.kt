package com.example.massagecentr.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.massagecentr.MassageCentrApp
import com.example.massagecentr.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnCall1.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+79307540204")))
        }

        binding.btnCall2.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+74842400204")))
        }

        binding.btnMap.setOnClickListener {
            val geoUri = Uri.parse("geo:54.513845,36.261215?q=ул.+Ленина+1,+Калуга")
            val intent = Intent(Intent.ACTION_VIEW, geoUri)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }
        }

        binding.btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:kaluga.8za@gmail.com"))
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            }
        }

        // Панель администратора — видна только администратору
        val isAdmin = MassageCentrApp.session.isAdmin
        binding.cardAdmin.isVisible = isAdmin
        if (isAdmin) {
            binding.btnAdminPanel.setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://massagecentr-admin.vercel.app/admin")
                )
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
