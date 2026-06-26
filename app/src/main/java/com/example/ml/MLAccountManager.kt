package com.example.ml

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.example.root.RootCommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupAccount(
    val id: String,
    val name: String,
    val date: String,
    val path: String,
    val isSimulated: Boolean
)

data class ExecutionLog(
    val timestamp: String,
    val type: LogType,
    val message: String
)

enum class LogType {
    INFO, ROOT, ERROR, SUCCESS
}

class MLAccountManager(private val context: Context) {
    private val TAG = "MLAccountManager"
    
    // Core state flows
    private val _backups = MutableStateFlow<List<BackupAccount>>(emptyList())
    val backups: StateFlow<List<BackupAccount>> = _backups.asStateFlow()

    private val _isRootMode = MutableStateFlow(false)
    val isRootMode: StateFlow<Boolean> = _isRootMode.asStateFlow()

    private val _logs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logs: StateFlow<List<ExecutionLog>> = _logs.asStateFlow()

    private val _isOperationRunning = MutableStateFlow(false)
    val isOperationRunning: StateFlow<Boolean> = _isOperationRunning.asStateFlow()

    // Create Account countdown timer: standard 10 minutes in milliseconds (600,000 ms)
    private val _createAccountTimer = MutableStateFlow<Long?>(null)
    val createAccountTimer: StateFlow<Long?> = _createAccountTimer.asStateFlow()

    private val _isMlForeground = MutableStateFlow(false)
    val isMlForeground: StateFlow<Boolean> = _isMlForeground.asStateFlow()

    private val _mlForegroundDuration = MutableStateFlow(0L)
    val mlForegroundDuration: StateFlow<Long> = _mlForegroundDuration.asStateFlow()

    private var timerRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Automatically check if the device has superuser binary
        val rootStatus = RootCommandExecutor.isRootAvailable()
        _isRootMode.value = rootStatus
        addLog(
            LogType.INFO, 
            if (rootStatus) "Akses Root DISETUJUI. Berjalan di Mode ROOT." 
            else "Akses Root TIDAK Ditemukan. Berjalan di Mode Simulasi (Developer Model)."
        )
        refreshBackups()
    }

    fun setRootMode(enabled: Boolean) {
        _isRootMode.value = enabled
        addLog(LogType.INFO, "Beralih mode ke: ${if (enabled) "ROOT MODE" else "SIMULATION MODE"}")
        refreshBackups()
    }

    fun verifyRootAccess(onResult: (Boolean) -> Unit) {
        _isOperationRunning.value = true
        addLog(LogType.INFO, "Melakukan verifikasi integritas biner akses root (su)...")
        CoroutineScope(Dispatchers.IO).launch {
            val isAvailable = RootCommandExecutor.isRootAvailable()
            if (!isAvailable) {
                addLog(LogType.ERROR, "Verifikasi Gagal: File biner 'su' tidak terdeteksi di system path.")
                _isOperationRunning.value = false
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
                return@launch
            }
            
            val testResult = RootCommandExecutor.executeRootCommand("id")
            val isRootReal = testResult.isSuccess && (testResult.output.contains("uid=0") || testResult.output.contains("root"))
            
            if (isRootReal) {
                addLog(LogType.SUCCESS, "Verifikasi Berhasil: Hak akses Superuser (uid=0) dikonfirmasi.")
            } else {
                addLog(LogType.ERROR, "Verifikasi Gagal: Biner 'su' ada namun tidak dapat diaktifkan atau akses ditolak.")
            }
            _isOperationRunning.value = false
            withContext(Dispatchers.Main) {
                onResult(isRootReal)
            }
        }
    }

    fun addLog(type: LogType, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = ExecutionLog(time, type, message)
        _logs.value = listOf(newLog) + _logs.value.take(49) // Keep last 50 logs
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addLog(LogType.INFO, "Log berhasil dibersihkan.")
    }

    /**
     * Refreshes the backup accounts list
     */
    fun refreshBackups() {
        CoroutineScope(Dispatchers.IO).launch {
            if (_isRootMode.value) {
                _backups.value = getRootBackups()
            } else {
                _backups.value = getSimulatedBackups()
            }
        }
    }

    private fun getRootBackups(): List<BackupAccount> {
        addLog(LogType.ROOT, "su -c \"mkdir -p /data/tmp/account && ls -1 /data/tmp/account/\"")
        
        // 1. Ensure backup directory exists
        RootCommandExecutor.executeRootCommand("mkdir -p /data/tmp/account")
        
        // 2. List folders inside /data/tmp/account/
        val result = RootCommandExecutor.executeRootCommand("ls -1 /data/tmp/account")
        if (!result.isSuccess || result.output.isEmpty()) {
            return emptyList()
        }

        val folders = result.output.split("\n").filter { it.trim().isNotEmpty() }
        val accounts = mutableListOf<BackupAccount>()

        for (folder in folders) {
            val sanitized = folder.trim()
            if (sanitized.isEmpty()) continue

            // Read metadata json if available
            val catMeta = RootCommandExecutor.executeRootCommand("cat /data/tmp/account/$sanitized/meta.json")
            var displayName = sanitized
            var dateStr = "Unknown Date"

            if (catMeta.isSuccess && catMeta.output.isNotEmpty()) {
                val metadata = parseMetaJson(catMeta.output)
                displayName = metadata["name"] ?: sanitized
                dateStr = metadata["date"] ?: "Unknown Date"
            } else {
                // Get folder timestamp via ls -ld
                val folderStat = RootCommandExecutor.executeRootCommand("stat -c %y /data/tmp/account/$sanitized")
                if (folderStat.isSuccess && folderStat.output.isNotEmpty()) {
                    dateStr = folderStat.output.substringBefore(".")
                }
            }

            accounts.add(
                BackupAccount(
                    id = sanitized,
                    name = displayName,
                    date = dateStr,
                    path = "/data/tmp/account/$sanitized",
                    isSimulated = false
                )
            )
        }
        return accounts
    }

    private fun getSimulatedBackups(): List<BackupAccount> {
        val simDir = File(context.filesDir, "simulated_accounts")
        if (!simDir.exists()) {
            simDir.mkdirs()
            // Create a default mock backup for onboarding demo
            val sampleDir = File(simDir, "smurf_premium_ml")
            sampleDir.mkdirs()
            val sampleDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            File(sampleDir, "meta.json").writeText("{\"name\":\"Smurf Premium ML\",\"date\":\"$sampleDate\"}")
        }

        val files = simDir.listFiles { file -> file.isDirectory } ?: return emptyList()
        return files.map { file ->
            var displayName = file.name
            var dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
            
            val metaFile = File(file, "meta.json")
            if (metaFile.exists()) {
                try {
                    val metadata = parseMetaJson(metaFile.readText())
                    displayName = metadata["name"] ?: file.name
                    dateStr = metadata["date"] ?: dateStr
                } catch (e: Exception) {
                    // Ignore
                }
            }

            BackupAccount(
                id = file.name,
                name = displayName,
                date = dateStr,
                path = file.absolutePath,
                isSimulated = true
            )
        }
    }

    private fun parseMetaJson(jsonStr: String): Map<String, String> {
        val results = mutableMapOf<String, String>()
        val nameRegex = """"\s*name\s*"\s*:\s*"\s*([^"]+)\s*"""".toRegex()
        val dateRegex = """"\s*date\s*"\s*:\s*"\s*([^"]+)\s*"""".toRegex()
        
        nameRegex.find(jsonStr)?.groupValues?.get(1)?.let { results["name"] = it }
        dateRegex.find(jsonStr)?.groupValues?.get(1)?.let { results["date"] = it }
        return results
    }

    /**
     * Backup Current ML Account
     */
    fun backupAccount(accountName: String, onComplete: (Boolean, String) -> Unit) {
        val sanitized = accountName.trim().replace(Regex("[^a-zA-Z0-9_]"), "_")
        if (sanitized.isEmpty()) {
            onComplete(false, "Nama akun tidak boleh kosong atau mengandung karakter ilegal.")
            return
        }

        _isOperationRunning.value = true
        CoroutineScope(Dispatchers.IO).launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            if (_isRootMode.value) {
                addLog(LogType.INFO, "Memulai Backup Akun Mobile Legends: $accountName")
                
                // 1. Ensure target backup dir exists
                val createDirCmd = "mkdir -p /data/tmp/account/$sanitized"
                addLog(LogType.ROOT, "su -c \"$createDirCmd\"")
                var r = RootCommandExecutor.executeRootCommand(createDirCmd)
                if (!r.isSuccess) {
                    addLog(LogType.ERROR, "Gagal membuat direktori backup: ${r.error}")
                    _isOperationRunning.value = false
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Gagal membuat folder backup di /data/tmp")
                    }
                    return@launch
                }

                // 2. Double check if ML folder exists, if not warn
                val mlCheck = RootCommandExecutor.executeRootCommand("[ -d /data/data/com.mobile.legends ] && echo 'yes'")
                if (mlCheck.output != "yes") {
                    addLog(LogType.ERROR, "Folder /data/data/com.mobile.legends tidak ditemukan. Apakah Mobile Legends terpasang?")
                }

                // 3. Clear backup content if exists and copy files recursively
                val copyCmd = "rm -rf /data/tmp/account/$sanitized/* && cp -R /data/data/com.mobile.legends/. /data/tmp/account/$sanitized/"
                addLog(LogType.ROOT, "su -c \"$copyCmd\"")
                r = RootCommandExecutor.executeRootCommand(copyCmd)
                if (!r.isSuccess) {
                    addLog(LogType.ERROR, "Gagal menyalin data: ${r.error}")
                    _isOperationRunning.value = false
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Gagal menyalin file data Mobile Legends. Pastikan izin root diberikan.")
                    }
                    return@launch
                }

                // 4. Create base64 meta json to bypass shell escapes
                val metaJson = "{\"name\":\"$accountName\",\"date\":\"$dateStr\"}"
                val b64Str = Base64.encodeToString(metaJson.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                val writeMetaCmd = "echo -n '$b64Str' | base64 -d > /data/tmp/account/$sanitized/meta.json"
                addLog(LogType.ROOT, "su -c \"$writeMetaCmd\"")
                RootCommandExecutor.executeRootCommand(writeMetaCmd)

                addLog(LogType.SUCCESS, "Backup Akun [$accountName] Berhasil disimpan secara offline!")
                _isOperationRunning.value = false
                refreshBackups()
                withContext(Dispatchers.Main) {
                    onComplete(true, "Akun $accountName berhasil dibackup di /data/tmp/account/$sanitized!")
                }

            } else {
                // Simulation Mode
                addLog(LogType.INFO, "[SIMULASI] Memulai backup $accountName...")
                java.lang.Thread.sleep(1500)
                
                val simDir = File(context.filesDir, "simulated_accounts")
                val backupFolder = File(simDir, sanitized)
                backupFolder.mkdirs()
                
                val metaFile = File(backupFolder, "meta.json")
                val metaJson = "{\"name\":\"$accountName\",\"date\":\"$dateStr\"}"
                metaFile.writeText(metaJson)
                
                // Write fake subfiles to simulate data volume
                File(backupFolder, "shared_prefs").mkdir()
                File(backupFolder, "files").mkdir()
                File(File(backupFolder, "files"), "Preferences").writeText("Mock User ML Game ID config")

                addLog(LogType.SUCCESS, "[SIMULASI] Akun $accountName berhasil disalin ke internal simulator sandbox.")
                _isOperationRunning.value = false
                refreshBackups()
                withContext(Dispatchers.Main) {
                    onComplete(true, "[Simulasi] Akun $accountName di-backup berhasil.")
                }
            }
        }
    }

    /**
     * Switch Mobile Legends Account (Restore)
     */
    fun switchAccount(backup: BackupAccount, onComplete: (Boolean, String) -> Unit) {
        _isOperationRunning.value = true
        CoroutineScope(Dispatchers.IO).launch {
            if (_isRootMode.value) {
                addLog(LogType.INFO, "Memulai peralihan (switch) ke akun: ${backup.name}")

                // 1. Backup SD card resources first to avoid redownloads
                addLog(LogType.INFO, "Mengamankan Asset Sumber Daya Game MLBB (Saves mobile.legends -> mobile.legends.backup)")
                val renameSdkCmd = "mv /sdcard/Android/data/com.mobile.legends /sdcard/Android/data/com.mobile.legends.backup"
                addLog(LogType.ROOT, "su -c \"$renameSdkCmd\"")
                var r = RootCommandExecutor.executeRootCommand(renameSdkCmd)
                if (!r.isSuccess) {
                    addLog(LogType.INFO, "Gagal memindah atau folder resources SDCard tidak ditemukan di /sdcard/. Mencoba path emulated...")
                    val renameSdkEmulatedCmd = "mv /storage/emulated/0/Android/data/com.mobile.legends /storage/emulated/0/Android/data/com.mobile.legends.backup"
                    addLog(LogType.ROOT, "su -c \"$renameSdkEmulatedCmd\"")
                    RootCommandExecutor.executeRootCommand(renameSdkEmulatedCmd)
                }

                // 2. Clear current app data (pm clear)
                addLog(LogType.INFO, "Membersihkan package data Mobile Legends saat ini...")
                val clearCmd = "pm clear com.mobile.legends"
                addLog(LogType.ROOT, "su -c \"$clearCmd\"")
                r = RootCommandExecutor.executeRootCommand(clearCmd)
                if (!r.isSuccess) {
                    addLog(LogType.ERROR, "Gagal membersihkan data aplikasi: ${r.error}")
                    // Fallback to restore GMS resources back directly
                    restoreResourcesBack()
                    _isOperationRunning.value = false
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Gagal melakukan 'pm clear'. Pastikan paket Mobile Legends terpasang.")
                    }
                    return@launch
                }

                // Wait a moment for OS to clean up directories
                java.lang.Thread.sleep(1000)

                // 3. Re-create com.mobile.legends data folder and copy backup
                addLog(LogType.INFO, "Menyalin data akun terpilih ke /data/data/com.mobile.legends/...")
                val restoreCmd = "mkdir -p /data/data/com.mobile.legends && cp -R /data/tmp/account/${backup.id}/. /data/data/com.mobile.legends/"
                addLog(LogType.ROOT, "su -c \"$restoreCmd\"")
                r = RootCommandExecutor.executeRootCommand(restoreCmd)
                if (!r.isSuccess) {
                    addLog(LogType.ERROR, "Gagal memulihkan file data backup: ${r.error}")
                    restoreResourcesBack()
                    _isOperationRunning.value = false
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Gagal menyalin folder backup ke direktori data Mobile Legends.")
                    }
                    return@launch
                }

                // 4. CHOWN permissions to ensure ML has permission to access files (Crucial!)
                addLog(LogType.INFO, "Memperbaiki hak milik (ownership UID/GID) file game...")
                val chownCmd = "ML_UID=\$(stat -c %u /data/data/com.mobile.legends); chown -R \$ML_UID:\$ML_UID /data/data/com.mobile.legends"
                addLog(LogType.ROOT, "su -c \"$chownCmd\"")
                r = RootCommandExecutor.executeRootCommand(chownCmd)
                if (!r.isSuccess) {
                    addLog(LogType.ERROR, "Gagal menyesuaikan ownership UI: ${r.error}. Menghubungi selinux...")
                }
                
                // Optional SELinux context restorecon just to be sure
                RootCommandExecutor.executeRootCommand("restorecon -R /data/data/com.mobile.legends")

                // 5. Restore SD card resource folders
                restoreResourcesBack()

                addLog(LogType.SUCCESS, "Sukses Switch ke Akun [${backup.name}]. Silakan buka Mobile Legends!")
                _isOperationRunning.value = false
                withContext(Dispatchers.Main) {
                    onComplete(true, "Akun ${backup.name} berhasil dimuat kembali. Silakan jalankan Mobile Legends!")
                }

            } else {
                // Simulation Mode
                addLog(LogType.INFO, "[SIMULASI] Menyamarkan SDCard data com.mobile.legends...")
                java.lang.Thread.sleep(800)
                addLog(LogType.INFO, "[SIMULASI] Menghapus data aplikasi (pm clear com.mobile.legends)")
                java.lang.Thread.sleep(1000)
                addLog(LogType.INFO, "[SIMULASI] Menyalin file backup [${backup.name}] ke data/data...")
                java.lang.Thread.sleep(800)
                addLog(LogType.INFO, "[SIMULASI] Menyalurkan UID & GID system perm...")
                java.lang.Thread.sleep(400)
                addLog(LogType.SUCCESS, "[SIMULASI] Akun ${backup.name} berhasil ditukar dalam database simulator.")
                _isOperationRunning.value = false
                withContext(Dispatchers.Main) {
                    onComplete(true, "[Simulasi] Berhasil memuat Akun ${backup.name}!")
                }
            }
        }
    }

    private fun restoreResourcesBack() {
        addLog(LogType.INFO, "Mengembalikan folder data resources asset SDCard...")
        val restoreSdkCmd = "rm -rf /sdcard/Android/data/com.mobile.legends && mv /sdcard/Android/data/com.mobile.legends.backup /sdcard/Android/data/com.mobile.legends"
        addLog(LogType.ROOT, "su -c \"$restoreSdkCmd\"")
        val r = RootCommandExecutor.executeRootCommand(restoreSdkCmd)
        if (!r.isSuccess) {
            val restoreEmulatedCmd = "rm -rf /storage/emulated/0/Android/data/com.mobile.legends && mv /storage/emulated/0/Android/data/com.mobile.legends.backup /storage/emulated/0/Android/data/com.mobile.legends"
            addLog(LogType.ROOT, "su -c \"$restoreEmulatedCmd\"")
            RootCommandExecutor.executeRootCommand(restoreEmulatedCmd)
        }
    }

    /**
     * Create New Account flow. Disables GMS, clears data, triggers ML.
     */
    fun createNewAccount(onMLOpened: (Boolean, String) -> Unit) {
        _isOperationRunning.value = true
        CoroutineScope(Dispatchers.IO).launch {
            if (_isRootMode.value) {
                addLog(LogType.INFO, "Menjalankan Prosedur Pembuatan Akun Baru (Instant Guest Smurf)...")

                // 1. Rename resource folder so it is not deleted
                addLog(LogType.INFO, "Mengamankan data Resources agar tidak perlu download ulang...")
                val renameCmd = "mv /sdcard/Android/data/com.mobile.legends /sdcard/Android/data/com.mobile.legends.backup"
                addLog(LogType.ROOT, "su -c \"$renameCmd\"")
                var r = RootCommandExecutor.executeRootCommand(renameCmd)
                if (!r.isSuccess) {
                    val renameEmulatedCmd = "mv /storage/emulated/0/Android/data/com.mobile.legends /storage/emulated/0/Android/data/com.mobile.legends.backup"
                    addLog(LogType.ROOT, "su -c \"$renameEmulatedCmd\"")
                    RootCommandExecutor.executeRootCommand(renameEmulatedCmd)
                }

                // 2. Disable Google Play Services (GMS)!
                addLog(LogType.INFO, "Menonaktifkan Layanan Google Play (com.google.android.gms)...")
                val disableGmsCmd = "pm disable com.google.android.gms"
                addLog(LogType.ROOT, "su -c \"$disableGmsCmd\"")
                r = RootCommandExecutor.executeRootCommand(disableGmsCmd)
                if (!r.isSuccess) {
                    // Try alternative user disable cmd
                    val disableGmsUserCmd = "pm disable-user --user 0 com.google.android.gms"
                    addLog(LogType.ROOT, "su -c \"$disableGmsUserCmd\"")
                    RootCommandExecutor.executeRootCommand(disableGmsUserCmd)
                }

                // 3. Clear current ML account data
                addLog(LogType.INFO, "Menghapus identitas data Mobile Legends sebelumnya (pm clear)...")
                val clearCmd = "pm clear com.mobile.legends"
                addLog(LogType.ROOT, "su -c \"$clearCmd\"")
                RootCommandExecutor.executeRootCommand(clearCmd)

                // Wait for OS sync
                java.lang.Thread.sleep(1000)

                // 4. Restore original high-volume game resources so user doesn't download resources in new account!
                restoreResourcesBack()

                // 5. Instantly open Mobile Legends app using launcher Intent or monkey tool
                addLog(LogType.INFO, "Meluncurkan Game Mobile Legends secara otomatis...")
                val openMLCmd = "monkey -p com.mobile.legends -c android.intent.category.LAUNCHER 1"
                addLog(LogType.ROOT, "su -c \"$openMLCmd\"")
                RootCommandExecutor.executeRootCommand(openMLCmd)

                // Attempt native android lunch fallback in main UI thread
                _isOperationRunning.value = false
                launchMLBBApp()

                // 6. Start countdown timer of 10 minutes to auto-enable Google Play Services
                startGmsAutoEnableTimer(600000) // 10 minutes

                withContext(Dispatchers.Main) {
                    onMLOpened(true, "Layanan Google Play dinonaktifkan. Mobile Legends diluncurkan. Anda punya waktu 10 menit untuk membuat akun baru!")
                }

            } else {
                // Simulation Mode
                addLog(LogType.INFO, "[SIMULASI] Menyembunyikan resource MLBB...")
                java.lang.Thread.sleep(500)
                addLog(LogType.INFO, "[SIMULASI] Menonaktifkan Google Play Services (com.google.android.gms)...")
                java.lang.Thread.sleep(500)
                addLog(LogType.INFO, "[SIMULASI] Membersihkan identitas Mobile Legends...")
                java.lang.Thread.sleep(500)
                addLog(LogType.INFO, "[SIMULASI] Memasang kembali folder Resources game...")
                java.lang.Thread.sleep(500)
                addLog(LogType.SUCCESS, "[SIMULASI] Menyalakan game Mobile Legends (Simulasi Launcher)...")
                _isOperationRunning.value = false

                startGmsAutoEnableTimer(600000) // 10 minutes (600,000 ms)
                withContext(Dispatchers.Main) {
                    onMLOpened(true, "[Simulasi] GMS Dinonaktifkan, Mobile Legends otomatis di-boot dalam sandbox.")
                }
            }
        }
    }

    private fun launchMLBBApp() {
        mainHandler.post {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.mobile.legends")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal meluncurkan game via package manager launcher", e)
            }
        }
    }

    /**
     * Starts the timer that will automatically enable GMS again after a specific duration
     * Also manages detecting if Mobile Legends is running in the foreground (UnityPlayerActivity).
     * If ML is in the foreground:
     * - The timer keeps counting down.
     * - If the user has opened Mobile Legends for > 3 minutes (180,000ms), and reaches > 5 minutes (300,000ms) of GMS disabled duration,
     *   we maintain the disabled state by ensuring we don't auto-re-enable GMS until they leave Mobile Legends.
     *   Essentially, if they spend at least 3 minutes inside ML and total time exceeds 5 mins, we continuously wait for them to exit ML before auto-enabling GMS,
     *   allowing unlimited play time as long as ML is in foreground.
     */
    private fun startGmsAutoEnableTimer(durationMs: Long) {
        cancelExistingTimer()
        _createAccountTimer.value = durationMs
        _isMlForeground.value = false
        _mlForegroundDuration.value = 0L
        addLog(LogType.INFO, "Penghitung waktu GMS otomatis menyala: ${durationMs / 60000} menit tersisa.")

        val tickInterval = 1000L
        var accumulatedMlForegroundTime = 0L

        timerRunnable = object : Runnable {
            override fun run() {
                val current = _createAccountTimer.value
                
                // Perform check on foreground activity in IO Thread to avoid freezing the system UI
                CoroutineScope(Dispatchers.IO).launch {
                    val isMlRunning = checkIfMlInForeground()
                    
                    withContext(Dispatchers.Main) {
                        _isMlForeground.value = isMlRunning
                        if (isMlRunning) {
                            accumulatedMlForegroundTime += tickInterval
                            _mlForegroundDuration.value = accumulatedMlForegroundTime
                        }
                        
                        val totalTimeElapsedFromStart = 600000L - (current ?: 0L)
                        
                        // Condition check:
                        // "selama Mobile Legends di Jalankan com.unity3d.player.UnityPlayerActivity Layanan Google Play akan terus di Disable dengan catatan user harus lebih dari 3 menit membuka Mobile Legends. jika Lebih 5 menit, ini akan berlaku."
                        // Under 3 minutes of opening ML, or total elapsed time under 5 minutes, we count down and auto-finish standard 10m timer normally.
                        // If user opened ML for > 3 minutes AND total GMS disable time is > 5 minutes (300,000ms), GMS remains disabled as long as ML is playing.
                        val isExtendEligible = (accumulatedMlForegroundTime > 180000L) && (totalTimeElapsedFromStart > 300000L)
                        
                        if (isMlRunning && isExtendEligible) {
                            // If ML is running and they are eligible, keep the countdown suspended or locked at a safe mock remaining value (e.g., 2 minutes / 120,000ms remaining)
                            // continuously disabling GMS, so it never reaches <= 0 until they exit ML.
                            _createAccountTimer.value = 120000L // Lock or reset to 2 minutes
                            _createAccountTimer.value?.let { nextTimer ->
                                mainHandler.postDelayed(timerRunnable!!, tickInterval)
                            }
                        } else {
                            if (current != null) {
                                if (current <= tickInterval) {
                                    // Timer expired naturally
                                    _createAccountTimer.value = null
                                    enableGooglePlayServices(manual = false)
                                } else {
                                    _createAccountTimer.value = current - tickInterval
                                    mainHandler.postDelayed(timerRunnable!!, tickInterval)
                                }
                            }
                        }
                    }
                }
            }
        }
        mainHandler.postDelayed(timerRunnable!!, tickInterval)
    }

    /**
     * Checks if Mobile Legends (com.unity3d.player.UnityPlayerActivity) is in the foreground.
     */
    private fun checkIfMlInForeground(): Boolean {
        return if (_isRootMode.value) {
            // Using real dumpsys tool to detect foreground activity
            val result = RootCommandExecutor.executeRootCommand("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp|mResumedActivity'")
            if (result.isSuccess && result.output.isNotEmpty()) {
                result.output.contains("com.unity3d.player.UnityPlayerActivity") || 
                result.output.contains("com.mobile.legends")
            } else {
                // Fallback to check via PS command or am monitor
                val psResult = RootCommandExecutor.executeRootCommand("ps -A | grep com.mobile.legends")
                psResult.isSuccess && psResult.output.isNotEmpty()
            }
        } else {
            // Simulation Mode: check if simulated duration timer has been activated
            true
        }
    }

    private fun cancelExistingTimer() {
        timerRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        timerRunnable = null
        _createAccountTimer.value = null
    }

    /**
     * Terminate countdown early and re-enable GMS
     */
    fun cancelCreatingAccountAndEnableGms() {
        addLog(LogType.INFO, "Menghentikan pembuatan akun baru secara manual oleh Pengguna.")
        cancelExistingTimer()
        enableGooglePlayServices(manual = true)
    }

    private fun enableGooglePlayServices(manual: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            if (_isRootMode.value) {
                addLog(LogType.INFO, "Mengaktifkan kembali Layanan Google Play (com.google.android.gms)...")
                val enableGmsCmd = "pm enable com.google.android.gms"
                addLog(LogType.ROOT, "su -c \"$enableGmsCmd\"")
                val r = RootCommandExecutor.executeRootCommand(enableGmsCmd)
                if (r.isSuccess) {
                    addLog(LogType.SUCCESS, "Layanan Google Play (GMS) BERHASIL Diaktifkan kembali!")
                } else {
                    addLog(LogType.ERROR, "Gagal mengaktifkan kembali GMS: ${r.error}. Coba lakukan reboot perangkat jika Play Store bermasalah!")
                }
            } else {
                addLog(LogType.SUCCESS, "[SIMULASI] Layanan Google Play (com.google.android.gms) dipulihkan kembali.")
            }
        }
    }

    fun deleteBackup(backup: BackupAccount, onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            if (_isRootMode.value) {
                addLog(LogType.ROOT, "su -c \"rm -rf /data/tmp/account/${backup.id}\"")
                val r = RootCommandExecutor.executeRootCommand("rm -rf /data/tmp/account/${backup.id}")
                if (r.isSuccess) {
                    addLog(LogType.SUCCESS, "Backup Akun [${backup.name}] Berhasil di Hapus.")
                    refreshBackups()
                    withContext(Dispatchers.Main) {
                        onComplete(true)
                    }
                } else {
                    addLog(LogType.ERROR, "Gagal menghapus backup: ${r.error}")
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            } else {
                val simDir = File(context.filesDir, "simulated_accounts")
                val folder = File(simDir, backup.id)
                val success = folder.deleteRecursively()
                if (success) {
                    addLog(LogType.SUCCESS, "[SIMULASI] Backup Akun [${backup.name}] Berhasil di Hapus.")
                    refreshBackups()
                    withContext(Dispatchers.Main) {
                        onComplete(true)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            }
        }
    }
}
