package com.example

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.network.SupabaseClient
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CareerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  private val viewModel: CareerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val currentAnimeSkin by viewModel.selectedAnimeSkin.collectAsStateWithLifecycle()
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
      MyApplicationTheme(animeSkin = currentAnimeSkin, darkTheme = isDarkMode) {
        MainScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    checkForUpdate()
  }

  private fun checkForUpdate() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val latestVersion = SupabaseClient.getLatestAppVersion()
        if (latestVersion != null) {
          val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
          } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode
          }

          if (latestVersion.versionCode > currentVersionCode) {
            withContext(Dispatchers.Main) {
              showUpdateDialog(latestVersion.versionName, latestVersion.apkUrl)
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun showUpdateDialog(versionName: String, apkUrl: String) {
    if (isFinishing || isDestroyed) return
    AlertDialog.Builder(this)
      .setTitle("发现新版本 ($versionName)")
      .setMessage("检测到系统有新版本发布，是否立即下载并更新？")
      .setPositiveButton("立即更新") { _, _ ->
        downloadAndInstallApk(apkUrl, versionName)
      }
      .setNegativeButton("以后再说", null)
      .show()
  }

  private fun downloadAndInstallApk(url: String, versionName: String) {
    if (url.isBlank()) {
      Toast.makeText(this, "下载链接为空", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      val request = DownloadManager.Request(Uri.parse(url)).apply {
        setTitle("应用更新 ($versionName)")
        setDescription("正在下载安装包...")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app_update_${versionName}.apk")
        setAllowedOverMetered(true)
        setAllowedOverRoaming(true)
      }
      val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
      val downloadId = downloadManager.enqueue(request)

      val onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
          if (downloadId == id) {
            try {
              val uri = downloadManager.getUriForDownloadedFile(downloadId)
              if (uri != null) {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                  setDataAndType(uri, "application/vnd.android.package-archive")
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(installIntent)
              }
            } catch (e: Exception) {
              e.printStackTrace()
            }
            try {
              unregisterReceiver(this)
            } catch (e: Exception) {}
          }
        }
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED)
      } else {
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
      }
      Toast.makeText(this, "开始后台下载更新...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }
}
