package com.example.massagecentr.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class AdminFragment : Fragment() {

    private var webView: WebView? = null

    companion object {
        const val ADMIN_URL = "https://massagecentr-admin.vercel.app/admin"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val wv = WebView(requireContext())
        webView = wv

        wv.settings.apply {
            javaScriptEnabled  = true          // Firebase JS SDK требует JS
            domStorageEnabled  = true          // Хранение сессии Firebase
            databaseEnabled    = true
            cacheMode          = WebSettings.LOAD_DEFAULT
            mixedContentMode   = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            loadWithOverviewMode = true
            useWideViewPort    = true
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                // Все ссылки открываем внутри WebView
                view.loadUrl(request.url.toString())
                return true
            }
        }

        wv.loadUrl(ADMIN_URL)
        return wv
    }

    /** Возвращает true, если WebView может перейти назад по истории */
    fun canGoBack(): Boolean = webView?.canGoBack() == true

    /** Переход назад по истории WebView */
    fun goBack() { webView?.goBack() }

    override fun onDestroyView() {
        webView?.apply {
            stopLoading()
            clearHistory()
            destroy()
        }
        webView = null
        super.onDestroyView()
    }
}
