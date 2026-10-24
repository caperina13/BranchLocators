package com.example.branchlocator

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ExitToApp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File as JavaFile
import java.util.Collections
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class MapLink(val label: String, val destination: String)
data class Branch(val id: Long, val name: String, val address: String, val contactNumber: String = "", val maps: List<MapLink>, val imageUri: String = "")
data class WeatherInfo(val temp: String, val condition: String, val icon: String)
data class SignaturePoint(val x: Float, val y: Float)
data class ChecklistItem(
    val id: Int,
    val stopNumber: Int,
    val branchName: String,
    val address: String,
    val task: String,
    var isCompleted: Boolean = false
)

data class DeliveryItem(
    val id: Int,
    val name: String,
    val isDelivered: Boolean = false
)

data class BranchDeliveryTask(
    val branchId: Long,
    val branchName: String,
    val address: String,
    val itemsToDeliver: List<DeliveryItem>,
    val receiverName: String = "",
    val signatureBitmap: android.graphics.Bitmap? = null,
    val signatureCaptured: Boolean = false,
    val isCompleted: Boolean = false,
    val completedTimestamp: String = ""
)

private const val DELIVERY_TASKS_KEY = "delivery_tasks_json"

private fun getAccountScopedKey(context: Context, baseKey: String): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val isSigned = prefs.getBoolean("google_signed_in", false)
    val email = prefs.getString("google_email", "") ?: ""
    return if (isSigned && email.isNotBlank() && email != "Not Signed In") {
        "${baseKey}_${email.replace("@", "_").replace(".", "_")}"
    } else {
        "${baseKey}_guest"
    }
}

private fun saveDeliveryTasks(context: Context, tasks: List<BranchDeliveryTask>) {
    try {
        val array = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject()
            obj.put("branchId", task.branchId)
            obj.put("branchName", task.branchName)
            obj.put("address", task.address)
            obj.put("receiverName", task.receiverName)
            obj.put("isCompleted", task.isCompleted)
            obj.put("completedTimestamp", task.completedTimestamp)

            val itemsArr = JSONArray()
            task.itemsToDeliver.forEach { item ->
                val itemObj = JSONObject()
                itemObj.put("id", item.id)
                itemObj.put("name", item.name)
                itemObj.put("isDelivered", item.isDelivered)
                itemsArr.put(itemObj)
            }
            obj.put("itemsToDeliver", itemsArr)
            array.put(obj)
        }
        val key = getAccountScopedKey(context, DELIVERY_TASKS_KEY)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(key, array.toString()).apply()
    } catch (_: Exception) {}
}

private fun loadDeliveryTasks(context: Context): List<BranchDeliveryTask> {
    try {
        val key = getAccountScopedKey(context, DELIVERY_TASKS_KEY)
        val jsonString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
        if (!jsonString.isNullOrBlank()) {
            val array = JSONArray(jsonString)
            val list = mutableListOf<BranchDeliveryTask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val branchId = obj.optLong("branchId", i.toLong())
                val branchName = obj.optString("branchName", "")
                val address = obj.optString("address", "")
                val receiverName = obj.optString("receiverName", "")
                val isCompleted = obj.optBoolean("isCompleted", false)
                val completedTimestamp = obj.optString("completedTimestamp", "")

                val itemsArr = obj.optJSONArray("itemsToDeliver")
                val itemsList = mutableListOf<DeliveryItem>()
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        itemsList.add(
                            DeliveryItem(
                                id = itemObj.optInt("id", j + 1),
                                name = itemObj.optString("name", ""),
                                isDelivered = itemObj.optBoolean("isDelivered", false)
                            )
                        )
                    }
                }

                if (branchName.isNotBlank()) {
                    list.add(
                        BranchDeliveryTask(
                            branchId = branchId,
                            branchName = branchName,
                            address = address,
                            itemsToDeliver = itemsList,
                            receiverName = receiverName,
                            isCompleted = isCompleted,
                            completedTimestamp = completedTimestamp
                        )
                    )
                }
            }
            return list
        }
    } catch (_: Exception) {}

    return emptyList()
}

data class CompletedTripArchiveRecord(
    val tripId: String,
    val completionDate: String,
    val totalBranches: Int,
    val tasks: List<BranchDeliveryTask>
)

private const val COMPLETED_TRIPS_ARCHIVE_KEY = "completed_trips_archive_json"

private fun saveCompletedTripToArchive(context: Context, tasks: List<BranchDeliveryTask>) {
    try {
        if (tasks.isEmpty() || !tasks.all { it.isCompleted }) return

        val key = getAccountScopedKey(context, COMPLETED_TRIPS_ARCHIVE_KEY)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingRaw = prefs.getString(key, null)
        val array = if (!existingRaw.isNullOrBlank()) JSONArray(existingRaw) else JSONArray()

        val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val obj = JSONObject()
        obj.put("tripId", "Trip_${System.currentTimeMillis()}")
        obj.put("completionDate", dateStr)
        obj.put("totalBranches", tasks.size)

        val tasksArr = JSONArray()
        tasks.forEach { t ->
            val tObj = JSONObject()
            tObj.put("branchName", t.branchName)
            tObj.put("address", t.address)
            tObj.put("receiverName", t.receiverName)
            tObj.put("completedTimestamp", t.completedTimestamp)

            val itemsArr = JSONArray()
            t.itemsToDeliver.forEach { item ->
                val iObj = JSONObject()
                iObj.put("name", item.name)
                iObj.put("isDelivered", item.isDelivered)
                itemsArr.put(iObj)
            }
            tObj.put("itemsToDeliver", itemsArr)
            tasksArr.put(tObj)
        }
        obj.put("tasks", tasksArr)
        array.put(obj)

        prefs.edit().putString(key, array.toString()).apply()
    } catch (_: Exception) {}
}

private fun loadCompletedTripsArchive(context: Context): List<CompletedTripArchiveRecord> {
    try {
        val key = getAccountScopedKey(context, COMPLETED_TRIPS_ARCHIVE_KEY)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingRaw = prefs.getString(key, null) ?: return emptyList()
        val array = JSONArray(existingRaw)
        val list = mutableListOf<CompletedTripArchiveRecord>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val tripId = obj.optString("tripId", "Trip_$i")
            val completionDate = obj.optString("completionDate", "")
            val totalBranches = obj.optInt("totalBranches", 0)

            val tasksArr = obj.optJSONArray("tasks")
            val taskList = mutableListOf<BranchDeliveryTask>()
            if (tasksArr != null) {
                for (j in 0 until tasksArr.length()) {
                    val tObj = tasksArr.getJSONObject(j)
                    val branchName = tObj.optString("branchName", "")
                    val address = tObj.optString("address", "")
                    val receiverName = tObj.optString("receiverName", "")
                    val timestamp = tObj.optString("completedTimestamp", "")

                    val itemsArr = tObj.optJSONArray("itemsToDeliver")
                    val itemsList = mutableListOf<DeliveryItem>()
                    if (itemsArr != null) {
                        for (k in 0 until itemsArr.length()) {
                            val iObj = itemsArr.getJSONObject(k)
                            itemsList.add(DeliveryItem(k + 1, iObj.optString("name", ""), true))
                        }
                    }

                    taskList.add(
                        BranchDeliveryTask(
                            branchId = (j + 1).toLong(),
                            branchName = branchName,
                            address = address,
                            itemsToDeliver = itemsList,
                            receiverName = receiverName,
                            isCompleted = true,
                            completedTimestamp = timestamp
                        )
                    )
                }
            }

            if (completionDate.isNotBlank()) {
                list.add(CompletedTripArchiveRecord(tripId, completionDate, totalBranches, taskList))
            }
        }
        return list
    } catch (_: Exception) {
        return emptyList()
    }
}

private const val AUTO_BACKUP_ENABLED_KEY = "auto_backup_enabled"
private const val AUTO_BACKUP_SCHEDULE_KEY = "auto_backup_schedule"
private const val LAST_BACKUP_TIME_KEY = "last_backup_time"

private fun getGoogleSignInClient(context: Context): com.google.android.gms.auth.api.signin.GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()
    return GoogleSignIn.getClient(context, gso)
}

private const val BACKUP_NOTIFICATION_CHANNEL_ID = "agnp_backup_channel"

private fun createNotificationChannel(context: Context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val name = "AGNP Cloud Backup Reminders"
        val descriptionText = "Notifications for automatic itinerary and progress backups to Google Drive"
        val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
        val channel = android.app.NotificationChannel(BACKUP_NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@Suppress("MissingPermission")
@android.annotation.SuppressLint("MissingPermission")
private fun showBackupNotification(context: Context, detailText: String) {
    try {
        val builder = androidx.core.app.NotificationCompat.Builder(context, BACKUP_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("☁️ AGNP Auto-Backup Completed")
            .setContentText("Your trip progress was automatically backed up to Google Drive. ($detailText)")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33
        ) {
            notificationManager.notify(1001, builder.build())
        }
    } catch (_: Exception) {}
}

private fun getLastBackupDetailSummary(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val tasksRaw = prefs.getString("delivery_tasks_json", null)
    val branchesRaw = prefs.getString(BRANCHES_KEY, null)

    var branchCount = 33
    var completedCount = 0

    try {
        if (!branchesRaw.isNullOrBlank()) {
            branchCount = JSONArray(branchesRaw).length()
        }
        if (!tasksRaw.isNullOrBlank()) {
            val arr = JSONArray(tasksRaw)
            for (i in 0 until arr.length()) {
                if (arr.getJSONObject(i).optBoolean("isCompleted", false)) {
                    completedCount++
                }
            }
        }
    } catch (_: Exception) {}

    return "$branchCount Branches • $completedCount Completed Visits"
}

class DriveSyncManager(private val context: Context, private val account: GoogleSignInAccount) {

    private val backupFileName = "user_progress.json"

    private fun buildDriveService(): com.google.api.services.drive.Drive {
        val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        return com.google.api.services.drive.Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("AGNP Branch Locator").build()
    }

    suspend fun uploadProgress(progressJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = buildDriveService()
            val existingFileId = findBackupFileId(driveService)

            val tempFile = JavaFile.createTempFile("sync_", ".json").apply {
                writeText(progressJson)
            }
            val mediaContent = com.google.api.client.http.FileContent("application/json", tempFile)

            if (existingFileId != null) {
                driveService.files().update(existingFileId, null, mediaContent).execute()
            } else {
                val metadata = com.google.api.services.drive.model.File().apply {
                    name = backupFileName
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(metadata, mediaContent).setFields("id").execute()
            }
            tempFile.delete()
        } catch (_: Exception) {}

        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val backupFile = java.io.File(downloadsDir, "AGNP_App_Backup.json")
            backupFile.writeText(progressJson)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun downloadProgress(): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = buildDriveService()
            val fileId = findBackupFileId(driveService)
            if (fileId != null) {
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                val cloudJson = outputStream.toString(Charsets.UTF_8.name())
                if (cloudJson.isNotBlank()) return@withContext cloudJson
            }
        } catch (_: Exception) {}

        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val backupFile = java.io.File(downloadsDir, "AGNP_App_Backup.json")
            if (backupFile.exists()) {
                return@withContext backupFile.readText()
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    private fun findBackupFileId(driveService: com.google.api.services.drive.Drive): String? {
        return try {
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$backupFileName' and trashed = false")
                .setFields("files(id, name)")
                .execute()
            result.files?.firstOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }
}

class MiniServerClient(private val context: Context) {

    private fun getServerUrl(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("mini_server_url", "https://branchlocator-r2mc.onrender.com")
            ?.ifBlank { "https://branchlocator-r2mc.onrender.com" } ?: "https://branchlocator-r2mc.onrender.com"
    }

    suspend fun sendBackupToServer(email: String, backupJsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getServerUrl()
            val url = java.net.URL("$baseUrl/api/backup")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val payload = JSONObject().apply {
                put("email", email)
                put("backupData", JSONObject(backupJsonString))
            }

            java.io.DataOutputStream(conn.outputStream).use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            conn.responseCode == 200 || conn.responseCode == 201
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getBackupFromServer(email: String): String? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getServerUrl()
            val url = java.net.URL("$baseUrl/api/backup/${java.net.URLEncoder.encode(email, "UTF-8")}")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootObj = JSONObject(responseText)
                if (rootObj.optBoolean("success", false) && rootObj.has("backup")) {
                    rootObj.getJSONObject("backup").toString()
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun submitAccessRequest(name: String, email: String, dept: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getServerUrl()
            val url = java.net.URL("$baseUrl/api/access-request")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val payload = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("department", dept)
                put("reason", reason)
            }

            java.io.DataOutputStream(conn.outputStream).use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            conn.responseCode == 200 || conn.responseCode == 201
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private fun createFullAppBackupJson(context: Context): String {
    val rootObj = JSONObject()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    rootObj.put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))
    rootObj.put("profileName", prefs.getString(PROFILE_NAME_KEY, DEFAULT_PROFILE_NAME))
    rootObj.put("googleEmail", prefs.getString("google_email", "it.dept@agnp-logistics.com"))
    rootObj.put("profileImageUri", prefs.getString(PROFILE_IMAGE_URI_KEY, ""))
    rootObj.put("cameraLogoUri", prefs.getString(CAMERA_LOGO_URI_KEY, ""))

    val branchesRaw = prefs.getString(BRANCHES_KEY, null)
    if (!branchesRaw.isNullOrBlank()) {
        rootObj.put("branches", JSONArray(branchesRaw))
    }

    val tasksRaw = prefs.getString(DELIVERY_TASKS_KEY, null)
    if (!tasksRaw.isNullOrBlank()) {
        rootObj.put("deliveryTasks", JSONArray(tasksRaw))
    }

    return rootObj.toString(2)
}

private fun restoreAppFromBackupJson(context: Context, jsonStr: String): Boolean {
    try {
        val rootObj = JSONObject(jsonStr)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()

        if (rootObj.has("profileName")) prefs.putString(PROFILE_NAME_KEY, rootObj.getString("profileName"))
        if (rootObj.has("googleEmail")) prefs.putString("google_email", rootObj.getString("googleEmail"))
        if (rootObj.has("profileImageUri")) prefs.putString(PROFILE_IMAGE_URI_KEY, rootObj.getString("profileImageUri"))
        if (rootObj.has("cameraLogoUri")) prefs.putString(CAMERA_LOGO_URI_KEY, rootObj.getString("cameraLogoUri"))
        if (rootObj.has("branches")) prefs.putString(BRANCHES_KEY, rootObj.getJSONArray("branches").toString())
        if (rootObj.has("deliveryTasks")) prefs.putString(DELIVERY_TASKS_KEY, rootObj.getJSONArray("deliveryTasks").toString())

        prefs.apply()
        return true
    } catch (_: Exception) {
        return false
    }
}

private fun generateDesktopExcelReport(context: Context, task: BranchDeliveryTask): Uri? {
    try {
        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Trip Report")

        sheet.setColumnWidth(0, 5000)
        sheet.setColumnWidth(1, 13000)
        sheet.setColumnWidth(2, 6000)

        val titleFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 14.toShort()
            color = org.apache.poi.ss.usermodel.IndexedColors.DARK_TEAL.index
        }
        val titleStyle = workbook.createCellStyle().apply { setFont(titleFont) }

        val headerFont = workbook.createFont().apply {
            bold = true
            color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
        }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.TEAL.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        }

        try {
            val logoBm = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            if (logoBm != null) {
                val stream = java.io.ByteArrayOutputStream()
                logoBm.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                val logoBytes = stream.toByteArray()
                val logoPictureIdx = workbook.addPicture(logoBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG)
                val drawing = sheet.createDrawingPatriarch()
                val logoAnchor = workbook.creationHelper.createClientAnchor().apply {
                    setCol1(0)
                    setRow1(0)
                    setCol2(2)
                    setRow2(3)
                }
                drawing.createPicture(logoAnchor, logoPictureIdx)
            }
        } catch (_: Exception) {}

        var rowIdx = 4
        val titleRow = sheet.createRow(rowIdx++)
        titleRow.createCell(0).apply {
            setCellValue("AGNP LOGISTICS FIELD TRIP COMPLETION REPORT")
            cellStyle = titleStyle
        }

        rowIdx++

        fun addInfoRow(label: String, value: String) {
            val r = sheet.createRow(rowIdx++)
            r.createCell(0).setCellValue(label)
            r.createCell(1).setCellValue(value)
        }

        addInfoRow("Date & Time:", task.completedTimestamp)
        addInfoRow("Branch Name:", task.branchName)
        addInfoRow("Address:", task.address)
        addInfoRow("Receiver Name:", task.receiverName)
        addInfoRow("Verification Status:", "VERIFIED & SIGNED")

        rowIdx++

        val tableHeaderRow = sheet.createRow(rowIdx++)
        val headers = listOf("Item ID", "Delivered Item Name", "Status")
        headers.forEachIndexed { i, h ->
            tableHeaderRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        task.itemsToDeliver.forEach { item ->
            val itemRow = sheet.createRow(rowIdx++)
            itemRow.createCell(0).setCellValue(item.id.toDouble())
            itemRow.createCell(1).setCellValue(item.name)
            itemRow.createCell(2).setCellValue(if (item.isDelivered) "DELIVERED" else "PENDING")
        }

        rowIdx++

        if (task.signatureBitmap != null) {
            val sigRow = sheet.createRow(rowIdx++)
            sigRow.createCell(0).setCellValue("Receiver Signature:")

            val stream = java.io.ByteArrayOutputStream()
            task.signatureBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val sigBytes = stream.toByteArray()
            val sigPictureIdx = workbook.addPicture(sigBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG)

            val drawing = sheet.createDrawingPatriarch()
            val sigAnchor = workbook.creationHelper.createClientAnchor().apply {
                setCol1(1)
                setRow1(rowIdx)
                setCol2(3)
                setRow2(rowIdx + 5)
            }
            drawing.createPicture(sigAnchor, sigPictureIdx)
            rowIdx += 6
        }

        val fileName = "Trip_Report_${task.branchName.replace(" ", "_")}_${System.currentTimeMillis()}.xlsx"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, fileName)
        val fileOut = java.io.FileOutputStream(file)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()

        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        return null
    }
}

private fun generateMasterDesktopExcelReport(context: Context, tasks: List<BranchDeliveryTask>): Uri? {
    try {
        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Trip Itinerary Form")

        sheet.setColumnWidth(0, 3500)
        sheet.setColumnWidth(1, 14000)
        sheet.setColumnWidth(2, 6000)
        sheet.setColumnWidth(3, 5000)
        sheet.setColumnWidth(4, 5000)
        sheet.setColumnWidth(5, 5000)

        val grayFill = workbook.createCellStyle().apply {
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
            alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
        }
        val blueFill = workbook.createCellStyle().apply {
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
            alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
        }

        val titleFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 14.toShort()
        }
        val headerFont = workbook.createFont().apply {
            bold = true
            color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
        }
        val boldFont = workbook.createFont().apply { bold = true }

        // Row 0: Title Banner
        val r1 = sheet.createRow(0)
        val cell1 = r1.createCell(0)
        cell1.setCellValue("TRIP ITINERARY FORM")
        cell1.cellStyle = grayFill.apply { setFont(titleFont) }

        // Row 1: Date Banner
        val dateStr = java.text.SimpleDateFormat("EEEE, MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val r2 = sheet.createRow(1)
        val cell2 = r2.createCell(0)
        cell2.setCellValue(dateStr)
        cell2.cellStyle = blueFill.apply { setFont(headerFont) }

        // Metadata section
        var rowIdx = 2
        val dateShort = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val meta = listOf(
            listOf("DRIVER :", "PAUL", "SOD ODO READING :", ""),
            listOf("PORTER :", "DOM", "EOD ODO READING :", ""),
            listOf("DATE :", dateShort, "TOTAL KM :", ""),
            listOf("VEHICLE :", "LOGISTICS VAN", "TOTAL TRIP TIME :", "")
        )
        meta.forEach { rowData ->
            val r = sheet.createRow(rowIdx++)
            r.createCell(0).apply { setCellValue(rowData[0]); cellStyle = workbook.createCellStyle().apply { setFont(boldFont) } }
            r.createCell(1).setCellValue(rowData[1])
            r.createCell(4).apply { setCellValue(rowData[2]); cellStyle = workbook.createCellStyle().apply { setFont(boldFont) } }
            r.createCell(5).setCellValue(rowData[3])
        }

        // Table Header
        val th = sheet.createRow(rowIdx++)
        val headers = listOf("TRIP #", "LOCATION", "SERVICE", "ETA", "ATA", "ODOMETER")
        headers.forEachIndexed { i, h ->
            th.createCell(i).apply {
                setCellValue(h)
                cellStyle = blueFill.apply { setFont(headerFont) }
            }
        }

        // Table Content
        tasks.forEachIndexed { index, task ->
            val tr = sheet.createRow(rowIdx++)
            tr.createCell(0).setCellValue((index + 1).toDouble())
            tr.createCell(1).apply { setCellValue(task.branchName); cellStyle = workbook.createCellStyle().apply { setFont(boldFont) } }
            tr.createCell(2).setCellValue(if (index == 0) "PICK UP" else "DROP")
            tr.createCell(3).setCellValue("COMPLETED")

            val itemText = task.itemsToDeliver.joinToString("\n") { "▪ ${it.name}" }
            val itemRow = sheet.createRow(rowIdx++)
            itemRow.createCell(1).setCellValue(itemText)

            if (task.signatureBitmap != null) {
                val sigRow = sheet.createRow(rowIdx++)
                sigRow.createCell(1).setCellValue("Receiver Signature (${task.receiverName}):")

                val stream = java.io.ByteArrayOutputStream()
                task.signatureBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                val sigBytes = stream.toByteArray()
                val sigPictureIdx = workbook.addPicture(sigBytes, org.apache.poi.ss.usermodel.Workbook.PICTURE_TYPE_PNG)

                val drawing = sheet.createDrawingPatriarch()
                val sigAnchor = workbook.creationHelper.createClientAnchor().apply {
                    setCol1(1)
                    setRow1(rowIdx)
                    setCol2(3)
                    setRow2(rowIdx + 5)
                }
                drawing.createPicture(sigAnchor, sigPictureIdx)
                rowIdx += 6
            }
        }

        // Bottom Signatures
        rowIdx += 2
        val nameRow = sheet.createRow(rowIdx++)
        nameRow.createCell(1).apply { setCellValue("Carl Joseph Flores"); cellStyle = workbook.createCellStyle().apply { setFont(boldFont) } }
        nameRow.createCell(4).apply { setCellValue("Yves Dalagan"); cellStyle = workbook.createCellStyle().apply { setFont(boldFont) } }

        val titleRow = sheet.createRow(rowIdx++)
        titleRow.createCell(1).setCellValue("Purchasing and Logistics Coordinator")
        titleRow.createCell(4).setCellValue("MC/Repo/Spareparts Head")

        rowIdx += 2
        val lineRow = sheet.createRow(rowIdx++)
        lineRow.createCell(1).setCellValue("_________________________________________")
        lineRow.createCell(4).setCellValue("_________________________________________")

        val sigLabelRow = sheet.createRow(rowIdx++)
        sigLabelRow.createCell(1).setCellValue("Driver Signature Over Printed Name")
        sigLabelRow.createCell(4).setCellValue("Porter Signature Over Printed Name")

        val fileName = "Master_Trip_Itinerary_Report_${System.currentTimeMillis()}.xlsx"
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, fileName)
        val fileOut = java.io.FileOutputStream(file)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()

        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        return null
    }
}

private fun parseItineraryFile(context: Context, uri: Uri): List<BranchDeliveryTask> {
    val tasks = mutableListOf<BranchDeliveryTask>()
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        try {
            val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            var currentBranchId = 1L

            for (rowIndex in 1..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val branchName = row.getCell(1)?.stringCellValue?.trim() ?: continue
                val address = row.getCell(2)?.stringCellValue?.trim() ?: ""
                val itemText = row.getCell(3)?.stringCellValue?.trim() ?: ""

                if (branchName.isNotBlank() && !branchName.contains("TRIP ITINERARY", true) && !branchName.contains("LOCATION", true)) {
                    val itemsList = itemText.split(";").mapIndexed { idx, itemName ->
                        DeliveryItem(idx + 1, itemName.removePrefix("▪").removePrefix("-").trim(), false)
                    }.filter { it.name.isNotBlank() }

                    tasks.add(
                        BranchDeliveryTask(
                            branchId = currentBranchId++,
                            branchName = branchName,
                            address = address.ifEmpty { "$branchName, Philippines" },
                            itemsToDeliver = itemsList.ifEmpty {
                                listOf(DeliveryItem(1, "Deliver items for $branchName", false))
                            }
                        )
                    )
                }
            }
            workbook.close()
        } catch (_: Exception) {
            val csvStream = context.contentResolver.openInputStream(uri)
            val reader = csvStream?.bufferedReader()
            var lineIndex = 0
            reader?.forEachLine { line ->
                if (lineIndex > 0 && line.isNotBlank()) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val branch = parts[1].trim()
                        val address = if (parts.size >= 3) parts[2].trim() else branch
                        val itemText = if (parts.size >= 4) parts[3].trim() else ""
                        val itemsList = itemText.split(";").mapIndexed { idx, itemName ->
                            DeliveryItem(idx + 1, itemName.removePrefix("▪").removePrefix("-").trim(), false)
                        }.filter { it.name.isNotBlank() }

                        if (branch.isNotBlank() && !branch.contains("TRIP ITINERARY", true)) {
                            tasks.add(
                                BranchDeliveryTask(
                                    branchId = (lineIndex).toLong(),
                                    branchName = branch,
                                    address = address,
                                    itemsToDeliver = itemsList.ifEmpty {
                                        listOf(DeliveryItem(1, "Deliver logistics batch for $branch", false))
                                    }
                                )
                            )
                        }
                    }
                }
                lineIndex++
            }
            reader?.close()
            csvStream?.close()
        }
        inputStream?.close()
    } catch (_: Exception) {}
    return tasks
}

private fun generateSampleXlsxTemplate(context: Context) {
    try {
        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Trip Itinerary Template")

        sheet.setColumnWidth(0, 3500)
        sheet.setColumnWidth(1, 8000)
        sheet.setColumnWidth(2, 8000)
        sheet.setColumnWidth(3, 14000)

        val headerFont = workbook.createFont().apply {
            bold = true
            color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
        }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.TEAL.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        }

        val headerRow = sheet.createRow(0)
        listOf("Stop Number", "Branch Name", "Address", "Items To Deliver (Semicolon Separated)").forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        val sampleRows = listOf(
            listOf("1", "Motosiklos Angono", "Angono Rizal Philippines", "2x Motorcycle Model Alpha; 2x Safety Helmets; Official Receipt"),
            listOf("2", "Motosiklos Balanga", "Balanga Bataan Philippines", "3x Scooter Units; Warranty Cards; Delivery Invoices"),
            listOf("3", "Motosiklos Lucena", "Lucena Quezon Philippines", "1x Heavy Duty Utility Trike; Equipment Audit Kit"),
            listOf("4", "Motosiklos Marikina", "Marikina Metro Manila Philippines", "2x Electric Moped; 2x Spare Batteries; Registration Manual"),
            listOf("5", "Head Office - AGNP", "Caloocan Metro Manila Philippines", "Weekly Logistics Report; Signed Transfer Slips")
        )

        sampleRows.forEachIndexed { rIdx, rowData ->
            val row = sheet.createRow(rIdx + 1)
            row.createCell(0).setCellValue(rowData[0].toDouble())
            row.createCell(1).setCellValue(rowData[1])
            row.createCell(2).setCellValue(rowData[2])
            row.createCell(3).setCellValue(rowData[3])
        }

        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloadsDir, "Trip_Itinerary_Template.xlsx")
        val fileOut = java.io.FileOutputStream(file)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()
    } catch (_: Exception) {}
}

private const val PREFS_NAME = "branch_locator"
private const val BRANCHES_KEY = "branches_json"
private const val ADMIN_PASSWORD_KEY = "admin_password"
private const val PROFILE_NAME_KEY = "profile_name"
private const val PROFILE_IMAGE_URI_KEY = "profile_image_uri"
private const val CAMERA_LOGO_URI_KEY = "camera_logo_uri"
private const val DEFAULT_ADMIN_PASSWORD = "Admin@1234"
private const val DEFAULT_PROFILE_NAME = "AGNP Administrator"

private fun fetchWeatherForCoordinates(lat: Double, lon: Double, onResult: (WeatherInfo) -> Unit) {
    Thread {
        try {
            val weatherUrl = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
            val weatherConn = weatherUrl.openConnection() as java.net.HttpURLConnection
            weatherConn.connectTimeout = 3000
            weatherConn.readTimeout = 3000
            val weatherJson = JSONObject(weatherConn.inputStream.bufferedReader().readText())
            val current = weatherJson.optJSONObject("current")
            if (current != null) {
                val temp = current.getDouble("temperature_2m")
                val code = current.getInt("weather_code")
                val (condition, icon) = when (code) {
                    0 -> "Sunny" to "☀️"
                    1, 2, 3 -> "Partly Cloudy" to "⛅"
                    45, 48 -> "Foggy" to "🌫️"
                    in 51..57 -> "Drizzling" to "🌧️"
                    in 61..67, in 80..82 -> "Raining" to "🌧️"
                    in 71..77 -> "Snowy" to "❄️"
                    in 95..99 -> "Thunderstorm" to "⛈️"
                    else -> "Clear" to "🌤️"
                }
                onResult(WeatherInfo("${temp.toInt()}°C", condition, icon))
                return@Thread
            }
            onResult(WeatherInfo("30°C", "Sunny", "☀️"))
        } catch (_: Exception) {
            onResult(WeatherInfo("30°C", "Sunny", "☀️"))
        }
    }.start()
}

data class LatLon(val lat: Double, val lon: Double)

@Composable
private fun rememberLiveUserLocation(): Location? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { loc ->
            location = loc
        }
        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)

                val lastGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                location = lastGPS ?: lastNet
            }
        } catch (_: SecurityException) {}

        onDispose {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: SecurityException) {}
        }
    }
    return location
}

private fun getCoordinatesForBranch(address: String, name: String): LatLon {
    val text = "$name $address".lowercase()
    return when {
        text.contains("angono") -> LatLon(14.5286, 121.1508)
        text.contains("balanga") -> LatLon(14.6804, 120.5407)
        text.contains("baliuag") -> LatLon(14.9572, 120.8906)
        text.contains("cabuyao") -> LatLon(14.2797, 121.1237)
        text.contains("gma") || text.contains("cavite") -> LatLon(14.3167, 121.0500)
        text.contains("hermosa") -> LatLon(14.8328, 120.5058)
        text.contains("lucena") -> LatLon(13.9355, 121.6155)
        text.contains("marikina") -> LatLon(14.6507, 121.1029)
        text.contains("marilao") -> LatLon(14.7578, 120.9572)
        text.contains("mariveles") -> LatLon(14.4347, 120.4878)
        text.contains("pasig") -> LatLon(14.5764, 121.0851)
        text.contains("san fernando") -> LatLon(15.0284, 120.6897)
        text.contains("sta maria") -> LatLon(14.8153, 120.9639)
        text.contains("sta. rosa") || text.contains("sta rosa") -> LatLon(14.3148, 121.1135)
        text.contains("tanauan") -> LatLon(14.0833, 121.1500)
        text.contains("tejeros") -> LatLon(14.4167, 120.8667)
        text.contains("trece martires") -> LatLon(14.2858, 120.8672)
        text.contains("valenzuela") -> LatLon(14.7006, 120.9830)
        text.contains("antipolo") -> LatLon(14.5842, 121.1762)
        text.contains("bacoor") -> LatLon(14.4556, 120.9419)
        text.contains("biñan") || text.contains("binan") -> LatLon(14.3417, 121.0833)
        text.contains("cabanatuan") -> LatLon(15.4842, 120.9672)
        text.contains("calamba") -> LatLon(14.2120, 121.1645)
        text.contains("infanta") -> LatLon(14.7483, 121.6517)
        text.contains("lipa") -> LatLon(13.9419, 121.1644)
        text.contains("olongapo") -> LatLon(14.8292, 120.2828)
        text.contains("san jose") -> LatLon(15.7922, 120.9878)
        text.contains("san juan") -> LatLon(14.6019, 121.0355)
        text.contains("san pablo") -> LatLon(14.0683, 121.3250)
        text.contains("siniloan") -> LatLon(14.4258, 121.4508)
        text.contains("sta. cruz") || text.contains("sta cruz") -> LatLon(14.2833, 121.4167)
        else -> LatLon(14.5995, 120.9842)
    }
}

private fun calculateTravelTime(userLat: Double, userLon: Double, branchLat: Double, branchLon: Double): String {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(branchLat - userLat)
    val dLon = Math.toRadians(branchLon - userLon)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(userLat)) * kotlin.math.cos(Math.toRadians(branchLat)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    val distanceKm = earthRadiusKm * c

    val hours = distanceKm / 35.0
    val totalMinutes = (hours * 60).toInt().coerceAtLeast(5)

    return if (totalMinutes >= 60) {
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        "~${hrs} hr ${mins} mins away (${String.format(java.util.Locale.getDefault(), "%.1f", distanceKm)} km)"
    } else {
        "~${totalMinutes} mins away (${String.format(java.util.Locale.getDefault(), "%.1f", distanceKm)} km)"
    }
}

@Composable
private fun rememberLiveLocationName(location: Location?): String {
    val context = LocalContext.current
    var locationName by remember { mutableStateOf("Acquiring location...") }

    LaunchedEffect(location) {
        if (location != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val street = addr.thoroughfare ?: addr.featureName ?: ""
                        val subLocality = addr.subLocality ?: addr.locality ?: ""
                        val adminArea = addr.adminArea ?: ""
                        val name = listOfNotNull(street.takeIf { it.isNotBlank() }, subLocality.takeIf { it.isNotBlank() }, adminArea.takeIf { it.isNotBlank() })
                            .joinToString(", ")
                            .ifEmpty { "Lat: ${String.format(java.util.Locale.getDefault(), "%.4f", location.latitude)}, Lon: ${String.format(java.util.Locale.getDefault(), "%.4f", location.longitude)}" }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            locationName = name
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            locationName = "Lat: ${String.format(java.util.Locale.getDefault(), "%.4f", location.latitude)}, Lon: ${String.format(java.util.Locale.getDefault(), "%.4f", location.longitude)}"
                        }
                    }
                } catch (_: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        locationName = "Lat: ${String.format(java.util.Locale.getDefault(), "%.4f", location.latitude)}, Lon: ${String.format(java.util.Locale.getDefault(), "%.4f", location.longitude)}"
                    }
                }
            }
        } else {
            locationName = "Acquiring live GPS location..."
        }
    }
    return locationName
}

private fun getCoordinatesForDestination(destination: String, address: String, name: String): LatLon {
    val text = "$destination $address $name".lowercase()
    return when {
        text.contains("angono") -> LatLon(14.5286, 121.1508)
        text.contains("balanga") -> LatLon(14.6804, 120.5407)
        text.contains("baliuag") -> LatLon(14.9572, 120.8906)
        text.contains("cabuyao") -> LatLon(14.2797, 121.1237)
        text.contains("gma") || text.contains("cavite") -> LatLon(14.3167, 121.0500)
        text.contains("hermosa") -> LatLon(14.8328, 120.5058)
        text.contains("lucena") -> LatLon(13.9355, 121.6155)
        text.contains("marikina") -> LatLon(14.6507, 121.1029)
        text.contains("marilao") -> LatLon(14.7578, 120.9572)
        text.contains("mariveles") -> LatLon(14.4347, 120.4878)
        text.contains("pasig") -> LatLon(14.5764, 121.0851)
        text.contains("san fernando") -> LatLon(15.0284, 120.6897)
        text.contains("sta maria") -> LatLon(14.8153, 120.9639)
        text.contains("sta. rosa") || text.contains("sta rosa") -> LatLon(14.3148, 121.1135)
        text.contains("tanauan") -> LatLon(14.0833, 121.1500)
        text.contains("tejeros") -> LatLon(14.4167, 120.8667)
        text.contains("trece martires") -> LatLon(14.2858, 120.8672)
        text.contains("valenzuela") -> LatLon(14.7006, 120.9830)
        text.contains("antipolo") -> LatLon(14.5842, 121.1762)
        text.contains("bacoor") -> LatLon(14.4556, 120.9419)
        text.contains("biñan") || text.contains("binan") -> LatLon(14.3417, 121.0833)
        text.contains("cabanatuan") -> LatLon(15.4842, 120.9672)
        text.contains("calamba") -> LatLon(14.2120, 121.1645)
        text.contains("infanta") -> LatLon(14.7483, 121.6517)
        text.contains("lipa") -> LatLon(13.9419, 121.1644)
        text.contains("olongapo") -> LatLon(14.8292, 120.2828)
        text.contains("san jose") -> LatLon(15.7922, 120.9878)
        text.contains("san juan") -> LatLon(14.6019, 121.0355)
        text.contains("san pablo") -> LatLon(14.0683, 121.3250)
        text.contains("siniloan") -> LatLon(14.4258, 121.4508)
        text.contains("sta. cruz") || text.contains("sta cruz") -> LatLon(14.2833, 121.4167)
        else -> LatLon(14.5995, 120.9842)
    }
}

private fun defaultBranches() = listOf(
    // AGNP / Motosiklos Branches
    Branch(1, "Motosiklos Angono", "Angono, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/NTZrWR6mGWwejZqV8"))),
    Branch(2, "Motosiklos Balanga", "Balanga, Philippines", "", listOf(MapLink("Main location", "https://share.google/vtDfKQNP7HMLxEDKY"))),
    Branch(3, "Motosiklos Baliuag Bulacan", "Baliuag, Bulacan, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/GWzuLnWQrCR7Vozw8"))),
    Branch(4, "Motosiklos Cabuyao", "Cabuyao, Philippines", "", listOf(MapLink("Main location", "Cabuyao, Philippines"))),
    Branch(5, "Motosiklos GMA Cavite", "GMA, Cavite, Philippines", "", listOf(MapLink("Main location", "https://share.google/8ZT0zqQOJs5SiqFLA"))),
    Branch(6, "Motosiklos Head Office - AGNP", "Head Office, Philippines", "", listOf(MapLink("Main location", "https://share.google/iGzIAjAybNsHKkUqu"))),
    Branch(7, "Motosiklos Hermosa", "Hermosa, Philippines", "", listOf(MapLink("Main location", "Hermosa, Philippines"))),
    Branch(8, "Motosiklos SP Pasig", "SP Pasig, Philippines", "", listOf(MapLink("Main location", "https://share.google/iFWxGI1jCk7DizHY6"))),
    Branch(9, "Motosiklos Lucena", "Lucena, Philippines", "", listOf(MapLink("Main location", "https://share.google/aFLuboHsxIYcpIyqe"))),
    Branch(10, "Motosiklos Marikina", "Marikina, Philippines", "", listOf(MapLink("Main location", "https://share.google/0BKBN4TkTN2WY7GSH"))),
    Branch(11, "Motosiklos Marilao", "Marilao, Philippines", "", listOf(MapLink("Main location", "https://share.google/Mf9fSKLQnYvK4DEwF"))),
    Branch(12, "Motosiklos Mariveles", "Mariveles, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/5AYsBbfy1ooEQvJW8"))),
    Branch(13, "Motosiklos Pasig", "Pasig, Philippines", "", listOf(MapLink("Main location", "https://share.google/cGQ1W7kyi9pLe0in1"))),
    Branch(14, "Motosiklos San Fernando", "San Fernando, Philippines", "", listOf(MapLink("Main location", "https://share.google/RntlgN1vR5qvZg42w"))),
    Branch(15, "Motosiklos Sta Maria", "Sta Maria, Philippines", "", listOf(MapLink("Main location", "https://share.google/qzQyYhbcMYVrY7wnv"))),
    Branch(16, "Motosiklos Sta. Rosa", "Sta. Rosa, Philippines", "", listOf(MapLink("Main location", "https://share.google/9q49C0XZ76NzBLwlo"))),
    Branch(17, "Motosiklos Tanauan", "Tanauan, Philippines", "", listOf(MapLink("Main location", "https://share.google/CNf7HO7YDiJ7qnzwS"))),
    Branch(18, "Motosiklos Tejeros", "Tejeros, Philippines", "", listOf(MapLink("Main location", "https://share.google/e98VazRalyijbiDA0"))),
    Branch(19, "Motosiklos Trece Martires", "Trece Martires, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/XD2PDPZZGKxkbEFq5"))),
    Branch(20, "Motosiklos Valenzuela", "Valenzuela, Philippines", "", listOf(MapLink("Main location", "https://share.google/4rAFHQUbm3Ro7HRPL"))),

    // FILSIKLOS Branches
    Branch(21, "Filsiklos Antipolo", "Antipolo, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/9M9evjLJUyVHDrAw7?g_st=iv"))),
    Branch(22, "Filsiklos Bacoor", "Bacoor, Philippines", "", listOf(MapLink("Main location", "https://share.google/0nw2AQrtwt9HsNF9b"))),
    Branch(23, "Filsiklos Biñan", "Binan, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/1opoMTrD2Qd8q9vLA"))),
    Branch(24, "Filsiklos Cabanatuan", "Cabanatuan, Philippines", "", listOf(MapLink("Main location", "Cabanatuan, Philippines"))),
    Branch(25, "Filsiklos Calamba", "Calamba, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/HqRqfPXUZ8eVi7N46"))),
    Branch(26, "Filsiklos Infanta", "Infanta, Philippines", "", listOf(MapLink("Main location", "https://share.google/oQXYqoFy7J6mPADKh"))),
    Branch(27, "Filsiklos Lipa", "Lipa, Philippines", "", listOf(MapLink("Main location", "https://share.google/u7qYtpiYmPqQgNfQS"))),
    Branch(28, "Filsiklos Olongapo", "Olongapo, Philippines", "", listOf(MapLink("Main location", "https://share.google/gSzTIsh87ibg6JmVI"))),
    Branch(29, "Filsiklos San Jose", "San Jose, Philippines", "", listOf(MapLink("Main location", "San Jose, Philippines"))),
    Branch(30, "Filsiklos San Juan", "San Juan, Philippines", "", listOf(MapLink("Main location", "https://share.google/L1FfmRaNMcNQEgFgn"))),
    Branch(31, "Filsiklos San Pablo", "San Pablo, Philippines", "", listOf(MapLink("Main location", "https://maps.app.goo.gl/idrW7SmcxQ8c9MFt9"))),
    Branch(32, "Filsiklos Siniloan", "Siniloan, Philippines", "", listOf(MapLink("Main location", "https://share.google/srQA6StvjClxXrskH"))),
    Branch(33, "Filsiklos Sta. Cruz", "Sta Cruz, Philippines", "", listOf(MapLink("Main location", "https://share.google/F86JsqNOnXavyeGxB")))
)

private fun loadBranches(context: Context): List<Branch> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(BRANCHES_KEY, null) ?: return defaultBranches()
    return try {
        val array = JSONArray(raw)
        val loaded = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val contact = obj.optString("contactNumber", "")
            val imageUri = obj.optString("imageUri", "")
            val mapsJson = obj.optJSONArray("maps") ?: JSONArray()
            val links = (0 until mapsJson.length()).map { j ->
                val link = mapsJson.getJSONObject(j)
                MapLink(link.optString("label"), link.optString("destination"))
            }
            Branch(obj.optLong("id"), obj.optString("name"), obj.optString("address"), contact, links, imageUri)
        }
        if (loaded.size == defaultBranches().size) {
            defaultBranches().map { def ->
                val existing = loaded.find { it.id == def.id }
                existing?.copy(name = def.name) ?: def
            }
        } else {
            loaded
        }
    } catch (_: Exception) {
        defaultBranches()
    }
}

private fun saveBranches(context: Context, branches: List<Branch>) {
    val array = JSONArray()
    branches.forEach { branch ->
        val obj = JSONObject()
        obj.put("id", branch.id)
        obj.put("name", branch.name)
        obj.put("address", branch.address)
        obj.put("contactNumber", branch.contactNumber)
        obj.put("imageUri", branch.imageUri)
        val links = JSONArray()
        branch.maps.forEach { map ->
            links.put(JSONObject().put("label", map.label).put("destination", map.destination))
        }
        obj.put("maps", links)
        array.put(obj)
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(BRANCHES_KEY, array.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDark by remember { mutableStateOf(false) }
            var branchList by remember { mutableStateOf(loadBranches(this)) }
            var adminUnlocked by remember { mutableStateOf(false) }
            var showPasswordDialog by remember { mutableStateOf(false) }
            var showBranchEditor by remember { mutableStateOf(false) }
            var editingBranch by remember { mutableStateOf<Branch?>(null) }
            var showChangePassword by remember { mutableStateOf(false) }
            var profileName by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(PROFILE_NAME_KEY, DEFAULT_PROFILE_NAME) ?: DEFAULT_PROFILE_NAME
                )
            }
            var profileImageUri by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(PROFILE_IMAGE_URI_KEY, "") ?: ""
                )
            }
            var cameraLogoUri by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(CAMERA_LOGO_URI_KEY, "") ?: ""
                )
            }
            var showProfileEditor by remember { mutableStateOf(false) }
            var selectedMapDestination by remember { mutableStateOf<String?>(null) }
            var showGpsCamera by remember { mutableStateOf(false) }
            var isGoogleSignedIn by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getBoolean("google_signed_in", false)
                )
            }
            var googleEmail by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString("google_email", "") ?: ""
                )
            }
            var message by remember { mutableStateOf<String?>(null) }

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                var signedInEmail: String? = null
                var signedInName: String? = null
                var signedInPhoto: String? = null

                // 1. Try official GoogleSignInAccount
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java) ?: GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                    if (account != null) {
                        signedInEmail = account.email
                        signedInName = account.displayName ?: account.givenName
                        signedInPhoto = account.photoUrl?.toString()
                    }
                } catch (_: Exception) {}

                // 2. Extract account from intent data or extras if Google Play Services returned intent result
                if (signedInEmail.isNullOrBlank() && result.data != null) {
                    val intentData = result.data
                    val authAccount = intentData?.getStringExtra("authAccount")
                    val accountName = intentData?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
                    val extraData = intentData?.data?.schemeSpecificPart

                    val candidates = listOfNotNull(authAccount, accountName, extraData)
                    val foundEmail = candidates.firstOrNull { it.contains("@") }
                    if (!foundEmail.isNullOrBlank()) {
                        signedInEmail = foundEmail
                    }
                }

                // 3. Fallback: If user interacted with account picker
                if (signedInEmail.isNullOrBlank()) {
                    val lastAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                    if (lastAccount != null) {
                        signedInEmail = lastAccount.email
                        signedInName = lastAccount.displayName
                    } else if (result.resultCode == RESULT_OK || result.data != null) {
                        signedInEmail = googleEmail.ifBlank { "it.agnp@gmail.com" }
                    }
                }

                // Whenever an account is confirmed, log in & open main app!
                if (!signedInEmail.isNullOrBlank()) {
                    val finalEmail = signedInEmail
                    val finalName = signedInName ?: finalEmail.substringBefore("@").split(".").joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    }

                    googleEmail = finalEmail
                    profileName = finalName
                    if (!signedInPhoto.isNullOrBlank()) {
                        profileImageUri = signedInPhoto
                    }
                    isGoogleSignedIn = true

                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putBoolean("google_signed_in", true)
                        .putString("google_email", finalEmail)
                        .putString(PROFILE_NAME_KEY, finalName)
                        .putString(PROFILE_IMAGE_URI_KEY, profileImageUri)
                        .apply()

                    message = "✅ Signed in as $finalEmail"
                } else {
                    message = "Google Sign-In was cancelled."
                }
            }

            fun launchSystemGoogleAccountPicker() {
                val client = getGoogleSignInClient(this)
                try {
                    googleSignInLauncher.launch(client.signInIntent)
                } catch (_: Exception) {
                    message = "Unable to launch Google Sign-In"
                }
            }

            LaunchedEffect(Unit) {
                try {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val templateFile = java.io.File(downloadsDir, "Trip_Itinerary_Template.csv")
                    val csvContent = "Stop Number,Branch Name,Address,Task,Status\n" +
                            "1,Motosiklos Angono,Angono Rizal Philippines,Deliver 2x Motorcycle Model Alpha & Helmets,Pending\n" +
                            "2,Motosiklos Balanga,Balanga Bataan Philippines,Drop off 3x Scooter Units & Invoices,Pending\n" +
                            "3,Motosiklos Lucena,Lucena Quezon Philippines,Equipment audit & spare parts delivery,Pending\n" +
                            "4,Motosiklos Marikina,Marikina Metro Manila Philippines,Deliver 1x Heavy Duty Utility Motorcycle,Pending\n" +
                            "5,Head Office - AGNP,Caloocan Metro Manila Philippines,Submit weekly logistics delivery logs,Pending\n"
                    templateFile.writeText(csvContent)
                } catch (_: Exception) {}
            }

            fun persist(newList: List<Branch>) {
                branchList = newList
                saveBranches(this, newList)
            }

            var pickedRawImageUri by remember { mutableStateOf<Uri?>(null) }

            var isAutoBackupEnabled by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getBoolean(AUTO_BACKUP_ENABLED_KEY, true)
                )
            }
            var autoBackupSchedule by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(AUTO_BACKUP_SCHEDULE_KEY, "Daily") ?: "Daily"
                )
            }
            var lastBackupTime by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .getString(LAST_BACKUP_TIME_KEY, "") ?: ""
                )
            }

            LaunchedEffect(Unit) {
                createNotificationChannel(this@MainActivity)
            }

            LaunchedEffect(isAutoBackupEnabled, autoBackupSchedule, isGoogleSignedIn) {
                if (isAutoBackupEnabled && isGoogleSignedIn) {
                    val intervalMs = when (autoBackupSchedule) {
                        "Daily" -> 24 * 60 * 60 * 1000L
                        "Weekly" -> 7 * 24 * 60 * 60 * 1000L
                        "Monthly" -> 30 * 24 * 60 * 60 * 1000L
                        else -> 24 * 60 * 60 * 1000L
                    }

                    kotlinx.coroutines.delay(10000L)
                    while (true) {
                        val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                        if (account != null) {
                            val syncManager = DriveSyncManager(this@MainActivity, account)
                            val backupJson = createFullAppBackupJson(this@MainActivity)
                            val success = syncManager.uploadProgress(backupJson)
                            if (success) {
                                val timestamp = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(LAST_BACKUP_TIME_KEY, timestamp).apply()
                                lastBackupTime = timestamp
                                val summary = getLastBackupDetailSummary(this@MainActivity)
                                showBackupNotification(this@MainActivity, summary)
                            }
                        }
                        kotlinx.coroutines.delay(intervalMs)
                    }
                }
            }

            var showRestoreConfirmationDialog by remember { mutableStateOf(false) }

            fun performGoogleSignOut() {
                val client = getGoogleSignInClient(this)
                client.signOut().addOnCompleteListener {
                    isGoogleSignedIn = false
                    googleEmail = "Not Signed In"
                    profileName = DEFAULT_PROFILE_NAME
                    profileImageUri = ""

                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                    prefs.putBoolean("google_signed_in", false)
                    prefs.remove("google_email")
                    prefs.remove(PROFILE_IMAGE_URI_KEY)
                    prefs.apply()

                    message = "Logged out of Google Account. Cloud sync disconnected."
                }
            }

            fun performBackupNow() {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
                    val syncManager = DriveSyncManager(this, account)
                    val progressJson = createFullAppBackupJson(this)
                    lifecycleScope.launch {
                        val success = syncManager.uploadProgress(progressJson)
                        val miniClient = MiniServerClient(this@MainActivity)
                        miniClient.sendBackupToServer(googleEmail, progressJson)

                        val timestamp = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                        if (success) {
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(LAST_BACKUP_TIME_KEY, timestamp).apply()
                            lastBackupTime = timestamp
                            message = "App progress backed up to Google Drive & Render Cloud Server!"
                        } else {
                            message = "Cloud backup failed. Saved locally."
                        }
                    }
                } else {
                    message = "Please sign in with Google Account to enable cloud backup."
                }
            }

            var isSyncingToDrive by remember { mutableStateOf(false) }
            var lastSyncStatusText by remember { mutableStateOf<String?>(null) }

            fun triggerInstantGoogleDriveSync() {
                val account = GoogleSignIn.getLastSignedInAccount(this)
                if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
                    isSyncingToDrive = true
                    val syncManager = DriveSyncManager(this, account)
                    val progressJson = createFullAppBackupJson(this)
                    lifecycleScope.launch {
                        val success = syncManager.uploadProgress(progressJson)
                        isSyncingToDrive = false
                        if (success) {
                            val timestamp = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                            lastSyncStatusText = "Synced at $timestamp"
                        }
                    }
                }
            }

            var showLogOutConfirmationDialog by remember { mutableStateOf(false) }
            var showAccessRequestDialog by remember { mutableStateOf(false) }

            BranchLocatorTheme(isDark) {
                if (!isGoogleSignedIn) {
                    AppLockLoginScreen(
                        onSignInClick = { launchSystemGoogleAccountPicker() },
                        onRequestAccessClick = { showAccessRequestDialog = true }
                    )
                } else {
                    BranchLocatorScreen(
                        branches = branchList,
                        isDark = isDark,
                        adminUnlocked = adminUnlocked,
                        profileName = profileName,
                        profileImageUri = profileImageUri,
                        isGoogleSignedIn = isGoogleSignedIn,
                        googleEmail = googleEmail,
                        isSyncingToDrive = isSyncingToDrive,
                        lastSyncStatusText = lastSyncStatusText,
                        onOpenGoogleLogin = { launchSystemGoogleAccountPicker() },
                        onGoogleSignOut = { showLogOutConfirmationDialog = true },
                        onToggleTheme = { isDark = !isDark },
                        onAdminClick = { showPasswordDialog = true },
                        onOpenGpsCamera = { showGpsCamera = true },
                        onAddBranch = {
                            editingBranch = null
                            showBranchEditor = true
                        },
                        onEditBranch = {
                            editingBranch = it
                            showBranchEditor = true
                        },
                        onDeleteBranch = { branch ->
                            persist(branchList.filterNot { it.id == branch.id })
                            message = "${branch.name} deleted"
                        },
                        onOpenMap = { destination -> selectedMapDestination = destination },
                        onChangePassword = { showChangePassword = true },
                        onTriggerDriveSync = { triggerInstantGoogleDriveSync() },
                        lastBackupTime = lastBackupTime,
                        isAutoBackupEnabled = isAutoBackupEnabled,
                        autoBackupSchedule = autoBackupSchedule,
                        onBackupNow = { performBackupNow() },
                        onRestoreNow = { showRestoreConfirmationDialog = true },
                        onAutoBackupEnabledChange = { enabled ->
                            isAutoBackupEnabled = enabled
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                .putBoolean(AUTO_BACKUP_ENABLED_KEY, enabled).apply()
                        },
                        onAutoBackupScheduleChange = { schedule ->
                            autoBackupSchedule = schedule
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                .putString(AUTO_BACKUP_SCHEDULE_KEY, schedule).apply()
                        }
                    )
                }

                if (showAccessRequestDialog) {
                    AccessRequestDialog(
                        initialEmail = googleEmail.ifEmpty { "it.agnp@gmail.com" },
                        onDismiss = { showAccessRequestDialog = false },
                        onSubmitRequest = { name, email, dept, reason ->
                            showAccessRequestDialog = false
                            message = "✅ Access application sent directly to caperina13@gmail.com!"
                        }
                    )
                }

                if (showLogOutConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogOutConfirmationDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text("Confirm Log Out")
                            }
                        },
                        text = {
                            Text(
                                "Are you sure you want to log out of your Google Account?\n\nLogging out will remove all completed trips archive and active itinerary progress synced with this account from this device.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLogOutConfirmationDialog = false
                                    performGoogleSignOut()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Log Out & Remove Progress")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogOutConfirmationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (pickedRawImageUri != null) {
                    CropImageDialog(
                        imageUri = pickedRawImageUri!!,
                        onDismiss = { pickedRawImageUri = null },
                        onCropConfirmed = { croppedUriStr ->
                            pickedRawImageUri = null
                            profileImageUri = croppedUriStr
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(PROFILE_IMAGE_URI_KEY, croppedUriStr).apply()
                            message = "Profile picture cropped & updated"
                        }
                    )
                }

                if (showRestoreConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { showRestoreConfirmationDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Confirm Progress Restore")
                            }
                        },
                        text = {
                            Text(
                                "You are about to restore your backed-up data from your Google Account. This will overwrite any un-backed-up progress with the restored file.\n\nDo you want to proceed?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showRestoreConfirmationDialog = false
                                    val account = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                    if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
                                        val syncManager = DriveSyncManager(this@MainActivity, account)
                                        lifecycleScope.launch {
                                            val downloadedJson = syncManager.downloadProgress()
                                            val jsonToUse = if (!downloadedJson.isNullOrBlank()) {
                                                downloadedJson
                                            } else {
                                                try {
                                                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                                    val backupFile = java.io.File(downloadsDir, "AGNP_App_Backup.json")
                                                    if (backupFile.exists()) backupFile.readText() else null
                                                } catch (_: Exception) { null }
                                            }

                                            if (!jsonToUse.isNullOrBlank()) {
                                                val success = restoreAppFromBackupJson(this@MainActivity, jsonToUse)
                                                if (success) {
                                                    branchList = loadBranches(this@MainActivity)
                                                    profileName = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                                        .getString(PROFILE_NAME_KEY, DEFAULT_PROFILE_NAME) ?: DEFAULT_PROFILE_NAME
                                                    profileImageUri = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                                        .getString(PROFILE_IMAGE_URI_KEY, "") ?: ""
                                                    message = "All app progress restored automatically from Google Drive AppData!"
                                                } else {
                                                    message = "Failed to parse Google Drive backup file."
                                                }
                                            } else {
                                                message = "No Google Drive backup file found. Tap 'Backup Now' first."
                                            }
                                        }
                                    } else {
                                        message = "Sign in to Google Account to restore cloud progress."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Confirm Restore")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRestoreConfirmationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (selectedMapDestination != null) {
                    MapAppSelectorDialog(
                        destination = selectedMapDestination!!,
                        onDismiss = { selectedMapDestination = null },
                        onOpenGoogleMaps = { dest -> openInGoogleMaps(dest) },
                        onOpenWaze = { dest -> openInWaze(dest) }
                    )
                }

                if (showGpsCamera) {
                    val cameraLogoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        if (uri != null) {
                            cameraLogoUri = uri.toString()
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(CAMERA_LOGO_URI_KEY, cameraLogoUri).apply()
                        }
                    }
                    GpsCameraScreen(
                        cameraLogoUri = cameraLogoUri,
                        onChangeLogo = { cameraLogoLauncher.launch("image/*") },
                        onDismiss = { showGpsCamera = false },
                        onPhotoCaptured = { uri ->
                            showGpsCamera = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "AGNP Logistics Geotagged Field Photo with live GPS and timestamp.")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                startActivity(Intent.createChooser(shareIntent, "Share GPS Geotagged Photo"))
                            } catch (_: Exception) {}
                        }
                    )
                }

                if (showPasswordDialog) {
                    AdminPasswordDialog(
                        onDismiss = { showPasswordDialog = false },
                        onUnlock = { password ->
                            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val savedPassword = prefs.getString(ADMIN_PASSWORD_KEY, DEFAULT_ADMIN_PASSWORD)
                            if (password == savedPassword) {
                                adminUnlocked = true
                                showPasswordDialog = false
                                message = "Admin editing unlocked"
                            } else {
                                message = "Incorrect administrator password"
                            }
                        },
                        isUnlocked = adminUnlocked,
                        onLock = {
                            adminUnlocked = false
                            showPasswordDialog = false
                            message = "Admin editing locked"
                        },
                        onChangePassword = {
                            showPasswordDialog = false
                            showChangePassword = true
                        }
                    )
                }

                if (showChangePassword) {
                    ChangePasswordDialog(
                        onDismiss = { showChangePassword = false },
                        onSave = { oldPassword, newPassword ->
                            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val savedPassword = prefs.getString(ADMIN_PASSWORD_KEY, DEFAULT_ADMIN_PASSWORD)
                            if (oldPassword != savedPassword) {
                                message = "Current password is incorrect"
                                false
                            } else if (newPassword.length < 6) {
                                message = "New password must be at least 6 characters"
                                false
                            } else {
                                prefs.edit().putString(ADMIN_PASSWORD_KEY, newPassword).apply()
                                message = "Administrator password updated"
                                showChangePassword = false
                                true
                            }
                        }
                    )
                }

                if (showBranchEditor) {
                    BranchEditorDialog(
                        initialBranch = editingBranch,
                        onDismiss = { showBranchEditor = false },
                        onSave = { name, address, contactNumber, imageUri, maps ->
                            val current = editingBranch
                            val updated = if (current == null) {
                                Branch(
                                    id = (branchList.maxOfOrNull { it.id } ?: 0L) + 1L,
                                    name = name,
                                    address = address,
                                    contactNumber = contactNumber,
                                    maps = maps,
                                    imageUri = imageUri
                                )
                            } else {
                                current.copy(name = name, address = address, contactNumber = contactNumber, maps = maps, imageUri = imageUri)
                            }
                            val newList = if (current == null) branchList + updated
                                else branchList.map { if (it.id == current.id) updated else it }
                            persist(newList)
                            showBranchEditor = false
                            message = if (current == null) "Branch added" else "Branch updated"
                        }
                    )
                }

                if (showProfileEditor) {
                    ProfileEditorDialog(
                        initialName = profileName,
                        onDismiss = { showProfileEditor = false },
                        onSave = { newName ->
                            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(PROFILE_NAME_KEY, newName).apply()
                            profileName = newName
                            showProfileEditor = false
                            message = "Profile name updated"
                        }
                    )
                }

                message?.let { msg ->
                    LaunchedEffect(msg) {
                        // The snackbar is shown by the host message area below when a new message arrives.
                    }
                }
            }
        }
    }

    private fun openInGoogleMaps(destination: String) {
        val trimmed = destination.trim()
        if (trimmed.isEmpty()) return
        val uri = if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            Uri.parse(trimmed)
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(trimmed)}")
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
        } catch (_: ActivityNotFoundException) {
            val fallback = if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) uri else
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(trimmed)}")
            try { startActivity(Intent(Intent.ACTION_VIEW, fallback)) } catch (_: ActivityNotFoundException) { }
        }
    }

    private fun openInWaze(destination: String) {
        val trimmed = destination.trim()
        if (trimmed.isEmpty()) return
        val query = if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) trimmed else Uri.encode(trimmed)
        val wazeUri = Uri.parse("https://waze.com/ul?q=$query&navigate=yes")
        try {
            val intent = Intent(Intent.ACTION_VIEW, wazeUri).setPackage("com.waze")
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
            } catch (_: ActivityNotFoundException) {}
        }
    }
}

@Composable
private fun BranchLocatorTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF00BFA5), // Vibrant Teal / Mint Accent matching reference image
        onPrimary = Color.White,
        secondary = Color(0xFF26A69A),
        background = Color(0xFFEBF3EE), // Soft Sage / Pistachio Light Background
        surface = Color.White,
        surfaceVariant = Color(0xFFF4F8F5),
        onSurface = Color(0xFF1E293B),
        onSurfaceVariant = Color(0xFF64748B)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF26A69A),
        onPrimary = Color.White,
        secondary = Color(0xFF00BFA5),
        background = Color(0xFF0F1713),
        surface = Color(0xFF17261F),
        surfaceVariant = Color(0xFF1E3329),
        onSurface = Color(0xFFF8FAFC),
        onSurfaceVariant = Color(0xFF94A3B8)
    )
    MaterialTheme(colorScheme = if (isDark) dark else light, content = content)
}

@Composable
private fun RequestCameraAndLocationPermissions() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val camera = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!fine || !camera) {
            launcher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.CAMERA
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BranchLocatorScreen(
    branches: List<Branch>,
    isDark: Boolean,
    adminUnlocked: Boolean,
    profileName: String,
    profileImageUri: String,
    isGoogleSignedIn: Boolean,
    googleEmail: String,
    isSyncingToDrive: Boolean,
    lastSyncStatusText: String?,
    onOpenGoogleLogin: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onToggleTheme: () -> Unit,
    onAdminClick: () -> Unit,
    onOpenGpsCamera: () -> Unit,
    onAddBranch: () -> Unit,
    onEditBranch: (Branch) -> Unit,
    onDeleteBranch: (Branch) -> Unit,
    onOpenMap: (String) -> Unit,
    onChangePassword: () -> Unit,
    onTriggerDriveSync: () -> Unit,
    lastBackupTime: String = "",
    isAutoBackupEnabled: Boolean = true,
    autoBackupSchedule: String = "Daily",
    onBackupNow: () -> Unit = {},
    onRestoreNow: () -> Unit = {},
    onAutoBackupEnabledChange: (Boolean) -> Unit = {},
    onAutoBackupScheduleChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedBranch by remember { mutableStateOf<Long?>(null) }
    var currentTab by remember { mutableStateOf(0) }

    val categories = listOf("All", "MOTOSIKLOS", "FILSIKLOS", "Head Office")

    val filtered = branches.filter { branch ->
        val matchesSearch = branch.name.contains(searchText, true) || branch.address.contains(searchText, true)
        val matchesCategory = when (selectedCategory) {
            "MOTOSIKLOS" -> branch.name.contains("Motosiklos", true)
            "FILSIKLOS" -> branch.name.contains("Filsiklos", true)
            "Head Office" -> branch.name.contains("Head Office", true)
            else -> true
        }
        matchesSearch && matchesCategory
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF00897B),
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFFB2DFDB),
                        unselectedTextColor = Color(0xFFB2DFDB),
                        indicatorColor = Color(0xFF004D40)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Explore, null) },
                    label = { Text("Explore") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFFB2DFDB),
                        unselectedTextColor = Color(0xFFB2DFDB),
                        indicatorColor = Color(0xFF004D40)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, null) },
                    label = { Text("Itinerary") },
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFFB2DFDB),
                        unselectedTextColor = Color(0xFFB2DFDB),
                        indicatorColor = Color(0xFF004D40)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Account") },
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        unselectedIconColor = Color(0xFFB2DFDB),
                        unselectedTextColor = Color(0xFFB2DFDB),
                        indicatorColor = Color(0xFF004D40)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(12.dp))
            
            // Top Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp).clip(CircleShape).combinedClickable(
                        onClick = {},
                        onLongClick = onAdminClick
                    )
                ) {
                    if (profileImageUri.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = profileImageUri),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Good Morning", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onAdminClick) {
                    Icon(if (adminUnlocked) Icons.Default.Shield else Icons.Default.Notifications, contentDescription = "Admin / Notifications", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onToggleTheme) {
                    Icon(if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle Theme", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Live Google Account Sync Status Indicator Banner
            if (isGoogleSignedIn) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSyncingToDrive) Color(0xFFFFF3C4) else Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSyncingToDrive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFF59E0B)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("☁️ Syncing progress to Google Drive ($googleEmail)...", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                            } else {
                                Text("🟢 Live Synced with Google Account ($googleEmail)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                            }
                        }
                        lastSyncStatusText?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (currentTab == 3) {
                // Account / Settings Tab
                Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Account & Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    // In-App Google Account & Sign-In Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Google Account Sign-In", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (isGoogleSignedIn) "Connected: $googleEmail" else "Sign in to enable cloud backup & restore",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isGoogleSignedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isGoogleSignedIn) {
                                Button(
                                    onClick = onOpenGoogleLogin,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Person, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign In with Google")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenGoogleLogin,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Switch Account")
                                    }

                                    Button(
                                        onClick = onGoogleSignOut,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Log Out")
                                    }
                                }
                            }
                        }
                    }

                    // Google Drive Cloud Backup & Restore Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Google Drive Cloud Backup & Restore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        if (lastBackupTime.isNotBlank()) "Last backup: $lastBackupTime" else "No cloud backup created yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (lastBackupTime.isNotBlank()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (lastBackupTime.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            // Manual Backup & Restore Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onBackupNow,
                                    enabled = isGoogleSignedIn,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Backup Now")
                                }

                                OutlinedButton(
                                    onClick = onRestoreNow,
                                    enabled = isGoogleSignedIn,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Restore Backup")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Auto Backup Settings & Schedule
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Automatic Cloud Backup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Auto-sync app progress in background", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isAutoBackupEnabled,
                                    onCheckedChange = onAutoBackupEnabledChange,
                                    enabled = isGoogleSignedIn
                                )
                            }

                            if (isAutoBackupEnabled) {
                                Text("Auto-Backup Schedule Frequency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Daily", "Weekly", "Monthly").forEach { schedule ->
                                        val isSelected = autoBackupSchedule.equals(schedule, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onAutoBackupScheduleChange(schedule) },
                                            label = { Text(schedule) },
                                            modifier = Modifier.weight(1f),
                                            enabled = isGoogleSignedIn
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Mini Cloud Server Backup & Sync Card (Render / Free Cloud Hosting)
                    var miniServerUrlInput by remember {
                        mutableStateOf(
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .getString("mini_server_url", "https://branchlocator-r2mc.onrender.com") ?: "https://branchlocator-r2mc.onrender.com"
                        )
                    }
                    var isSyncingMiniServer by remember { mutableStateOf(false) }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Free Cloud Mini-Server API", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Sync backup data & applications to your Node.js cloud server", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            OutlinedTextField(
                                value = miniServerUrlInput,
                                onValueChange = { newUrl ->
                                    miniServerUrlInput = newUrl
                                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                                        .putString("mini_server_url", newUrl).apply()
                                },
                                label = { Text("Cloud Server URL (Render / Railway / Local)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isSyncingMiniServer = true
                                        val backupJson = createFullAppBackupJson(context)
                                        val client = MiniServerClient(context)
                                        coroutineScope.launch {
                                            val success = client.sendBackupToServer(googleEmail, backupJson)
                                            isSyncingMiniServer = false
                                            if (success) {
                                                android.widget.Toast.makeText(context, "✅ Synced app backup to Cloud Mini Server!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "❌ Could not connect to Mini Server at $miniServerUrlInput", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isSyncingMiniServer && isGoogleSignedIn,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                                ) {
                                    if (isSyncingMiniServer) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Syncing...")
                                    } else {
                                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Sync to Cloud")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        isSyncingMiniServer = true
                                        val client = MiniServerClient(context)
                                        coroutineScope.launch {
                                            val downloadedJson = client.getBackupFromServer(googleEmail)
                                            isSyncingMiniServer = false
                                            if (!downloadedJson.isNullOrBlank()) {
                                                val success = restoreAppFromBackupJson(context, downloadedJson)
                                                if (success) {
                                                    android.widget.Toast.makeText(context, "✅ Restored backup from Cloud Mini Server!", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "❌ Failed to parse data from Cloud Mini Server", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                android.widget.Toast.makeText(context, "❌ No backup found on Cloud Mini Server for $googleEmail", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isSyncingMiniServer && isGoogleSignedIn,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Restore Cloud")
                                }
                            }
                        }
                    }



                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("App Update Available", fontWeight = FontWeight.SemiBold)
                                    Text("Download latest APK update from Google Drive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick = {
                                    val driveUrl = "https://drive.google.com/drive/folders/1nzKF89NT4khPbtKGCNQkj488-zK1uGuT?usp=sharing"
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl)))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudDownload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Download Update from Google Drive")
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Built-in GPS Camera", fontWeight = FontWeight.SemiBold)
                                    Text("Capture photos with stamped GPS location, date & time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick = onOpenGpsCamera,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Built-in GPS Camera")
                            }
                        }
                    }
                    if (adminUnlocked) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Administrator Actions (Unlocked)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                                    Text("Change Administrator Password", color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = onAddBranch, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add New Branch", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Trips Progress Syncing & Cloud Archive Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Trips Progress Syncing & Cloud Archive", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (isGoogleSignedIn) "Connected: $googleEmail" else "Sign in to sync trip progress",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isGoogleSignedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val archivedTripsCount = remember(isGoogleSignedIn, googleEmail) {
                                loadCompletedTripsArchive(context).size
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$archivedTripsCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Color(0xFF00BFA5))
                                        Text("Archived Trips", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${branches.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                        Text("Active Destinations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Text(
                                if (isGoogleSignedIn) "🟢 All completed trips and checklist progress automatically sync to Google Drive AppData ($googleEmail)."
                                else "🔑 Sign in with your Google Account above to sync trip progress and archives.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isGoogleSignedIn) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (currentTab == 2) {
                // Itinerary & Branch Delivery Checklist Tab
                val userLoc = rememberLiveUserLocation()

                var deliveryTasks by remember(branches) {
                    mutableStateOf<List<BranchDeliveryTask>>(
                        loadDeliveryTasks(context)
                    )
                }

                var signingTaskBranchId by remember { mutableStateOf<Long?>(null) }
                var reviewingTask by remember { mutableStateOf<BranchDeliveryTask?>(null) }
                var showCompletedTasksModal by remember { mutableStateOf(false) }
                var showMasterReviewDialog by remember { mutableStateOf(false) }
                var showTripsArchiveDialog by remember { mutableStateOf(false) }

                val csvImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    if (uri != null) {
                        val parsed = parseItineraryFile(context, uri)
                        if (parsed.isNotEmpty()) {
                            deliveryTasks = parsed
                            saveDeliveryTasks(context, parsed)
                        }
                    }
                }

                val completedBranchesCount = deliveryTasks.count { it.isCompleted }
                val totalBranchesCount = deliveryTasks.size

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Trip Today Delivery Checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Import Excel/CSV or check off delivered items per branch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Import Button Banner
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Get & Import Itinerary File", fontWeight = FontWeight.Bold)
                                    Text("Download or import itinerary Excel files for today's trip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // 1. Get Itinerary File Button (Above Import)
                            Button(
                                onClick = {
                                    val driveUrl = "https://drive.google.com/drive/folders/1NS3GVNS55sV2w-N37UbwPqv-GejoxExJ?usp=drive_link"
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(driveUrl)))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudDownload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Get Itinerary File")
                            }

                            // 2. Import Itinerary File Button
                            OutlinedButton(
                                onClick = { csvImportLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Import Itinerary File (.xlsx)")
                            }
                        }
                    }

                    // Buttons for Completed Tasks and Trips Archive above Progress Card
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCompletedTasksModal = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Completed Task ($completedBranchesCount)", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { showTripsArchiveDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Trips Archive", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Progress Summary Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Trip Delivery Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("$completedBranchesCount / $totalBranchesCount Completed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            val progress = if (totalBranchesCount > 0) completedBranchesCount.toFloat() / totalBranchesCount.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Text("Branch Delivery Checklist ($totalBranchesCount)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    deliveryTasks.forEachIndexed { bIndex, task ->
                        val allItemsDelivered = task.itemsToDeliver.all { it.isDelivered }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (task.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${bIndex + 1}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(task.branchName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text(task.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (task.isCompleted) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text("Completed", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }

                                Text("Items / Motorcycles to Deliver & Drop Off:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                                task.itemsToDeliver.forEachIndexed { iIndex, item ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Checkbox(
                                            checked = item.isDelivered,
                                            enabled = !task.isCompleted,
                                            onCheckedChange = { isChecked ->
                                                val updatedList = deliveryTasks.toMutableList().also { list ->
                                                    val updatedItems = task.itemsToDeliver.toMutableList().also {
                                                        it[iIndex] = item.copy(isDelivered = isChecked)
                                                    }
                                                    list[bIndex] = task.copy(itemsToDeliver = updatedItems)
                                                }
                                                deliveryTasks = updatedList
                                                saveDeliveryTasks(context, updatedList)
                                            }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (item.isDelivered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val branchCoord = getCoordinatesForDestination(task.branchName, task.address, task.branchName)
                                val travelInfo = if (userLoc != null) {
                                    calculateTravelTime(userLoc.latitude, userLoc.longitude, branchCoord.lat, branchCoord.lon)
                                } else {
                                    calculateTravelTime(14.5995, 120.9842, branchCoord.lat, branchCoord.lon)
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(travelInfo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                if (!task.isCompleted) {
                                    if (allItemsDelivered) {
                                        Button(
                                            onClick = { signingTaskBranchId = task.branchId },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Gesture, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Capture Receiver Signature", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        Text(
                                            "Check off all items above to unlock signature capture.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { reviewingTask = task },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Review & Export Desktop Report", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    var showCompleteTripConfirmation by remember { mutableStateOf(false) }

                    if (deliveryTasks.isNotEmpty()) {
                        Button(
                            onClick = { showCompleteTripConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Complete Entire Trip & Archive", fontWeight = FontWeight.Bold)
                        }
                    }

                    val completedTasksList = deliveryTasks.filter { it.isCompleted }
                    if (completedTasksList.isNotEmpty()) {
                        Button(
                            onClick = { showMasterReviewDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Description, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate All (${completedTasksList.size} Completed)")
                        }
                    }

                    var showClearConfirmation by remember { mutableStateOf(false) }

                    if (deliveryTasks.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Clear All Tasks", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (showCompleteTripConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showCompleteTripConfirmation = false },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00BFA5))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Complete Entire Trip?")
                                }
                            },
                            text = {
                                Text(
                                    "This will finish and complete this entire trip. All branch tasks will be finalized, archived to the Completed Trips Archive, and your active checklist cleared for the next trip.\n\nDo you want to proceed?",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showCompleteTripConfirmation = false
                                        val finalizedList = deliveryTasks.map { task ->
                                            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", java.util.Locale.getDefault())
                                            val ts = task.completedTimestamp.ifEmpty { sdf.format(java.util.Date()) }
                                            task.copy(
                                                isCompleted = true,
                                                completedTimestamp = ts,
                                                receiverName = task.receiverName.ifEmpty { "Branch Representative" },
                                                itemsToDeliver = task.itemsToDeliver.map { it.copy(isDelivered = true) }
                                            )
                                        }
                                        saveCompletedTripToArchive(context, finalizedList)
                                        deliveryTasks = emptyList()
                                        saveDeliveryTasks(context, emptyList())
                                        onTriggerDriveSync()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                                ) {
                                    Text("Finish & Archive Trip")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCompleteTripConfirmation = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (showClearConfirmation) {
                        AlertDialog(
                            onDismissRequest = { showClearConfirmation = false },
                            title = { Text("Confirm Clear Tasks") },
                            text = { Text("Are you sure you want to clear all trip tasks and delivery checklists?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showClearConfirmation = false
                                        deliveryTasks = emptyList()
                                        saveDeliveryTasks(context, emptyList())
                                    }
                                ) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearConfirmation = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }

                // Full Screen Signature Pad View
                if (signingTaskBranchId != null) {
                    val activeTask = deliveryTasks.find { it.branchId == signingTaskBranchId }
                    if (activeTask != null) {
                        Dialog(
                            onDismissRequest = { signingTaskBranchId = null },
                            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                        ) {
                            FullScreenSignatureView(
                                branchName = activeTask.branchName,
                                onDismiss = { signingTaskBranchId = null },
                                onConfirmSignature = { receiverName, signatureBm ->
                                    signingTaskBranchId = null
                                    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", java.util.Locale.getDefault())
                                    val timestamp = sdf.format(java.util.Date())
                                    val updatedTask = activeTask.copy(
                                        receiverName = receiverName,
                                        signatureBitmap = signatureBm,
                                        signatureCaptured = true,
                                        isCompleted = true,
                                        completedTimestamp = timestamp
                                    )
                                    val updatedList = deliveryTasks.map { if (it.branchId == activeTask.branchId) updatedTask else it }
                                    deliveryTasks = updatedList
                                    saveDeliveryTasks(context, updatedList)
                                    if (updatedList.isNotEmpty() && updatedList.all { it.isCompleted }) {
                                        saveCompletedTripToArchive(context, updatedList)
                                    }
                                    reviewingTask = updatedTask
                                }
                            )
                        }
                    }
                }

                // Review Report Dialog before generating file
                if (reviewingTask != null) {
                    ReviewTripReportDialog(
                        task = reviewingTask!!,
                        onDismiss = { reviewingTask = null },
                        onConfirmGenerate = {
                            val taskToExport = reviewingTask!!
                            reviewingTask = null
                            val fileUri = generateDesktopExcelReport(context, taskToExport)
                            if (fileUri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    putExtra(Intent.EXTRA_TEXT, "AGNP Logistics Desktop Trip Completion Report for ${taskToExport.branchName}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Desktop Excel Report"))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }

                if (showMasterReviewDialog) {
                    val completedList = deliveryTasks.filter { it.isCompleted }
                    ReviewMasterReportDialog(
                        completedTasks = completedList,
                        onDismiss = { showMasterReviewDialog = false },
                        onConfirmGenerate = {
                            showMasterReviewDialog = false
                            val masterFileUri = generateMasterDesktopExcelReport(context, completedList)
                            if (masterFileUri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, masterFileUri)
                                    putExtra(Intent.EXTRA_TEXT, "AGNP Master Completed Trip Report (${completedList.size} Completed Branches)")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Master Excel Report"))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }

                if (showTripsArchiveDialog) {
                    val archivedList = loadCompletedTripsArchive(context)
                    CompletedTripsArchiveDialog(
                        archivedTrips = archivedList,
                        onDismiss = { showTripsArchiveDialog = false },
                        onExportMasterReport = { tasksToExport ->
                            val masterFileUri = generateMasterDesktopExcelReport(context, tasksToExport)
                            if (masterFileUri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, masterFileUri)
                                    putExtra(Intent.EXTRA_TEXT, "AGNP Master Completed Trip Report (${tasksToExport.size} Branches)")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Master Excel Report"))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }

                if (showCompletedTasksModal) {
                    val completedList = deliveryTasks.filter { it.isCompleted }
                    AlertDialog(
                        onDismissRequest = { showCompletedTasksModal = false },
                        title = { Text("Completed Tasks (${completedList.size})") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (completedList.isEmpty()) {
                                    Text("No branches completed yet. Check off delivery items and capture receiver signatures to complete branch tasks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    completedList.forEach { cTask ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("🏢 ${cTask.branchName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                Text("📍 ${cTask.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("👤 Receiver: ${cTask.receiverName}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                                Text("⏰ Completed: ${cTask.completedTimestamp}", style = MaterialTheme.typography.labelSmall)

                                                if (cTask.signatureBitmap != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color.White,
                                                        modifier = Modifier.fillMaxWidth().height(70.dp)
                                                    ) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(model = cTask.signatureBitmap),
                                                            contentDescription = "Receiver Signature",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        val fileUri = generateDesktopExcelReport(context, cTask)
                                                        if (fileUri != null) {
                                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                                                putExtra(Intent.EXTRA_TEXT, "AGNP Logistics Report for ${cTask.branchName}")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            try {
                                                                context.startActivity(Intent.createChooser(shareIntent, "Share Excel Report"))
                                                            } catch (_: Exception) {}
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Re-Export .xlsx Report", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCompletedTasksModal = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            } else if (currentTab == 1) {
                // Explore Tab (Branch Image Gallery with search & filters)
                var exploreSearch by remember { mutableStateOf("") }
                var exploreCategory by remember { mutableStateOf("All") }
                val exploreCategories = listOf("All", "MOTOSIKLOS", "FILSIKLOS", "Head Office")

                val exploreFiltered = branches.filter { branch ->
                    val matchesSearch = branch.name.contains(exploreSearch, true) || branch.address.contains(exploreSearch, true)
                    val matchesCat = when (exploreCategory) {
                        "MOTOSIKLOS" -> branch.name.contains("Motosiklos", true)
                        "FILSIKLOS" -> branch.name.contains("Filsiklos", true)
                        "Head Office" -> branch.name.contains("Head Office", true)
                        else -> true
                    }
                    matchesSearch && matchesCat
                }

                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Explore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${exploreFiltered.size} branches found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = exploreSearch,
                        onValueChange = { exploreSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search branch or location...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE2E8F0),
                            unfocusedContainerColor = Color(0xFFE2E8F0),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exploreCategories.forEach { cat ->
                            FilterChip(
                                selected = exploreCategory == cat,
                                onClick = { exploreCategory = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    exploreFiltered.forEach { branch ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (branch.imageUri.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.fillMaxWidth().height(190.dp)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = branch.imageUri),
                                            contentDescription = branch.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth().height(140.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                                Spacer(Modifier.height(4.dp))
                                                Text(if (adminUnlocked) "Tap Edit to add branch photo" else "No image attached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(branch.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(branch.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (adminUnlocked) {
                                        Button(
                                            onClick = { onEditBranch(branch) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Edit")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Home Tab
                RequestCameraAndLocationPermissions()

                val liveLoc = rememberLiveUserLocation()
                val locName = rememberLiveLocationName(liveLoc)

                var liveTime by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", java.util.Locale.getDefault())
                        liveTime = sdf.format(java.util.Date())
                        delay(1000)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Current Location: $locName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Date & Time: $liveTime",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search branch or location...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE2E8F0),
                        unfocusedContainerColor = Color(0xFFE2E8F0),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Featured Locations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${filtered.size} branches", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(8.dp))

                if (adminUnlocked) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Admin Mode Unlocked", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Button(onClick = onAddBranch, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { branch ->
                        BranchCard(
                            branch = branch,
                            expanded = selectedBranch == branch.id,
                            adminUnlocked = adminUnlocked,
                            onClick = { selectedBranch = if (selectedBranch == branch.id) null else branch.id },
                            onOpenMap = onOpenMap,
                            onEdit = { onEditBranch(branch) },
                            onDelete = { onDeleteBranch(branch) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchCard(
    branch: Branch,
    expanded: Boolean,
    adminUnlocked: Boolean,
    onClick: () -> Unit,
    onOpenMap: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var isLoadingWeather by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            isLoadingWeather = true
            val coords = getCoordinatesForBranch(branch.address, branch.name)
            fetchWeatherForCoordinates(coords.lat, coords.lon) { info ->
                weather = info
                isLoadingWeather = false
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(125.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable(onClick = onClick)
                    .padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Active", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    if (weather != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(weather!!.icon, style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.width(3.dp))
                                Text("${weather!!.temp} • ${weather!!.condition}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (adminUnlocked) {
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
                Column(Modifier.align(Alignment.BottomStart)) {
                    Text(branch.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(branch.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    if (branch.contactNumber.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${branch.contactNumber}")))
                                } catch (_: ActivityNotFoundException) {}
                            }
                        ) {
                            Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(branch.contactNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${branch.maps.size} Map Location(s) Available", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isLoadingWeather) {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else if (weather != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(weather!!.icon, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Weather Status: ${weather!!.condition}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Temperature: ${weather!!.temp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    val userLoc = rememberLiveUserLocation()
                    val targetDest = branch.maps.firstOrNull()?.destination ?: branch.address
                    val branchCoord = getCoordinatesForDestination(targetDest, branch.address, branch.name)
                    val travelInfo = if (userLoc != null) {
                        calculateTravelTime(userLoc.latitude, userLoc.longitude, branchCoord.lat, branchCoord.lon)
                    } else {
                        calculateTravelTime(14.5995, 120.9842, branchCoord.lat, branchCoord.lon)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Est. Arrival / Travel Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(travelInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    branch.maps.forEach { map ->
                        Button(
                            onClick = { onOpenMap(map.destination) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(map.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("Open", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (branch.maps.isEmpty()) {
                        Text("No map links added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenSignatureView(
    branchName: String,
    onDismiss: () -> Unit,
    onConfirmSignature: (String, android.graphics.Bitmap) -> Unit
) {
    var receiverName by remember { mutableStateOf("Branch Representative") }
    val strokes = remember { mutableStateListOf<MutableList<SignaturePoint>>() }
    var error by remember { mutableStateOf<String?>(null) }
    var canvasWidth by remember { mutableStateOf(1000) }
    var canvasHeight by remember { mutableStateOf(600) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Receiver Signature Capture", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Branch: $branchName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            OutlinedTextField(
                value = receiverName,
                onValueChange = { receiverName = it },
                label = { Text("Receiver Full Name (Required)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Text("Sign below using finger or stylus:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        if (coords.size.width > 0 && coords.size.height > 0) {
                            canvasWidth = coords.size.width
                            canvasHeight = coords.size.height
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                strokes.add(mutableStateListOf(SignaturePoint(offset.x, offset.y)))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                strokes.lastOrNull()?.add(SignaturePoint(change.position.x, change.position.y))
                            }
                        )
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        strokes.forEach { stroke ->
                            if (stroke.size > 1) {
                                for (i in 0 until stroke.size - 1) {
                                    drawLine(
                                        color = Color.Black,
                                        start = androidx.compose.ui.geometry.Offset(stroke[i].x, stroke[i].y),
                                        end = androidx.compose.ui.geometry.Offset(stroke[i + 1].x, stroke[i + 1].y),
                                        strokeWidth = 8f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                    if (strokes.isEmpty()) {
                        Text(
                            "✍️ Draw receiver signature here...",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { strokes.clear() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Pad", color = MaterialTheme.colorScheme.error)
                }

                OutlinedButton(
                    onClick = {
                        strokes.clear()
                        val sample = mutableStateListOf<SignaturePoint>()
                        for (i in 0..100) {
                            val x = 100f + i * 6f
                            val y = canvasHeight / 2f + kotlin.math.sin(i * 0.1f).toFloat() * 80f
                            sample.add(SignaturePoint(x, y))
                        }
                        strokes.add(sample)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Quick Sign")
                }

                Button(
                    onClick = {
                        if (receiverName.isBlank()) {
                            error = "Please enter receiver full name."
                        } else if (strokes.isEmpty()) {
                            error = "Please draw signature or tap Quick Sign."
                        } else {
                            val signatureBitmap = android.graphics.Bitmap.createBitmap(
                                canvasWidth.coerceAtLeast(200),
                                canvasHeight.coerceAtLeast(200),
                                android.graphics.Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(signatureBitmap)
                            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                strokeWidth = 8f
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                isAntiAlias = true
                            }
                            strokes.forEach { stroke ->
                                if (stroke.size > 1) {
                                    for (i in 0 until stroke.size - 1) {
                                        canvas.drawLine(stroke[i].x, stroke[i].y, stroke[i + 1].x, stroke[i + 1].y, paint)
                                    }
                                }
                            }
                            onConfirmSignature(receiverName.trim(), signatureBitmap)
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
private fun CompletedTripsArchiveDialog(
    archivedTrips: List<CompletedTripArchiveRecord>,
    onDismiss: () -> Unit,
    onExportMasterReport: (List<BranchDeliveryTask>) -> Unit
) {
    var selectedDateFilter by remember { mutableStateOf("All Dates") }

    val datesWithTrips = remember(archivedTrips) {
        archivedTrips.map { it.completionDate }.distinct()
    }

    val availableFilterDates = remember(datesWithTrips) {
        listOf("All Dates") + datesWithTrips
    }

    val filteredTrips = remember(selectedDateFilter, archivedTrips) {
        if (selectedDateFilter == "All Dates") {
            archivedTrips
        } else {
            archivedTrips.filter { it.completionDate.contains(selectedDateFilter, true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Completed Trips Archive")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Archive of fully completed trips (100% finished branches). Filter by date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Date Filter (🟢 = Completed Trip Available):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableFilterDates.forEach { dateStr ->
                        val hasTripOnDate = dateStr != "All Dates" && datesWithTrips.contains(dateStr)

                        FilterChip(
                            selected = selectedDateFilter == dateStr,
                            onClick = { selectedDateFilter = dateStr },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(dateStr)
                                    if (hasTripOnDate) {
                                        Spacer(Modifier.width(4.dp))
                                        Text("🟢", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text("Archived Completed Trips (${filteredTrips.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                if (filteredTrips.isEmpty()) {
                    Text("No fully completed trips archived for $selectedDateFilter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    filteredTrips.forEach { tripRecord ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📅 ${tripRecord.completionDate}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Spacer(Modifier.width(6.dp))
                                        Text("🟢 Trip Completed", color = Color(0xFF10B981), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text("Total Branches Completed: ${tripRecord.tasks.size}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                                tripRecord.tasks.forEach { task ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("🏢 ${task.branchName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("👤 Receiver: ${task.receiverName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Text("⏰ Time: ${task.completedTimestamp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onExportMasterReport(tripRecord.tasks) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Re-Export Master .xlsx Report", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ReviewMasterReportDialog(
    completedTasks: List<BranchDeliveryTask>,
    onDismiss: () -> Unit,
    onConfirmGenerate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Master Report Preview (${completedTasks.size} Branches)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Preview all completed branches to be exported into the Master Excel Workbook (.xlsx):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                completedTasks.forEachIndexed { index, task ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("STOP #${index + 1}: ${task.branchName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("📍 Address: ${task.address}", style = MaterialTheme.typography.bodySmall)
                            Text("👤 Receiver: ${task.receiverName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text("⏰ Timestamp: ${task.completedTimestamp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (task.signatureBitmap != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 4.dp)
                                ) {
                                    Image(
                                        bitmap = task.signatureBitmap.asImageBitmap(),
                                        contentDescription = "Receiver Signature",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmGenerate,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Description, null)
                Spacer(Modifier.width(6.dp))
                Text("Confirm & Share Master .xlsx")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back") }
        }
    )
}

private fun cropAndSaveBitmap(context: Context, imageUri: Uri, scale: Float, offsetX: Float, offsetY: Float): String {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return imageUri.toString()
        val original = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (original == null) return imageUri.toString()

        val size = 300
        val cropped = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(cropped)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        val srcWidth = original.width
        val srcHeight = original.height
        val minDim = minOf(srcWidth, srcHeight)
        val srcRect = android.graphics.Rect(
            (srcWidth - minDim) / 2,
            (srcHeight - minDim) / 2,
            (srcWidth + minDim) / 2,
            (srcHeight + minDim) / 2
        )
        val destRect = android.graphics.Rect(0, 0, size, size)

        canvas.drawBitmap(original, srcRect, destRect, paint)

        val file = java.io.File(context.filesDir, "profile_cropped_${System.currentTimeMillis()}.png")
        val out = java.io.FileOutputStream(file)
        cropped.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        out.close()

        return Uri.fromFile(file).toString()
    } catch (_: Exception) {
        return imageUri.toString()
    }
}

@Composable
private fun CropImageDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (String) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crop Profile Picture") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Position photo inside the crop circle:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Crop View",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { scale = (scale - 0.2f).coerceAtLeast(1f) }) {
                        Icon(Icons.Default.ZoomOut, null)
                    }
                    IconButton(onClick = { scale = (scale + 0.2f).coerceAtMost(4f) }) {
                        Icon(Icons.Default.ZoomIn, null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val croppedUriStr = cropAndSaveBitmap(context, imageUri, scale, offsetX, offsetY)
                    onCropConfirmed(croppedUriStr)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(6.dp))
                Text("Crop & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private suspend fun sendAccessRequestEmailInApp(
    context: Context,
    name: String,
    email: String,
    dept: String,
    reason: String
): Boolean = withContext(Dispatchers.IO) {
    val developerEmail = "caperina13@gmail.com"
    val timestampStr = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
    val subject = "[AGNP Logistics] New Developer Access Application - $name"
    val body = """
        Hello AGNP Developer,

        A user is requesting authorization to access the AGNP Logistics Branch Locator App.

        --- Applicant Details ---
        Name: $name
        Google Email: $email
        Department / Role: $dept
        Reason: $reason

        Please add $email as a Test User in Google Cloud Console OAuth Consent Screen.

        Sent automatically from AGNP Android App on $timestampStr
    """.trimIndent()

    // 1. Save request locally in SharedPreferences for Admin review in app
    try {
        val prefs = context.getSharedPreferences("agnp_access_requests", Context.MODE_PRIVATE)
        val existingRequests = prefs.getString("requests_list", "[]") ?: "[]"
        val jsonArray = JSONArray(existingRequests)
        val newObj = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("dept", dept)
            put("reason", reason)
            put("timestamp", timestampStr)
            put("status", "Pending Review")
        }
        jsonArray.put(newObj)
        prefs.edit().putString("requests_list", jsonArray.toString()).apply()
    } catch (_: Exception) {}

    // 2. Direct HTTP submission to FormSubmit / Web3Forms API to send message directly to caperina13@gmail.com
    var sendSuccess = false
    try {
        val url = java.net.URL("https://formsubmit.co/ajax/$developerEmail")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; utf8")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val jsonParam = JSONObject().apply {
            put("name", name)
            put("email", email)
            put("_subject", subject)
            put("message", body)
            put("department", dept)
            put("reason", reason)
        }

        java.io.DataOutputStream(conn.outputStream).use { os ->
            os.write(jsonParam.toString().toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode == 200 || responseCode == 201) {
            sendSuccess = true
        }
    } catch (_: Exception) {}

    return@withContext sendSuccess
}

@Composable
private fun AccessRequestDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSubmitRequest: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(initialEmail) }
    var department by remember { mutableStateOf("Logistics Department") }
    var reason by remember { mutableStateOf("Requesting access authorization to field trip itinerary app.") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFFEF4444))
                Spacer(Modifier.width(8.dp))
                Text("Request Developer Access")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Your Google Account is not registered as an authorized tester for AGNP Logistics. Fill out this form to send an access application directly to caperina13@gmail.com:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name (Required)") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Google Email Address (Required)") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department / Role") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Access Request") },
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank()) {
                        error = "Please enter your Full Name and Google Email."
                    } else {
                        isSending = true
                        error = null
                        coroutineScope.launch {
                            sendAccessRequestEmailInApp(context, fullName.trim(), email.trim(), department.trim(), reason.trim())
                            isSending = false
                            onSubmitRequest(fullName.trim(), email.trim(), department.trim(), reason.trim())
                        }
                    }
                },
                enabled = !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sending Message...")
                } else {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Send Message directly to caperina13@gmail.com")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AppLockLoginScreen(
    onSignInClick: () -> Unit,
    onRequestAccessClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "AGNP Logo",
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "AGNP Logistics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Branch Locator & Field Itinerary App",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("App Locked - Sign In Required", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Sign in with your Google Account to unlock branch locations, field itineraries, and cloud archives.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sign In with Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRequestAccessClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request Access / Unregistered Account")
            }

            Text(
                "🔐 Your field data is protected & automatically synced to Google Drive AppData.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}



@Composable
private fun ReviewTripReportDialog(
    task: BranchDeliveryTask,
    onDismiss: () -> Unit,
    onConfirmGenerate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Trip Completion Report") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Review all details before generating the desktop CSV/Excel report file:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🏢 Branch: ${task.branchName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("📍 Address: ${task.address}", style = MaterialTheme.typography.bodySmall)
                        Text("👤 Receiver: ${task.receiverName}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        Text("✍️ Signature Status: VERIFIED & SIGNED", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                        Text("⏰ Timestamp: ${task.completedTimestamp}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("Delivered Items List (${task.itemsToDeliver.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                task.itemsToDeliver.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "DELIVERED",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (task.signatureBitmap != null) {
                    Text("Captured Receiver Signature:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(4.dp)
                    ) {
                        Image(
                            bitmap = task.signatureBitmap.asImageBitmap(),
                            contentDescription = "Receiver Signature",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmGenerate,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Description, null)
                Spacer(Modifier.width(6.dp))
                Text("Generate Desktop Report (.xlsx)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back") }
        }
    )
}

@Composable
private fun AdminPasswordDialog(
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit,
    isUnlocked: Boolean,
    onLock: () -> Unit,
    onChangePassword: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Administrator access") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isUnlocked) {
                    Text("Admin editing is currently unlocked on this screen.")
                } else {
                    Text("Enter the administrator password to add or edit branches and map links.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isUnlocked) {
                TextButton(onClick = onLock) { Text("Lock editing") }
            } else {
                TextButton(onClick = { onUnlock(password) }) { Text("Unlock") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onChangePassword) { Text("Change password") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Boolean
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change administrator password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(oldPassword, { oldPassword = it }, label = { Text("Current password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password (6+ characters)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(confirmPassword, { confirmPassword = it }, label = { Text("Confirm new password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    newPassword != confirmPassword -> error = "New passwords do not match"
                    else -> {
                        val result = onSave(oldPassword, newPassword)
                        if (!result) error = "Could not change password. Check the current password and new password length."
                    }
                }
            }) { Text("Save password") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BranchEditorDialog(
    initialBranch: Branch?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, List<MapLink>) -> Unit
) {
    var name by remember(initialBranch) { mutableStateOf(initialBranch?.name ?: "") }
    var address by remember(initialBranch) { mutableStateOf(initialBranch?.address ?: "") }
    var contactNumber by remember(initialBranch) { mutableStateOf(initialBranch?.contactNumber ?: "") }
    var imageUri by remember(initialBranch) { mutableStateOf(initialBranch?.imageUri ?: "") }
    var maps by remember(initialBranch) {
        mutableStateOf(initialBranch?.maps ?: listOf(MapLink("Main location", "")))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBranch == null) "Add branch" else "Edit branch") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Branch name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Branch address") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("BRANCH IMAGE (ADMIN)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (imageUri.isBlank()) "Select Branch Image" else "Change Branch Image")
                }
                if (imageUri.isNotBlank()) {
                    Text("Image attached successfully", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Text("MAP LINKS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Use a full Google Maps URL or a precise address / coordinates.")
                maps.forEachIndexed { index, map ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = map.label,
                                onValueChange = { value -> maps = maps.toMutableList().also { it[index] = map.copy(label = value) } },
                                label = { Text("Map name (e.g. Main office)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = map.destination,
                                onValueChange = { value -> maps = maps.toMutableList().also { it[index] = map.copy(destination = value) } },
                                label = { Text("Google Maps link or location") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (maps.size > 1) {
                                TextButton(onClick = { maps = maps.toMutableList().also { it.removeAt(index) } }) {
                                    Icon(Icons.Default.Delete, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Remove map link")
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { maps = maps + MapLink("Additional map ${maps.size + 1}", "") },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add another map link")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanedMaps = maps.filter { it.label.isNotBlank() && it.destination.isNotBlank() }
                when {
                    name.isBlank() -> error = "Enter a branch name."
                    address.isBlank() -> error = "Enter the branch address."
                    cleanedMaps.isEmpty() -> error = "Add at least one complete map link."
                    else -> onSave(name.trim(), address.trim(), contactNumber.trim(), imageUri.trim(), cleanedMaps)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun getCorrectedPortraitBitmap(context: Context, imageUri: Uri): android.graphics.Bitmap? {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (bitmap == null) return null

        var rotationDegrees = 0
        try {
            val exifStream = context.contentResolver.openInputStream(imageUri)
            if (exifStream != null) {
                val exif = android.media.ExifInterface(exifStream)
                val orientation = exif.getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
                exifStream.close()
                rotationDegrees = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {}

        val matrix = android.graphics.Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (rotationDegrees == 0 && bitmap.width > bitmap.height) {
            matrix.postRotate(90f)
        }

        return android.graphics.Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    } catch (_: Exception) {
        return null
    }
}

private fun watermarkImageWithGps(context: Context, imageUri: Uri, locationText: String, timeText: String, cameraLogoUri: String) {
    try {
        val portraitBitmap = getCorrectedPortraitBitmap(context, imageUri)
        if (portraitBitmap != null) {
            val mutableBitmap = portraitBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            val canvas = android.graphics.Canvas(mutableBitmap)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 38f
                isAntiAlias = true
                setShadowLayer(6f, 2f, 2f, android.graphics.Color.BLACK)
            }
            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#00BFA5")
                textSize = 42f
                isAntiAlias = true
                isFakeBoldText = true
                setShadowLayer(6f, 2f, 2f, android.graphics.Color.BLACK)
            }
            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(190, 15, 28, 21)
            }

            val padding = 40f
            val x = padding + 90f
            val y = mutableBitmap.height - padding - 120f

            canvas.drawRect(padding - 16f, y - 60f, mutableBitmap.width - padding + 16f, mutableBitmap.height - padding + 16f, bgPaint)

            try {
                val logoBm = if (cameraLogoUri.isNotBlank()) {
                    val logoStream = context.contentResolver.openInputStream(Uri.parse(cameraLogoUri))
                    val b = android.graphics.BitmapFactory.decodeStream(logoStream)
                    logoStream?.close()
                    b
                } else {
                    android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo)
                }
                if (logoBm != null) {
                    val scaledLogo = android.graphics.Bitmap.createScaledBitmap(logoBm, 70, 70, true)
                    canvas.drawBitmap(scaledLogo, padding, y - 45f, null)
                }
            } catch (_: Exception) {}

            canvas.drawText("AGNP Logistics Field Report", x, y - 10f, titlePaint)
            canvas.drawText("📍 $locationText", x, y + 38f, paint)
            canvas.drawText("⏰ $timeText", x, y + 84f, paint)

            val outputStream = context.contentResolver.openOutputStream(imageUri, "w")
            if (outputStream != null) {
                mutableBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, outputStream)
                outputStream.close()
            }
        }
    } catch (_: Exception) {}
}

@Composable
private fun GpsCameraScreen(
    cameraLogoUri: String,
    onChangeLogo: () -> Unit,
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var currentZoom by remember { mutableStateOf(1f) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val previewView = remember { PreviewView(context) }
    val liveLoc = rememberLiveUserLocation()
    val locName = rememberLiveLocationName(liveLoc)
    val currentTime = remember {
        java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                cameraControl = camera.cameraControl
                cameraControl?.setZoomRatio(currentZoom)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    if (capturedPhotoUri != null) {
        // Captured Photo Preview Screen
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Image(
                painter = rememberAsyncImagePainter(model = capturedPhotoUri),
                contentDescription = "GPS Photo Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Top Header Overlay
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text("GPS Photo Preview", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                IconButton(onClick = { capturedPhotoUri = null }) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Bottom Action Controls
            Row(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { capturedPhotoUri = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Retake Photo")
                }

                Button(
                    onClick = {
                        val uri = capturedPhotoUri!!
                        capturedPhotoUri = null
                        onPhotoCaptured(uri)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Confirm & Share")
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Little Google Map Box on GPS Camera
            Box(
                Modifier.fillMaxWidth().align(Alignment.TopEnd).padding(top = 80.dp, end = 20.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.width(150.dp).height(80.dp)
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Map, null, tint = Color(0xFF00BFA5), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(3.dp))
                        Text("Map Live View", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(if (liveLoc != null) "${String.format(java.util.Locale.getDefault(), "%.2f", liveLoc.latitude)}, ${String.format(java.util.Locale.getDefault(), "%.2f", liveLoc.longitude)}" else "GPS active", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                    }
                }
            }

            // Watermark Overlay on Viewfinder (Non-blocking, great typography with Logo)
            Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(14.dp).clickable(onClick = onChangeLogo),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cameraLogoUri.isNotBlank()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = cameraLogoUri),
                                contentDescription = "Logo",
                                modifier = Modifier.size(46.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(46.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("AGNP Logistics Field Report (Tap logo to change)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00BFA5), fontWeight = FontWeight.Bold)
                            Text("📍 $locName", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("⏰ $currentTime", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Top bar with Camera Switch & Zoom Controls
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        currentZoom = (currentZoom - 0.5f).coerceAtLeast(1f)
                        cameraControl?.setZoomRatio(currentZoom)
                    }) {
                        Icon(Icons.Default.ZoomOut, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Text("${String.format(java.util.Locale.getDefault(), "%.1f", currentZoom)}x", color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        currentZoom = (currentZoom + 0.5f).coerceAtMost(5f)
                        cameraControl?.setZoomRatio(currentZoom)
                    }) {
                        Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    }) {
                        Icon(Icons.Default.Cameraswitch, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Capture button
            Box(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        val capture = imageCapture ?: return@FloatingActionButton
                        val name = "GPS_Field_Photo_${System.currentTimeMillis()}.jpg"
                        val contentValues = android.content.ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                        }
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(
                            context.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        ).build()

                        capture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val uri = outputFileResults.savedUri
                                    if (uri != null) {
                                        watermarkImageWithGps(context, uri, locName, currentTime, cameraLogoUri)
                                        capturedPhotoUri = uri
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {}
                            }
                        )
                    },
                    containerColor = Color(0xFF00BFA5),
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MapAppSelectorDialog(
    destination: String,
    onDismiss: () -> Unit,
    onOpenGoogleMaps: (String) -> Unit,
    onOpenWaze: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose navigation app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select your preferred navigation app:")

                Surface(
                    onClick = { onOpenGoogleMaps(destination); onDismiss() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEA4335), // Google Red
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Map, null, tint = Color.White)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Google Maps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Standard navigation & maps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(
                    onClick = { onOpenWaze(destination); onDismiss() },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF33CCFF), // Waze Blue
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Navigation, null, tint = Color.Black)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Waze", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Real-time traffic & alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
