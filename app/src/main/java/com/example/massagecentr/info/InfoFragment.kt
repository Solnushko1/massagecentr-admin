package com.example.massagecentr.info

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.massagecentr.MassageCentrApp
import com.example.massagecentr.R
import com.example.massagecentr.databinding.FragmentInfoBinding

class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    // Документы о государственных гарантиях
    private val govDocs = listOf(
        "Программа гос. гарантий бесплатного оказания мед. помощи" to
            "https://drive.google.com/file/d/1ZGwmG0PaSNiQTzE1jIiZCxvlyiBGOPkV/view?usp=sharing",
        "Программа гос. гарантий Калужской области" to
            "https://drive.google.com/file/d/1SkOfrIrN88xq8z_lETkW5RZvP87KTB_d/view?usp=sharing",
        "Информация о порядке предоставления мед. услуг и их оплате" to
            "https://docs.google.com/document/d/1KAEnXH2iEHwxbqnvSXDC7MUd72m4gdv6/edit?usp=sharing",
        "Перечень определённых видов вмешательств" to
            "https://docs.google.com/document/d/19gEdI1tX-xVIfmugTRl3SBpxChdM_VJV/edit?usp=sharing",
        "Решение конфликтных ситуаций" to
            "https://docs.google.com/document/d/1_gsx_Fu-AydYyXVUDG6FrsUVW6zbFZ7j/edit?usp=sharing",
        "Сроки ожидания" to
            "https://docs.google.com/document/d/1dq5RBbmNuHgvguuV4mFB2U2AJAzJrJZl/edit?usp=sharing",
        "Контакты органов здравоохранения" to
            "https://docs.google.com/document/d/1FQXYI4qMXmo5N3If_BT5UG5w9BUCqfDv/edit?usp=sharing"
    )

    // Формы для пациентов
    private val formDocs = listOf(
        "Согласие на обработку персональных данных" to
            "https://docs.google.com/document/d/1cejpz8n_aOkEUzxb2HTXE2Sdf5Hm-A4k/edit?usp=sharing",
        "Договор на оказание платных медицинских услуг" to
            "https://docs.google.com/document/d/1piixnYqePFO1SH0659_AQmYIHSsiIkK9/edit?usp=sharing",
        "Дополнительное соглашение к договору" to
            "https://docs.google.com/document/d/13a1LsmrcGrQmdWu_7E-vn85sadI8LKeU/edit?usp=sharing",
        "Отказ от видов медицинских вмешательств" to
            "https://docs.google.com/document/d/1NnLg8H0LItr8yfw3p0eIIDWGfqU3FjR8/edit?usp=sharing",
        "Информированное добровольное согласие" to
            "https://docs.google.com/document/d/1lDDlS5guKAzgG-AR_DOCy-FNxInl0raB/edit?usp=sharing",
        "Акт выполненных услуг" to
            "https://docs.google.com/spreadsheets/d/19yWaUIIqOVzPUOZSPhDos_4ckPeb0ji4/edit?usp=sharing"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ── Кнопки звонка ──────────────────────────────────────
        binding.btnCall1.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+79307540204")))
        }
        binding.btnCall2.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+74842400204")))
        }

        // ── Карта ───────────────────────────────────────────────
        binding.btnMap.setOnClickListener {
            val geoUri = Uri.parse("geo:54.513845,36.261215?q=ул.+Ленина+1,+Калуга")
            val intent = Intent(Intent.ACTION_VIEW, geoUri)
            try { startActivity(intent) }
            catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://yandex.ru/maps/?text=г.+Калуга,+ул.+Ленина,+д.+1")))
            }
        }

        // ── Email — фикс: без resolveActivity (ломается на Android 11+) ──
        binding.btnEmail.setOnClickListener {
            try {
                startActivity(
                    Intent(Intent.ACTION_SENDTO,
                        Uri.parse("mailto:kaluga.8za@gmail.com?subject=Обращение"))
                )
            } catch (e: Exception) {
                // Нет почтового приложения — открываем Gmail в браузере
                startActivity(
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://mail.google.com/mail/?view=cm&to=kaluga.8za@gmail.com&su=Обращение"))
                )
            }
        }

        // ── Аккордеон: Государственные гарантии ────────────────
        populateDocList(binding.contentGovDocs, govDocs)
        setupAccordion(binding.headerGovDocs, binding.contentGovDocs, binding.arrowGovDocs)

        // ── Аккордеон: Формы для пациентов ─────────────────────
        populateDocList(binding.contentForms, formDocs)
        setupAccordion(binding.headerForms, binding.contentForms, binding.arrowForms)

        // ── Панель администратора ───────────────────────────────
        val isAdmin = MassageCentrApp.session.isAdmin
        binding.cardAdmin.isVisible = isAdmin
        if (isAdmin) {
            binding.btnAdminPanel.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://massagecentr-admin.vercel.app/admin")))
            }
        }
    }

    /** Наполняет LinearLayout ссылками-документами */
    private fun populateDocList(container: android.widget.LinearLayout,
                                 docs: List<Pair<String, String>>) {
        val ctx = requireContext()
        val accentColor = ContextCompat.getColor(ctx, R.color.accent)
        val dividerColor = Color.parseColor("#F0F0F0")
        val dp = resources.displayMetrics.density

        docs.forEachIndexed { index, (name, url) ->
            val tv = TextView(ctx).apply {
                text = "📄  $name"
                textSize = 13.5f
                setTextColor(accentColor)
                setPadding((16 * dp).toInt(), (12 * dp).toInt(),
                            (16 * dp).toInt(), (12 * dp).toInt())
                setTypeface(null, Typeface.NORMAL)
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.attr.selectableItemBackground.let {
                    val ta = ctx.obtainStyledAttributes(intArrayOf(it))
                    val res = ta.getResourceId(0, 0)
                    ta.recycle(); res
                })
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
            container.addView(tv)

            // Разделитель между пунктами (кроме последнего)
            if (index < docs.size - 1) {
                val divider = View(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
                    )
                    setBackgroundColor(dividerColor)
                }
                container.addView(divider)
            }
        }
    }

    /** Логика раскрытия/схлопывания аккордеона */
    private fun setupAccordion(header: android.widget.LinearLayout,
                                content: android.widget.LinearLayout,
                                arrow: TextView) {
        header.setOnClickListener {
            val isOpen = content.isVisible
            content.isVisible = !isOpen
            arrow.text = if (isOpen) "▼" else "▲"
            arrow.setTextColor(
                ContextCompat.getColor(requireContext(),
                    if (isOpen) R.color.text_secondary else R.color.primary)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
