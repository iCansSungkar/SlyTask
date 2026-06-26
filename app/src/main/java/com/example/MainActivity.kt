package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.BackupAccount
import com.example.ml.ExecutionLog
import com.example.ml.LogType
import com.example.ml.MLAccountManager
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private lateinit var accountManager: MLAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accountManager = MLAccountManager(applicationContext)

        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var currentLang by remember { mutableStateOf("ID") }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainBentoDashboard(
                        manager = accountManager,
                        isDarkMode = isDarkMode,
                        onThemeChanged = { isDarkMode = it },
                        currentLang = currentLang,
                        onLangChanged = { currentLang = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainBentoDashboard(
    manager: MLAccountManager,
    isDarkMode: Boolean = true,
    onThemeChanged: (Boolean) -> Unit = {},
    currentLang: String = "ID",
    onLangChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Collecting reactive states from backend manager
    val listBackups by manager.backups.collectAsState()
    val isRoot by manager.isRootMode.collectAsState()
    val listLogs by manager.logs.collectAsState()
    val isBusy by manager.isOperationRunning.collectAsState()
    val activeTimer by manager.createAccountTimer.collectAsState()
    val isMlForeground by manager.isMlForeground.collectAsState()
    val mlForegroundDuration by manager.mlForegroundDuration.collectAsState()

    // Dialog state management
    var showBackupDialog by remember { mutableStateOf(false) }
    var currentBackupNameInput by remember { mutableStateOf("") }
    
    var showSwitchConfirmDialog by remember { mutableStateOf<BackupAccount?>(null) }
    var showCreateConfirmDialog by remember { mutableStateOf(false) }

    // Intercept verifikasi Root
    var isVerifyingRoot by remember { mutableStateOf(false) }
    var showRootErrorDialog by remember { mutableStateOf(false) }

    // Scrollstate for terminal log auto-scroll simulation
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // ==========================================
            // HEADER BENTO BOX (Branding & Connection)
            // ==========================================
            BentoSectionCard(
                glowColor = if (isRoot) NeonMint else SoftCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SlyTask",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = getTranslation("subtitle", currentLang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        
                        // Status Badge
                        CustomBadge(
                            text = if (isRoot) "ROOT MODE" else "SIMULATED",
                            backgroundColor = if (isRoot) NeonMintTranslucent else Color(0x1F00E5FF),
                            textColor = if (isRoot) NeonMint else SoftCyan
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Root Mode Switcher (helpful developer feature)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = getTranslation("root_access", currentLang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (isVerifyingRoot) {
                                    CircularProgressIndicator(
                                        color = NeonMint,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            Text(
                                text = if (isRoot) getTranslation("root_desc_on", currentLang) else getTranslation("root_desc_off", currentLang),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = isRoot,
                            enabled = !isVerifyingRoot && !isBusy,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    isVerifyingRoot = true
                                    manager.verifyRootAccess { success ->
                                        isVerifyingRoot = false
                                        if (success) {
                                            manager.setRootMode(true)
                                        } else {
                                            manager.setRootMode(false)
                                            showRootErrorDialog = true
                                        }
                                    }
                                } else {
                                    manager.setRootMode(false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = NeonMint,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // ==========================================
            // ACTIVE ACCOUNT COUNTDOWN OVERLAY CONTAINER
            // ==========================================
            AnimatedVisibility(
                visible = activeTimer != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                activeTimer?.let { timeRemaining ->
                    BentoSectionCard(
                        glowColor = ErrorRed,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (isDarkMode) Color(0xFF1F1115) else Color(0xFFFFECEF) // Themed warm red shade
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Alert", tint = ErrorRed)
                                Text(
                                    text = getTranslation("timer_banner", currentLang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val minutes = (timeRemaining / 1000) / 60
                            val seconds = (timeRemaining / 1000) % 60
                            val formattedTime = String.format("%02d:%02d", minutes, seconds)
                            
                            // Glowing Countdown text
                            Text(
                                text = formattedTime,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = ErrorRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Text(
                                text = getTranslation("timer_desc", currentLang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))

                            // Live detector state
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMlForeground) (if (isDarkMode) Color(0x3300FF88) else Color(0x2200CC66)) else (if (isDarkMode) Color(0x33FFB300) else Color(0x22FFA000)))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isMlForeground) (if (isDarkMode) NeonMint else Color(0xFF00AA4F)) else Color(0xFFFFA000))
                                )
                                
                                val trackingMinutes = (mlForegroundDuration / 1000) / 60
                                val trackingSeconds = (mlForegroundDuration / 1000) % 60
                                val trackedTimeStr = String.format("%02d:%02d", trackingMinutes, trackingSeconds)
                                
                                Text(
                                    text = if (isMlForeground) {
                                        if (currentLang == "ID") "Mendekteksi MLBB Aktif di Foreground ($trackedTimeStr)" else "MLBB Detected in Foreground ($trackedTimeStr)"
                                    } else {
                                        if (currentLang == "ID") "Menunggu Mobile Legends dijalankan..." else "Waiting for Mobile Legends to launch..."
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMlForeground) (if (isDarkMode) NeonMint else Color(0xFF00833C)) else Color(0xFFE65100)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { manager.cancelCreatingAccountAndEnableGms() },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Restore", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(getTranslation("timer_btn", currentLang), color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PRIMARY ACTION BENTO TILES (Backup & Create)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TILE 1: Backup Current Account (Left Bento)
                BentoSectionCard(
                    glowColor = NeonMint,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isBusy) { 
                            if (!isRoot) {
                                showRootErrorDialog = true
                            } else {
                                currentBackupNameInput = ""
                                showBackupDialog = true 
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkMode) Color(0x1F00FFCC) else LightTranslucentTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Backup", tint = if (isDarkMode) NeonMint else NeonTealLight, modifier = Modifier.size(20.dp))
                            }
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = getTranslation("btn_backup", currentLang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getTranslation("desc_backup", currentLang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                // TILE 2: Create New Account (Right Bento)
                BentoSectionCard(
                    glowColor = SoftCyan,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !isBusy && activeTimer == null) { 
                            if (!isRoot) {
                                showRootErrorDialog = true
                            } else {
                                showCreateConfirmDialog = true 
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDarkMode) Color(0x1F00E5FF) else Color(0x1A00899B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Create", tint = if (isDarkMode) SoftCyan else SoftCyanLight, modifier = Modifier.size(20.dp))
                            }
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = getTranslation("btn_create", currentLang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getTranslation("desc_create", currentLang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ==========================================
            // BACKUP DIRECTORY ROW (Switch Accounts Hub)
            // ==========================================
            BentoSectionCard(
                glowColor = NeonMint,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccountBox, contentDescription = "Backups", tint = if (isDarkMode) NeonMint else NeonTealLight)
                            Text(
                                text = getTranslation("backup_header", currentLang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        IconButton(
                            onClick = { manager.refreshBackups() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = if (isDarkMode) SoftCyan else SoftCyanLight, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Backup accounts items list
                    if (listBackups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(BorderStroke(1.dp, if (isDarkMode) Color(0x1A00FFCC) else LightBorder), RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) Color(0x0500FFCC) else LightTranslucentTeal.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = "Void", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = getTranslation("backup_empty_title", currentLang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = getTranslation("backup_empty_desc", currentLang),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listBackups.forEach { backup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backup.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${getTranslation("sub_backup_date", currentLang)} ${backup.date}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = backup.path,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            maxLines = 1
                                        )
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Switch Account Button
                                        IconButton(
                                            onClick = { 
                                                 if (!isRoot) {
                                                     showRootErrorDialog = true
                                                 } else {
                                                     showSwitchConfirmDialog = backup 
                                                 }
                                             },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isDarkMode) Color(0x1A00FFCC) else LightTranslucentTeal)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Switch", tint = if (isDarkMode) NeonMint else NeonTealLight, modifier = Modifier.size(20.dp))
                                        }
                                        
                                        // Swipe/Delete Button
                                        IconButton(
                                            onClick = { 
                                                if (!isRoot) {
                                                    showRootErrorDialog = true
                                                } else {
                                                    manager.deleteBackup(backup) { success ->
                                                        val msg = if (success) getTranslation("toast_delete_success", currentLang) else getTranslation("toast_delete_failed", currentLang)
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1AFF4D4D))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ADVANCED FEATURE COMING SOON BENTO TILE
            // ==========================================
            BentoSectionCard(glowColor = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            Toast.makeText(context, getTranslation("toast_coming_soon", currentLang), Toast.LENGTH_LONG).show()
                        }
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Build, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            Text(
                                text = getTranslation("btn_optimizer", currentLang),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                             )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getTranslation("desc_optimizer", currentLang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    CustomBadge(
                        text = "Soon!",
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            // ==========================================
            // TERMINAL SYSTEM COMMAND LOGS BENTO BOX
            // ==========================================
            BentoSectionCard(
                glowColor = if (isDarkMode) SoftCyan else SoftCyanLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Build, contentDescription = "Shell logs", tint = if (isDarkMode) SoftCyan else SoftCyanLight, modifier = Modifier.size(18.dp))
                            Text(
                                text = getTranslation("terminal_title", currentLang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        TextButton(
                            onClick = { manager.clearLogs() },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (isDarkMode) SoftCyan else SoftCyanLight),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Code Console/Terminal block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkMode) Color(0xFF030406) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                             reverseLayout = true,
                             modifier = Modifier.fillMaxSize(),
                             verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(listLogs) { log ->
                                val logColor = when (log.type) {
                                    LogType.ROOT -> Color(0xFFE5C07B) // Sandy gold
                                    LogType.SUCCESS -> if (isDarkMode) NeonMint else NeonTealLight
                                    LogType.ERROR -> ErrorRed
                                    LogType.INFO -> if (isDarkMode) SoftCyan else SoftCyanLight
                                }
                                val symbol = when (log.type) {
                                    LogType.ROOT -> "# "
                                    LogType.SUCCESS -> "✓ "
                                    LogType.ERROR -> "❌ "
                                    LogType.INFO -> "i "
                                }
                                Text(
                                    text = "[${log.timestamp}] $symbol${log.message}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = logColor,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SYSTEM SETTINGS & COLLAPSIBLE ABOUT BENTO BOX
            // ==========================================
            var isAboutExpanded by remember { mutableStateOf(false) }

            BentoSectionCard(
                glowColor = if (isDarkMode) SoftCyan else SoftCyanLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (isDarkMode) SoftCyan else SoftCyanLight)
                        Text(
                            text = getTranslation("settings_title", currentLang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Language Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = getTranslation("settings_lang", currentLang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ID", "EN").forEach { lang ->
                                val isSelected = currentLang == lang
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDarkMode) Color(0x3300FFCC) else LightTranslucentTeal
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) {
                                                if (isDarkMode) NeonMint else NeonTealLight
                                            } else {
                                                Color.Transparent
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onLangChanged(lang) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (lang == "ID") "Bahasa" else "English",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            if (isDarkMode) NeonMint else NeonTealLight
                                        } else {
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Theme Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = getTranslation("settings_theme", currentLang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(true, false).forEach { isDark ->
                                val isSelected = isDarkMode == isDark
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDarkMode) Color(0x3300E5FF) else Color(0x1A00899B)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) {
                                                if (isDarkMode) SoftCyan else SoftCyanLight
                                            } else {
                                                Color.Transparent
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onThemeChanged(isDark) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isDark) getTranslation("settings_theme_dark", currentLang) else getTranslation("settings_theme_light", currentLang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            if (isDarkMode) SoftCyan else SoftCyanLight
                                        } else {
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Collapsible About Section
                     Row(
                         modifier = Modifier
                             .fillMaxWidth()
                             .clip(RoundedCornerShape(8.dp))
                             .clickable { isAboutExpanded = !isAboutExpanded }
                             .padding(vertical = 4.dp),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.SpaceBetween
                     ) {
                         Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                             Icon(Icons.Default.Info, contentDescription = "About", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                             Text(
                                 text = getTranslation("credits_title", currentLang),
                                 fontSize = 13.sp,
                                 fontWeight = FontWeight.SemiBold,
                                 color = MaterialTheme.colorScheme.onBackground
                             )
                         }
                         Icon(
                             imageVector = if (isAboutExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                             contentDescription = "Toggle About",
                             tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                         )
                     }

                     AnimatedVisibility(
                         visible = isAboutExpanded,
                         enter = expandVertically() + fadeIn(),
                         exit = shrinkVertically() + fadeOut()
                     ) {
                         Column {
                             Spacer(modifier = Modifier.height(12.dp))
                             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                 CreditDetailRow(
                                      label = getTranslation("credit_dev", currentLang),
                                      value = "Ihsan Sungkar",
                                      badgeText = "Creator",
                                      isDarkMode = isDarkMode,
                                      githubUrl = "https://github.com/iCansSungkar/",
                                      instagramUrl = "https://www.instagram.com/ihsan.sungkar/"
                                  )
                                 CreditDetailRow(
                                      label = getTranslation("credit_assistant", currentLang),
                                      value = "AI Assistant (DeepMind Antigravity)",
                                      badgeText = "AI Specialist",
                                      isDarkMode = isDarkMode,
                                      websiteUrl = "https://aistudio.google.com/"
                                  )
                                 CreditDetailRow(
                                      label = getTranslation("credit_tester", currentLang),
                                      value = "Ramadhan Sungkar",
                                      badgeText = "Tester & QA",
                                      isDarkMode = isDarkMode,
                                      githubUrl = "https://github.com/adanSncrs",
                                      instagramUrl = "https://www.instagram.com/adansncr"
                                  )
                             }
                             
                             Spacer(modifier = Modifier.height(12.dp))
                             
                             Text(
                                 text = getTranslation("credit_note", currentLang),
                                 fontSize = 10.sp,
                                 color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                 textAlign = TextAlign.Center,
                                 modifier = Modifier.fillMaxWidth()
                             )
                         }
                     }
                 }
             }

             Spacer(modifier = Modifier.height(24.dp))
        }

        // ==========================================
        // OPERATIONAL PROGRESS DIALOG
        // ==========================================
        if (isBusy) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                dismissButton = {},
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = if (isDarkMode) NeonMint else NeonTealLight, modifier = Modifier.size(24.dp))
                        Text(getTranslation("dialog_root_running", currentLang), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    }
                },
                text = {
                    Text(
                        getTranslation("dialog_root_desc", currentLang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            )
        }

        // ==========================================
        // BACKUP COMPOSABLE MODAL DIALOG
        // ==========================================
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = if (isDarkMode) NeonMint else NeonTealLight)
                        Text(getTranslation("dialog_backup_title", currentLang), color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            getTranslation("dialog_backup_desc", currentLang),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        
                        TextField(
                            value = currentBackupNameInput,
                            onValueChange = { currentBackupNameInput = it },
                            placeholder = { Text(getTranslation("dialog_backup_placeholder", currentLang), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedIndicatorColor = if (isDarkMode) NeonMint else NeonTealLight,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = currentBackupNameInput
                            showBackupDialog = false
                            manager.backupAccount(name) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) NeonMint else NeonTealLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(getTranslation("dialog_backup_confirm", currentLang), color = if (isDarkMode) ObsidianPure else Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text(getTranslation("btn_batal", currentLang), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // ==========================================
        // SWITCH ACCOUNT OVERLAY/CONFIRMATION
        // ==========================================
        showSwitchConfirmDialog?.let { backup ->
            AlertDialog(
                onDismissRequest = { showSwitchConfirmDialog = null },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Switch", tint = if (isDarkMode) NeonMint else NeonTealLight)
                        Text(getTranslation("dialog_switch_title", currentLang), color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            getTranslation("dialog_switch_desc", currentLang),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = backup.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) NeonMint else NeonTealLight
                        )
                        Text(
                            text = getTranslation("dialog_switch_warning", currentLang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSwitchConfirmDialog = null
                            manager.switchAccount(backup) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) NeonMint else NeonTealLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(getTranslation("btn_switch", currentLang), color = if (isDarkMode) ObsidianPure else Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSwitchConfirmDialog = null }) {
                        Text(getTranslation("btn_batal", currentLang), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // ==========================================
        // CREATE ACCOUNT WARNING DIALOG
        // ==========================================
        if (showCreateConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showCreateConfirmDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = "Warn", tint = if (isDarkMode) SoftCyan else SoftCyanLight)
                        Text(getTranslation("dialog_create_title", currentLang), color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = getTranslation("dialog_create_desc", currentLang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) SoftCyan else SoftCyanLight
                        )
                        Text(
                            text = getTranslation("dialog_create_warning", currentLang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = getTranslation("dialog_create_steps", currentLang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCreateConfirmDialog = false
                            manager.createNewAccount { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) SoftCyan else SoftCyanLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(getTranslation("dialog_create_confirm", currentLang), color = if (isDarkMode) ObsidianPure else Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateConfirmDialog = false }) {
                        Text(getTranslation("btn_batal", currentLang), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // ==========================================
        // ROOT ERROR WARNING DIALOG
        // ==========================================
        if (showRootErrorDialog) {
            AlertDialog(
                onDismissRequest = { showRootErrorDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = ErrorRed)
                        Text("Root Check Failed", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = getTranslation("root_error_toast", currentLang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showRootErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun slate_desc_placeholder(): Color {
    return MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
}

// ==========================================
// REUSABLE UI COMPONENTS WITH NEO-BENTO AESTHETIC
// ==========================================
@Composable
fun BentoSectionCard(
    glowColor: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                BorderStroke(
                    1.dp, 
                    Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        content()
    }
}

@Composable
fun CustomBadge(text: String, backgroundColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
fun CreditDetailRow(
    label: String,
    value: String,
    badgeText: String,
    isDarkMode: Boolean = true,
    githubUrl: String? = null,
    instagramUrl: String? = null,
    websiteUrl: String? = null
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            instagramUrl?.let { url ->
                IconButton(
                    onClick = { uriHandler.openUri(url) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_instagram),
                        contentDescription = "Instagram link",
                        tint = if (isDarkMode) SoftCyan else SoftCyanLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            githubUrl?.let { url ->
                IconButton(
                    onClick = { uriHandler.openUri(url) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_github),
                        contentDescription = "GitHub link",
                        tint = if (isDarkMode) NeonMint else NeonTealLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            websiteUrl?.let { url ->
                IconButton(
                    onClick = { uriHandler.openUri(url) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Website link",
                        tint = if (isDarkMode) SoftCyan else SoftCyanLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            CustomBadge(
                text = badgeText,
                backgroundColor = when (badgeText) {
                    "Creator" -> if (isDarkMode) NeonMintTranslucent else LightTranslucentTeal
                    "AI Specialist" -> if (isDarkMode) Color(0x1F00E5FF) else Color(0x1A00899B)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                },
                textColor = when (badgeText) {
                    "Creator" -> if (isDarkMode) NeonMint else NeonTealLight
                    "AI Specialist" -> if (isDarkMode) SoftCyan else SoftCyanLight
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                }
            )
        }
    }
}

fun getTranslation(key: String, lang: String): String {
    val idMap = mapOf(
        "title" to "SLYTASK",
        "subtitle" to "Mobile Legends Tools",
        "root_access" to "Gunakan Akses Root",
        "root_desc_on" to "Menjalankan real command (su)",
        "root_desc_off" to "Menjalankan internal mock sandbox",
        "btn_backup" to "Backup Akun",
        "desc_backup" to "Backup sesi login /data/data offline.",
        "btn_create" to "Buat Akun Baru",
        "desc_create" to "Disable Play Services untuk reset akun guest atau membuat akun baru.",
        "backup_header" to "List Akun",
        "backup_empty_title" to "Belum Ada Backup Offline",
        "backup_empty_desc" to "Ketuk tombol 'Backup Akun' di atas untuk menyimpan akun saat ini.",
        "btn_switch" to "Ya, Muat Akun",
        "sub_backup_date" to "Backup:",
        "btn_delete" to "Hapus",
        "btn_batal" to "Batal",
        "toast_delete_success" to "Backup dihapus",
        "toast_delete_failed" to "Gagal menghapus",
        "toast_coming_soon" to "Coming Soon! Fitur optimasi sistem premium sedang dirancang.",
        "btn_optimizer" to "Advance Settings & Optimizer",
        "desc_optimizer" to "Fitur bypass skrip bypass patch, optimasi latency, dll.",
        "terminal_title" to "System Term Shell Logs",
        "credits_title" to "Tentang Aplikasi & Kredit",
        "credit_dev" to "Developer",
        "credit_assistant" to "Asisten",
        "credit_tester" to "Penguji (Tester)",
        "credit_note" to "SlyTask v1.1 • Mobile Legends Utility. Gunakan bijak sesuai instruksi.",
        "dialog_root_running" to "Running root commands...",
        "dialog_root_desc" to "Menjalankan script biner root superuser. Harap tunggu sebentar, jangan matikan aplikasi.",
        "dialog_backup_title" to "Backup Akun MLBB",
        "dialog_backup_desc" to "Masukkan nama akun atau Nickname akun sedang login saat ini di perangkat Anda.",
        "dialog_backup_placeholder" to "Contoh: Akun Utama, Akun_Tumbal123",
        "dialog_backup_confirm" to "Mulai Backup",
        "dialog_switch_title" to "Ganti Akun (Konfirmasi)",
        "dialog_switch_desc" to "Anda akan mengganti akun Anda ke:",
        "dialog_switch_warning" to "Proses ini akan mengeluakan akun yang sedang Login saat ini. Pastikan bahwa anda telah mengaitkan dan atau backup.",
        "dialog_create_title" to "Buat Akun Baru (Instant Guest)",
        "dialog_create_desc" to "PERINGATAN PROSEDUR DISABLING GMS!",
        "dialog_create_warning" to "Aplikasi ini akan menonaktifkan Layanan Google Play (com.google.android.gms) selama anda membuka aplikasi Mobile Legends.",
        "dialog_create_steps" to "Yang Mungkin Anda Ingin Ketahui:\n1. Ini akan mengeluarkan akun yang sedang Login.\n2. Google Play Services akan di nonaktifkan (disable).\n3. Ressource atau data anda tidak akan download ulang.\n4. Ketika akun berhasil di buat, Layanan Google Play akan di aktifkan kembali (enable).\n\nLayanan Google Play akan di aktifkan selama beberapa menit. sekarang anda punya waktu untuk mengonfigurasi akun baru Anda, setelah itu GMS diaktifkan otomatis (atau dimatikan lewat floating dialog).",
        "dialog_create_confirm" to "Saya Setuju & Mulai",
        "timer_banner" to "PLAY SERVICES (GMS) DINONAKTIFKAN",
        "timer_desc" to "Silakan buat/konfigurasi Akun baru dalam game Mobile Legends sekarang. GMS akan otomatis aktif saat timer habis.",
        "timer_btn" to "Aktifkan kembali GMS Sekarang (Selesai)",
        "settings_title" to "Pengaturan",
        "settings_lang" to "Bahasa",
        "settings_theme" to "Tema Aplikasi",
        "settings_theme_dark" to "Gelap",
        "settings_theme_light" to "Terang",
        "root_error_toast" to "Perangkat Anda gagal diverifikasi akses root-nya. Mohon berikan izin Superuser (su binary) atau laksanakan instalasi Magisk/APatch.",
        "root_success_toast" to "Akses Root berhasil terpasang dan diverifikasi!",
        "root_not_available" to "Biner root tidak ditemukan. Silakan beralih kembali ke mode Simulasi.",
        "verification_running" to "Sedang memeriksa akses root..."
    )

    val enMap = mapOf(
        "title" to "SLYTASK",
        "subtitle" to "Mobile Legends Utility",
        "root_access" to "Enable Root Access",
        "root_desc_on" to "Executing real root commands (su)",
        "root_desc_off" to "Executing internal simulation sandbox",
        "btn_backup" to "Backup Account",
        "desc_backup" to "Backup current login session offline.",
        "btn_create" to "Create New Account",
        "desc_create" to "Disable Play Services to reset guest accounts or create a new account.",
        "backup_header" to "Account List",
        "backup_empty_title" to "No Offline Backups Yet",
        "backup_empty_desc" to "Tap 'Backup Account' at top to save current account.",
        "btn_switch" to "Yes, Load Account",
        "sub_backup_date" to "Backup:",
        "btn_delete" to "Delete",
        "btn_batal" to "Cancel",
        "toast_delete_success" to "Backup deleted successfully",
        "toast_delete_failed" to "Delete failed",
        "toast_coming_soon" to "Coming Soon! Premium performance optimizations are being designed.",
        "btn_optimizer" to "Advanced Settings & Optimizer",
        "desc_optimizer" to "Bypass patch scripts, latency optimization, etc.",
        "terminal_title" to "System Term Shell Logs",
        "credits_title" to "About Application & Credits",
        "credit_dev" to "Developer",
        "credit_assistant" to "Assistant",
        "credit_tester" to "Tester",
        "credit_note" to "SlyTask v1.3 • Mobile Legends Utility. Use wisely in accordance with instructions.",
        "dialog_root_running" to "Running root commands...",
        "dialog_root_desc" to "Executing superuser root scripts. Please wait, do not close the app.",
        "dialog_backup_title" to "Backup MLBB Account",
        "dialog_backup_desc" to "Enter the account name or Nickname for the account currently logged in on your device.",
        "dialog_backup_placeholder" to "Example: Main Account, Sub_Account123",
        "dialog_backup_confirm" to "Start Backup",
        "dialog_switch_title" to "Switch Account (Confirm)",
        "dialog_switch_desc" to "You are about to switch accounts to:",
        "dialog_switch_warning" to "This process will log out the account currently logged in. Make sure you have linked and/or backed it up.",
        "dialog_create_title" to "Create New Account (Guest Smurf)",
        "dialog_create_desc" to "GMS DISABLING PROCEDURE WARNING!",
        "dialog_create_warning" to "This app will disable Google Play Services (com.google.android.gms) while you are opening the Mobile Legends game.",
        "dialog_create_steps" to "What You Might Want to Know:\n1. This will log out the account currently logged in.\n2. Google Play Services will be disabled.\n3. Your resources or data will not be redownloaded.\n4. Once the account is created successfully, Google Play Services will be re-enabled.\n\nGoogle Play Services will be active for a few minutes. You now have time to configure your new account, after which GMS is activated automatically (or disabled via floating dialog/banner).",
        "dialog_create_confirm" to "I Agree & Proceed",
        "timer_banner" to "PLAY SERVICES (GMS) IS DISABLED",
        "timer_desc" to "Please create your new Account in game now. GMS will auto-reactivate when the countdown completes.",
        "timer_btn" to "Enable GMS Now (Finish early)",
        "settings_title" to "Settings",
        "settings_lang" to "Language",
        "settings_theme" to "App Theme",
        "settings_theme_dark" to "Dark",
        "settings_theme_light" to "Light",
        "root_error_toast" to "Your device failed verification. Please grant Superuser (su permission) or run Magisk/APatch.",
        "root_success_toast" to "Root Access successfully verified!",
        "root_not_available" to "Root binary not found. Falling back to Simulation mode.",
        "verification_running" to "Verifying root permission..."
    )

    return if (lang == "ID") idMap[key] ?: key else enMap[key] ?: key
}
