package com.example.devicemonitorapp

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.concurrent.thread

class StorageFragment : Fragment() {
    private lateinit var textViewTotalStorage: TextView
    private lateinit var textViewUsedStorage: TextView
    private lateinit var textViewFreeStorage: TextView
    private lateinit var progressBarStorageUsage: ProgressBar
    private lateinit var textViewStoragePercentage: TextView
    private lateinit var textViewAppsStorage: TextView
    private lateinit var textViewMediaStorage: TextView
    private lateinit var textViewOtherStorage: TextView
    private lateinit var textViewSystemStorage: TextView
    private lateinit var textViewCacheStorage: TextView
    private lateinit var textViewDocumentsStorage: TextView
    private lateinit var textViewDownloadsStorage: TextView
    private lateinit var textViewTemporaryFilesStorage: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_storage, container, false)
        textViewTotalStorage = view.findViewById(R.id.textViewTotalStorage)
        textViewUsedStorage = view.findViewById(R.id.textViewUsedStorage)
        textViewFreeStorage = view.findViewById(R.id.textViewFreeStorage)
        progressBarStorageUsage = view.findViewById(R.id.progressBarStorageUsage)
        textViewStoragePercentage = view.findViewById(R.id.textViewStoragePercentage)
        textViewAppsStorage = view.findViewById(R.id.textViewAppsStorage)
        textViewMediaStorage = view.findViewById(R.id.textViewMediaStorage)
        textViewOtherStorage = view.findViewById(R.id.textViewOtherStorage)
        textViewSystemStorage = view.findViewById(R.id.textViewSystemStorage)
        textViewCacheStorage = view.findViewById(R.id.textViewCacheStorage)
        textViewDocumentsStorage = view.findViewById(R.id.textViewDocumentsStorage)
        textViewDownloadsStorage = view.findViewById(R.id.textViewDownloadsStorage)
        textViewTemporaryFilesStorage = view.findViewById(R.id.textViewTemporaryFilesStorage)
        monitorStorageUsage()
        // Check and set the background drawable based on the dark mode setting
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isNightMode = uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES

        val root = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.storagebg)

        if (isNightMode) {
            root.setBackgroundResource(R.drawable.night3) // Replace with your night mode drawable
        } else {
            root.setBackgroundResource(R.drawable.bg1) // Replace with your day mode drawable
        }
        return view
    }

    private fun monitorStorageUsage() {
        thread {
            while (true) {
                try {
                    val storageInfo = getStorageInfo()
                    val totalStorage = storageInfo["totalStorage"] ?: 0L
                    val usedStorage = storageInfo["usedStorage"] ?: 0L
                    val freeStorage = storageInfo["freeStorage"] ?: 0L
                    val usedPercentage = if (totalStorage > 0) (usedStorage * 100 / totalStorage).toInt() else 0

                    val appsStorage = getAppsStorage()
                    val mediaStorage = getMediaStorage()
                    val systemStorage = getSystemStorage()
                    val cacheStorage = getCacheStorage()
                    val documentsStorage = getDocumentsStorage()
                    val downloadsStorage = getDownloadsStorage()
                    val temporaryFilesStorage = getTemporaryFilesStorage()
                    //fetchFilesystemDetails()
                    val otherStorage = usedStorage - appsStorage - mediaStorage - systemStorage - cacheStorage - documentsStorage - downloadsStorage - temporaryFilesStorage

                    activity?.runOnUiThread {
                        textViewTotalStorage.text = "Total Storage: ${convertBytesToReadableFormat(totalStorage)}"
                        textViewUsedStorage.text = "Used Storage: ${convertBytesToReadableFormat(usedStorage)}"
                        textViewFreeStorage.text = "Free Storage: ${convertBytesToReadableFormat(freeStorage)}"
                        progressBarStorageUsage.progress = usedPercentage
                        textViewStoragePercentage.text = "Used: $usedPercentage%"

                        textViewAppsStorage.text = "Apps: ${convertBytesToReadableFormat(appsStorage)}"
                        textViewMediaStorage.text = "Media: ${convertBytesToReadableFormat(mediaStorage)}"
                        textViewOtherStorage.text = "Other: ${convertBytesToReadableFormat(otherStorage)}"
                        textViewSystemStorage.text = "System: ${convertBytesToReadableFormat(systemStorage)}"
                        textViewCacheStorage.text = "Cache: ${convertBytesToReadableFormat(cacheStorage)}"
                        textViewDocumentsStorage.text = "Documents: ${convertBytesToReadableFormat(documentsStorage)}"
                        textViewDownloadsStorage.text = "Downloads: ${convertBytesToReadableFormat(downloadsStorage)}"
                        textViewTemporaryFilesStorage.text = "Temporary Files: ${convertBytesToReadableFormat(temporaryFilesStorage)}"
                    }
                    Thread.sleep(10000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getStorageInfo(): Map<String, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val totalStorage = totalBlocks * blockSize
        val freeStorage = availableBlocks * blockSize
        val usedStorage = totalStorage - freeStorage

        return mapOf(
            "totalStorage" to totalStorage,
            "usedStorage" to usedStorage,
            "freeStorage" to freeStorage
        )
    }

    private fun getAppsStorage(): Long {
        // Example logic to calculate Apps storage usage
        return 11L * 1024 * 1024 * 1024 // Convert GB to Bytes
    }

    private fun getMediaStorage(): Long {
        val mediaDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )
        var totalSize = 0L
        mediaDirs.forEach { dir ->
            if (dir != null && dir.exists()) {
                val dirSize = getDirectorySize(dir)
                Log.d("StorageFragment", "Directory ${dir.path} size: $dirSize")
                totalSize += dirSize
            }
        }
        return totalSize
    }

    private fun getSystemStorage(): Long {
        // Example logic to calculate System storage usage
        return 15L * 1024 * 1024 * 1024 // Convert GB to Bytes
    }

    private fun getCacheStorage(): Long {
        val cacheDirs = listOf(
            context?.cacheDir,
            context?.externalCacheDir
        )
        var totalSize = 0L
        cacheDirs.forEach { dir ->
            if (dir != null && dir.exists()) {
                val dirSize = getDirectorySize(dir)
                Log.d("StorageFragment", "Cache directory size: $dirSize")
                totalSize += dirSize
            }
        }
        return totalSize
    }

    private fun getDocumentsStorage(): Long {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        var totalSize = 0L
        if (documentsDir != null && documentsDir.exists()) {
            val dirSize = getDirectorySize(documentsDir)
            Log.d("StorageFragment", "Documents directory size: $dirSize")
            totalSize = dirSize
        } else {
            Log.d("StorageFragment", "Documents directory does not exist: ${documentsDir?.path}")
        }
        return totalSize
    }

    private fun getDownloadsStorage(): Long {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var totalSize = 0L
        if (downloadsDir != null && downloadsDir.exists()) {
            val dirSize = getDirectorySize(downloadsDir)
            Log.d("StorageFragment", "Downloads directory size: $dirSize")
            totalSize = dirSize
        } else {
            Log.d("StorageFragment", "Downloads directory does not exist: ${downloadsDir?.path}")
        }
        return totalSize
    }

    private fun getTemporaryFilesStorage(): Long {
        val tempDirs = listOf(
            context?.cacheDir,
            context?.externalCacheDir
        )
        var totalSize = 0L
        tempDirs.forEach { dir ->
            if (dir != null && dir.exists()) {
                val dirSize = getDirectorySize(dir)
                Log.d("StorageFragment", "Temporary files directory size: $dirSize")
                totalSize += dirSize
            } else {
                Log.d("StorageFragment", "Temporary files directory does not exist: ${dir?.path}")
            }
        }
        return totalSize
    }

    private fun getDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) {
            return 0L
        }
        return dir.walkTopDown().fold(0L) { acc, file ->
            acc + file.length()
        }
    }

    private fun convertBytesToReadableFormat(bytes: Long): String {
        val kb = bytes / 1024
        val mb = kb / 1024
        val gb = mb / 1024
        return when {
            gb > 0 -> "$gb GB"
            mb > 0 -> "$mb MB"
            kb > 0 -> "$kb KB"
            else -> "$bytes Bytes"
        }
    }


}
