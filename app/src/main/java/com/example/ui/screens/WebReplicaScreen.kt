package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.PriceNotificationHelper

private const val WEB_REPLICA_ASSET_URL = "file:///android_asset/finadvisor_web/index.html"

private class FinAdvisorWebBridge(private val context: Context) {
    @JavascriptInterface
    fun postAlertNotification(title: String, body: String) {
        PriceNotificationHelper.postPriceAlertNotification(
            context = context,
            notificationId = (title + body).hashCode(),
            title = title,
            body = body
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebReplicaScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mobileFramePreview by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val currentWebView = webViewRef
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else {
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("web_replica_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Global Pulse"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FinAdvisor Web Replica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Responsive D2C MBA Website MVP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilterChip(
                    selected = !mobileFramePreview,
                    onClick = { mobileFramePreview = false },
                    label = { Text("Full") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.DesktopWindows,
                            contentDescription = "Full width web view",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("web_replica_full_chip")
                )

                FilterChip(
                    selected = mobileFramePreview,
                    onClick = { mobileFramePreview = true },
                    label = { Text("390dp") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = "Mobile width preview",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("web_replica_mobile_chip")
                )

                IconButton(
                    onClick = { webViewRef?.reload() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("web_replica_reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reload Website Replica"
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(if (mobileFramePreview) 12.dp else 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val containerModifier = if (mobileFramePreview) {
                Modifier
                    .widthIn(max = 390.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
            } else {
                Modifier.fillMaxSize()
            }

            Surface(
                modifier = containerModifier,
                shape = RoundedCornerShape(if (mobileFramePreview) 24.dp else 0.dp),
                border = if (mobileFramePreview) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                } else {
                    null
                }
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            addJavascriptInterface(
                                FinAdvisorWebBridge(ctx.applicationContext),
                                "FinAdvisorAndroidBridge"
                            )
                            loadUrl(WEB_REPLICA_ASSET_URL)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("web_replica_webview")
                )
            }
        }
    }
}
