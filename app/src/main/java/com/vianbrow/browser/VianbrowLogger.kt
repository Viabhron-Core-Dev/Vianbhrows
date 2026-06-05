package com.vianbrow.browser

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
) {
    fun toFormattedString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return "[${sdf.format(Date(timestamp))}] [$level] $tag: $message"
    }
}

object VianbrowLogger {
    private const val PREFS_NAME = "vianbrow_logger_prefs"
    private const val KEY_ENABLED = "logging_enabled"
    private const val KEY_LOGS = "saved_logs" // We'll store logs as a big string for simplicity in Session 1, separated by a rare delimiter
    private const val DELIMITER = "||~||"
    
    private val inMemoryLogs = mutableListOf<LogEntry>()
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    
    var isEnabled: Boolean = true
        set(value) {
            field = value
            prefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply()
        }

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs?.getBoolean(KEY_ENABLED, true) ?: true
        
        loadLogsFromPrefs()
        
        setupCrashHandler()
        i("VianbrowLogger", "Logger initialized")
    }

    fun d(tag: String, message: String) = log("DEBUG", tag, message)
    fun i(tag: String, message: String) = log("INFO", tag, message)
    fun w(tag: String, message: String) = log("WARN", tag, message)
    fun e(tag: String, message: String) = log("ERROR", tag, message)

    private fun log(level: String, tag: String, message: String) {
        if (!isEnabled) return
        
        // Never log passwords, API keys, cookies, etc. is an instruction for the developer,
        // but we can add a basic defensive scan just in case.
        val safeMessage = message
            .replace(Regex("password=.*"), "password=***")
            .replace(Regex("api_key=.*"), "api_key=***")

        val entry = LogEntry(System.currentTimeMillis(), level, tag, safeMessage)
        
        // standard logcat
        when (level) {
            "DEBUG" -> Log.d(tag, safeMessage)
            "INFO" -> Log.i(tag, safeMessage)
            "WARN" -> Log.w(tag, safeMessage)
            "ERROR" -> Log.e(tag, safeMessage)
        }

        synchronized(inMemoryLogs) {
            inMemoryLogs.add(entry)
            saveLogsToPrefs()
        }
    }

    fun getLogs(hours: Int): List<LogEntry> {
        val cutoff = System.currentTimeMillis() - (hours * 3600000L)
        synchronized(inMemoryLogs) {
            return inMemoryLogs.filter { it.timestamp >= cutoff }.sortedByDescending { it.timestamp }
        }
    }

    fun clearLogs() {
        synchronized(inMemoryLogs) {
            inMemoryLogs.clear()
            prefs?.edit()?.remove(KEY_LOGS)?.apply()
        }
    }

    private fun loadLogsFromPrefs() {
        val saved = prefs?.getString(KEY_LOGS, "") ?: ""
        if (saved.isNotEmpty()) {
            val lines = saved.split(DELIMITER)
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split("|", limit = 4)
                if (parts.size == 4) {
                    val ts = parts[0].toLongOrNull() ?: 0L
                    inMemoryLogs.add(LogEntry(ts, parts[1], parts[2], parts[3]))
                }
            }
        }
    }

    private fun saveLogsToPrefs() {
        // Limit to 500 logs to avoid OOM in prefs
        val toSave = inMemoryLogs.takeLast(500)
        val sb = StringBuilder()
        for (log in toSave) {
            sb.append("${log.timestamp}|${log.level}|${log.tag}|${log.message}$DELIMITER")
        }
        prefs?.edit()?.putString(KEY_LOGS, sb.toString())?.apply()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            e("CRASH", "Uncaught exception in thread ${thread.name}: ${exception.message}")
            try {
                // Ensure logs are flushed to prefs
                saveLogsToPrefs()
                
                // Write crash report
                appContext?.let { ctx ->
                    val stackTrace = exception.stackTraceToString()
                    val last50 = inMemoryLogs.takeLast(50).joinToString("\n") { it.toFormattedString() }
                    val content = "CRASH REPORT\n\nStack Trace:\n$stackTrace\n\nLast 50 Logs:\n$last50"
                    
                    val sdf = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
                    val filename = "Vianbrow_crash_${sdf.format(Date())}.txt"
                    
                    saveFileToDownloads(ctx, filename, content)
                }
            } catch (e: Exception) {
                Log.e("VianbrowLogger", "Error writing crash report", e)
            } finally {
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
    
    fun exportLogs(context: Context, logsToExport: List<LogEntry>) {
        val content = logsToExport.joinToString("\n") { it.toFormattedString() }
        val sdf = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
        val filename = "Vianbrow_log_${sdf.format(Date())}.txt"
        saveFileToDownloads(context, filename, content)
    }

    private fun saveFileToDownloads(context: Context, filename: String, content: String) {
        try {
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { resolver.openOutputStream(it) }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, filename)
                outputStream = FileOutputStream(file)
            }
            
            outputStream?.use { it.write(content.toByteArray()) }
            Log.i("VianbrowLogger", "Saved file to Downloads: $filename")
        } catch (e: Exception) {
            Log.e("VianbrowLogger", "Failed to save file to downloads", e)
        }
    }
}
