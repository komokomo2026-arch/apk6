package com.ekambani.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.webkit.SslError
import android.content.ActivityNotFoundException

class MainActivity : Activity() {

    companion object {
        private const val HOME_URL = "https://komokomo2026-arch.github.io/posnew6/"
        private const val TRUSTED_HOST = "komokomo2026-arch.github.io"
        private const val CAMERA_PERMISSION_REQUEST = 410
        private const val FILE_CHOOSER_REQUEST = 411
    }

    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var loadingPanel: LinearLayout
    private lateinit var offlinePanel: LinearLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermission: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        root = FrameLayout(this)
        root.setBackgroundColor(Color.rgb(245, 247, 250))

        webView = WebView(this)
        webView.setBackgroundColor(Color.WHITE)

        loadingPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(245, 247, 250))
            setPadding(36, 32, 36, 32)
        }
        val brand = TextView(this).apply {
            text = "EKAMBANI"
            textSize = 23f
            setTextColor(Color.rgb(18, 59, 93))
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Loading your workspace"
            textSize = 14f
            setTextColor(Color.rgb(95, 105, 115))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 22)
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            max = 100
        }
        progress.layoutParams = LinearLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.62f).toInt(), 7
        )
        loadingText = subtitle
        loadingPanel.addView(brand)
        loadingPanel.addView(subtitle)
        loadingPanel.addView(progress)

        offlinePanel = createOfflinePanel()

        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(loadingPanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { })
        root.addView(offlinePanel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.cacheMode = if (hasNetwork()) WebSettings.LOAD_DEFAULT
                             else WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.mediaPlaybackRequiresUserGesture = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val swSettings = android.webkit.ServiceWorkerController.getInstance().serviceWorkerWebSettings
            swSettings.cacheMode = WebSettings.LOAD_DEFAULT
            swSettings.allowContentAccess = true
            swSettings.allowFileAccess = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == TRUSTED_HOST) return false
                openExternal(uri)
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                showLoading()
            }

            override fun onPageFinished(view: WebView, url: String) {
                loadingPanel.visibility = View.GONE
                offlinePanel.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) showOffline()
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                if (request.isForMainFrame && errorResponse.statusCode >= 500) showOffline()
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                showOffline()
            }

        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress < 100 && offlinePanel.visibility != View.VISIBLE) {
                    progress.visibility = View.VISIBLE
                    loadingText.visibility = View.VISIBLE
                } else if (newProgress >= 100) {
                    loadingPanel.visibility = View.GONE
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val requested = request.resources.toSet()
                    val cameraNeeded = requested.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    if (request.origin.host != TRUSTED_HOST) {
                        request.deny()
                        return@runOnUiThread
                    }
                    if (cameraNeeded && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        pendingWebPermission = request
                        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
                    } else {
                        val allowed = request.resources.filter {
                            it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        }.toTypedArray()
                        if (allowed.isNotEmpty()) request.grant(allowed) else request.deny()
                    }
                }
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                webView.visibility = View.GONE
                root.addView(view, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
                enterImmersiveMode()
            }

            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                webView.visibility = View.VISIBLE
                enterImmersiveMode()
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                return try {
                    val intent = fileChooserParams.createIntent()
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                    true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                if (url.startsWith("https://")) {
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setMimeType(mimeType)
                        .addRequestHeader("User-Agent", userAgent)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            URLUtil.guessFileName(url, contentDisposition, mimeType)
                        )
                    val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    manager.enqueue(request)
                    Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Download could not start", Toast.LENGTH_SHORT).show()
            }
        })

        loadHome()
    }

    private fun createOfflinePanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(42, 42, 42, 42)
            setBackgroundColor(Color.rgb(248, 250, 252))
            visibility = View.GONE

            addView(TextView(this@MainActivity).apply {
                text = "No internet connection"
                textSize = 21f
                setTextColor(Color.rgb(30, 41, 59))
                gravity = Gravity.CENTER
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@MainActivity).apply {
                text = "Connect to the internet and try again. If this is your first launch, an internet connection is needed to load EKAMBANI for the first time."
                textSize = 14f
                setTextColor(Color.rgb(100, 116, 139))
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 22)
            })
            addView(Button(this@MainActivity).apply {
                text = "TRY AGAIN"
                isAllCaps = false
                setOnClickListener { loadHome() }
            })
        }
    }

    private fun showLoading() {
        offlinePanel.visibility = View.GONE
        loadingPanel.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        webView.visibility = View.VISIBLE
    }

    private fun showOffline() {
        runOnUiThread {
            loadingPanel.visibility = View.GONE
            webView.visibility = View.GONE
            offlinePanel.visibility = View.VISIBLE
        }
    }

    private fun loadHome() {
        showLoading()
        webView.settings.cacheMode = if (hasNetwork()) WebSettings.LOAD_DEFAULT
                                     else WebSettings.LOAD_CACHE_ELSE_NETWORK
        webView.loadUrl(HOME_URL)
    }

    private fun hasNetwork(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No app available to open this link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == RESULT_OK && data != null) {
                val clip = data.clipData
                if (clip != null) Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                else data.data?.let { arrayOf(it) }
            } else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            val request = pendingWebPermission
            pendingWebPermission = null
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                request?.deny()
            }
        }
    }

    override fun onBackPressed() {
        if (customView != null) {
            customView?.let { root.removeView(it) }
            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
            webView.visibility = View.VISIBLE
        } else if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
