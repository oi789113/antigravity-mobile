package com.antigravity.mobile

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class WorkspacePreset(val title: String, val subtitle: String, val defaultUrl: String) {
    ANTIGRAVITY(
        title = "Antigravity 控制台",
        subtitle = "antigravity.google.com",
        defaultUrl = "https://antigravity.google.com"
    ),
    AI_STUDIO(
        title = "Google AI Studio",
        subtitle = "aistudio.google.com",
        defaultUrl = "https://aistudio.google.com"
    ),
    CUSTOM(
        title = "自定义工作台",
        subtitle = "自定义 URL 地址",
        defaultUrl = "https://antigravity.google.com"
    )
}

data class QuickPrompt(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String
)

object AppPrefs {
    private const val PREFS_NAME = "antigravity_mobile_prefs"
    private const val KEY_PRESET = "selected_preset"
    private const val KEY_CUSTOM_URL = "custom_url"
    private const val KEY_USE_WEBVIEW = "use_builtin_webview"
    private const val KEY_AUTO_LAUNCH = "auto_launch_on_start"
    private const val KEY_PROMPTS_JSON = "quick_prompts_json"

    val defaultPrompts = listOf(
        QuickPrompt(
            id = "preset-progress-diff",
            title = "查进度与 Diff",
            content = "请汇报当前任务进度，列出已修改的文件和关键 git diff 摘要，并说明下一步计划。"
        ),
        QuickPrompt(
            id = "preset-test-fix",
            title = "跑测试并修复",
            content = "请运行当前项目的测试套件与 Lint 检查，定位所有失败用例或报错并直接修复，直到测试全部通过。"
        ),
        QuickPrompt(
            id = "preset-pause-confirm",
            title = "暂停等待确认",
            content = "请先暂停后续代码修改，总结目前的改动方案与潜在风险点，等待我确认后再继续执行。"
        ),
        QuickPrompt(
            id = "preset-commit-msg",
            title = "生成 Commit 信息",
            content = "请检查当前所有的变更（git status / git diff），生成符合 Conventional Commits 规范的中英文 Commit Message。"
        )
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedPreset(context: Context): WorkspacePreset {
        val raw = prefs(context).getString(KEY_PRESET, WorkspacePreset.ANTIGRAVITY.name)
        return WorkspacePreset.entries.firstOrNull { it.name == raw } ?: WorkspacePreset.ANTIGRAVITY
    }

    fun setSelectedPreset(context: Context, preset: WorkspacePreset) {
        prefs(context).edit().putString(KEY_PRESET, preset.name).apply()
    }

    fun getCustomUrl(context: Context): String =
        prefs(context).getString(KEY_CUSTOM_URL, "https://antigravity.google.com")
            ?: "https://antigravity.google.com"

    fun setCustomUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_CUSTOM_URL, url.trim()).apply()
    }

    fun getUseWebView(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_WEBVIEW, false)

    fun setUseWebView(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_WEBVIEW, enabled).apply()
    }

    fun getAutoLaunch(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_LAUNCH, false)

    fun setAutoLaunch(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_LAUNCH, enabled).apply()
    }

    fun getPrompts(context: Context): List<QuickPrompt> {
        val raw = prefs(context).getString(KEY_PROMPTS_JSON, null) ?: return defaultPrompts
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<QuickPrompt>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.optString("title").trim()
                val content = obj.optString("content").trim()
                val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
                if (title.isNotEmpty() && content.isNotEmpty()) {
                    list.add(QuickPrompt(id = id, title = title, content = content))
                }
            }
            if (list.isEmpty()) defaultPrompts else list
        } catch (_: Exception) {
            defaultPrompts
        }
    }

    fun savePrompts(context: Context, prompts: List<QuickPrompt>) {
        val array = JSONArray()
        prompts.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("content", item.content)
            }
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_PROMPTS_JSON, array.toString()).apply()
    }
}

private val AntigravityDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA855F7),
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = AntigravityDarkColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AntigravityMobileApp()
                }
            }
        }
    }
}

fun normalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return "https://antigravity.google.com"
    return if (trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
    ) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

fun launchCustomTab(context: Context, rawUrl: String) {
    val targetUrl = normalizeUrl(rawUrl)
    val uri = Uri.parse(targetUrl)
    val darkColorInt = 0xFF0B0F19.toInt()

    val darkParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(darkColorInt)
        .setNavigationBarColor(darkColorInt)
        .setSecondaryToolbarColor(darkColorInt)
        .build()

    val customTabsIntent = CustomTabsIntent.Builder()
        .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
        .setDefaultColorSchemeParams(darkParams)
        .setUrlBarHidingEnabled(true)
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .build()

    // Prefer Google Chrome when installed so the user's primary Google session & notifications are reused
    val chromePackages = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary"
    )
    val pm = context.packageManager
    val availableChrome = chromePackages.firstOrNull { pkg ->
        try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
    if (availableChrome != null) {
        customTabsIntent.intent.setPackage(availableChrome)
    }

    try {
        customTabsIntent.launchUrl(context, uri)
    } catch (_: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开链接: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun AntigravityMobileApp() {
    val context = LocalContext.current

    var selectedPreset by remember { mutableStateOf(AppPrefs.getSelectedPreset(context)) }
    var customUrl by remember { mutableStateOf(AppPrefs.getCustomUrl(context)) }
    var useWebView by remember { mutableStateOf(AppPrefs.getUseWebView(context)) }
    var autoLaunch by remember { mutableStateOf(AppPrefs.getAutoLaunch(context)) }
    var prompts by remember { mutableStateOf(AppPrefs.getPrompts(context)) }

    var activeWebViewUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var didAutoLaunchThisSession by rememberSaveable { mutableStateOf(false) }

    val resolvedTargetUrl = remember(selectedPreset, customUrl) {
        when (selectedPreset) {
            WorkspacePreset.ANTIGRAVITY -> WorkspacePreset.ANTIGRAVITY.defaultUrl
            WorkspacePreset.AI_STUDIO -> WorkspacePreset.AI_STUDIO.defaultUrl
            WorkspacePreset.CUSTOM -> normalizeUrl(customUrl)
        }
    }

    val openWorkspace: (String) -> Unit = { urlToOpen ->
        val normalized = normalizeUrl(urlToOpen)
        if (useWebView) {
            activeWebViewUrl = normalized
        } else {
            launchCustomTab(context, normalized)
        }
    }

    // Auto-launch on app startup if enabled
    LaunchedEffect(Unit) {
        if (autoLaunch && !didAutoLaunchThisSession) {
            didAutoLaunchThisSession = true
            openWorkspace(resolvedTargetUrl)
        }
    }

    if (activeWebViewUrl != null) {
        BuiltInWebViewScreen(
            initialUrl = activeWebViewUrl!!,
            onClose = { activeWebViewUrl = null },
            onOpenInCustomTabs = { currentUrl ->
                launchCustomTab(context, currentUrl)
            }
        )
    } else {
        CompanionHomeScreen(
            selectedPreset = selectedPreset,
            onSelectPreset = { preset ->
                selectedPreset = preset
                AppPrefs.setSelectedPreset(context, preset)
            },
            customUrl = customUrl,
            onCustomUrlChange = { newUrl ->
                customUrl = newUrl
                AppPrefs.setCustomUrl(context, newUrl)
            },
            resolvedTargetUrl = resolvedTargetUrl,
            useWebView = useWebView,
            onUseWebViewChange = { enabled ->
                useWebView = enabled
                AppPrefs.setUseWebView(context, enabled)
            },
            autoLaunch = autoLaunch,
            onAutoLaunchChange = { enabled ->
                autoLaunch = enabled
                AppPrefs.setAutoLaunch(context, enabled)
            },
            prompts = prompts,
            onAddPrompt = { title, content ->
                val updated = prompts + QuickPrompt(title = title.trim(), content = content.trim())
                prompts = updated
                AppPrefs.savePrompts(context, updated)
            },
            onDeletePrompt = { promptId ->
                val updated = prompts.filterNot { it.id == promptId }
                prompts = updated
                AppPrefs.savePrompts(context, updated)
            },
            onResetDefaultPrompts = {
                prompts = AppPrefs.defaultPrompts
                AppPrefs.savePrompts(context, AppPrefs.defaultPrompts)
                Toast.makeText(context, "已恢复默认快捷指令", Toast.LENGTH_SHORT).show()
            },
            onPromptClick = { prompt ->
                copyToClipboard(context, prompt.title, prompt.content)
                Toast.makeText(
                    context,
                    "已复制「${prompt.title}」，正在打开工作台…",
                    Toast.LENGTH_SHORT
                ).show()
                openWorkspace(resolvedTargetUrl)
            },
            onLaunchWorkspace = {
                openWorkspace(resolvedTargetUrl)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompanionHomeScreen(
    selectedPreset: WorkspacePreset,
    onSelectPreset: (WorkspacePreset) -> Unit,
    customUrl: String,
    onCustomUrlChange: (String) -> Unit,
    resolvedTargetUrl: String,
    useWebView: Boolean,
    onUseWebViewChange: (Boolean) -> Unit,
    autoLaunch: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
    prompts: List<QuickPrompt>,
    onAddPrompt: (String, String) -> Unit,
    onDeletePrompt: (String) -> Unit,
    onResetDefaultPrompts: () -> Unit,
    onPromptClick: (QuickPrompt) -> Unit,
    onLaunchWorkspace: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showAddPromptDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Antigravity Mobile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "云端 Agent 移动控制台伴侣",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Launch Card + Workspace Switcher
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "目标工作台",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = if (useWebView) "内置 WebView 模式" else "Chrome Custom Tabs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WorkspacePreset.entries.forEach { preset ->
                                val selected = preset == selectedPreset
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectPreset(preset) },
                                    label = {
                                        Text(
                                            text = preset.title,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        AnimatedVisibility(visible = selectedPreset == WorkspacePreset.CUSTOM) {
                            OutlinedTextField(
                                value = customUrl,
                                onValueChange = onCustomUrlChange,
                                label = { Text("自定义工作台 URL（自动保存）") },
                                placeholder = { Text("https://antigravity.google.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = "当前地址：$resolvedTargetUrl",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Button(
                            onClick = onLaunchWorkspace,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "立即进入 ${selectedPreset.title}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Quick Prompts Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "常用开发指令快捷板",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "点击任意指令自动复制并立即打开目标工作台",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onResetDefaultPrompts) {
                            Text("重置", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { showAddPromptDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "新增指令",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("新增", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 3. Quick Prompt Cards
            items(prompts, key = { it.id }) { prompt ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onPromptClick(prompt) }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = prompt.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, prompt.title, prompt.content)
                                        Toast.makeText(
                                            context,
                                            "已复制「${prompt.title}」到剪贴板",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "仅复制",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeletePrompt(prompt.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "删除指令",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = prompt.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. Launch & Browser Mode Settings
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "连接与启动设置",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Switch 1: Built-in WebView vs Chrome Custom Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "使用内置 WebView（备用模式）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "默认关闭（推荐使用 Chrome Custom Tabs 复用系统 Google 登录态与通知）；开启后在内置 WebView 打开，遇到 accounts.google.com 登录自动跳回 Custom Tabs。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useWebView,
                                onCheckedChange = onUseWebViewChange
                            )
                        }

                        // Switch 2: Auto-launch on app startup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "打开 App 自动直达工作台",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "开启后点击手机桌面图标将直接进入当前选中的工作台。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoLaunch,
                                onCheckedChange = onAutoLaunchChange
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPromptDialog) {
        AddQuickPromptDialog(
            onDismiss = { showAddPromptDialog = false },
            onConfirm = { title, content ->
                onAddPrompt(title, content)
                showAddPromptDialog = false
            }
        )
    }
}

@Composable
fun AddQuickPromptDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "新增开发指令",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("指令名称") },
                    placeholder = { Text("例如：审查 PR 安全隐患") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("指令内容（点击卡片时自动复制）") },
                    placeholder = { Text("请输入要发送给 Antigravity Agent 的完整提示词…") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BuiltInWebViewScreen(
    initialUrl: String,
    onClose: () -> Unit,
    onOpenInCustomTabs: (String) -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(initialUrl) }
    var pageTitle by rememberSaveable { mutableStateOf("Antigravity 工作台") }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileChooserCallback
        fileChooserCallback = null
        if (callback != null) {
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            callback.onReceiveValue(uris)
        }
    }

    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Compact Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val wv = webViewRef
                    if (wv != null && wv.canGoBack()) {
                        wv.goBack()
                    } else {
                        onClose()
                    }
                }
            ) {
                Icon(
                    imageVector = if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                    contentDescription = "返回"
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = pageTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新"
                )
            }

            IconButton(onClick = { onOpenInCustomTabs(currentUrl) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "在 Chrome Custom Tabs 中打开"
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "返回伴侣首页"
                )
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        userAgentString = userAgentString.replace("; wv", "")
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val reqUri = request?.url ?: return false
                            val host = reqUri.host?.lowercase() ?: ""
                            if (host == "accounts.google.com" || host.endsWith(".accounts.google.com")) {
                                Toast.makeText(
                                    context,
                                    "检测到 Google 账号登录，已自动切换至 Chrome Custom Tabs",
                                    Toast.LENGTH_LONG
                                ).show()
                                onOpenInCustomTabs(reqUri.toString())
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            if (url != null) {
                                val host = Uri.parse(url).host?.lowercase() ?: ""
                                if (host == "accounts.google.com" || host.endsWith(".accounts.google.com")) {
                                    view?.stopLoading()
                                    Toast.makeText(
                                        context,
                                        "检测到 Google 账号登录，已自动切换至 Chrome Custom Tabs",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onOpenInCustomTabs(url)
                                    return
                                }
                                currentUrl = url
                            }
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (url != null) currentUrl = url
                            canGoBack = view?.canGoBack() == true
                            CookieManager.getInstance().flush()
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank()) {
                                pageTitle = title
                            }
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            fileChooserCallback?.onReceiveValue(null)
                            fileChooserCallback = filePathCallback
                            return try {
                                val intent = fileChooserParams?.createIntent()
                                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                                filePickerLauncher.launch(intent)
                                true
                            } catch (_: Exception) {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = null
                                false
                            }
                        }
                    }

                    webViewRef = this
                    loadUrl(initialUrl)
                }
            },
            update = { view ->
                webViewRef = view
            }
        )
    }
}
