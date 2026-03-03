package com.example.weight

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val Context.dataStore by preferencesDataStore(name = "weight_store")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    WeightApp(this)
                }
            }
        }
    }
}

@Serializable
data class WeightEntry(
    val id: Long,
    val value: String,
    val timestamp: Long,
)

@Serializable
data class ExportPayload(
    val columns: Int,
    val styleId: Int,
    val entries: List<WeightEntry>,
)

private data class CellStyle(
    val id: Int,
    val name: String,
    val background: Brush,
    val textColor: Color,
    val borderColor: Color,
    val corner: Int,
)

private val entriesKey = stringPreferencesKey("entries_json")
private val columnsKey = intPreferencesKey("columns")
private val styleKey = intPreferencesKey("style_id")

private val json = Json { ignoreUnknownKeys = true }

private val allStyles = listOf(
    CellStyle(0, "苹果风格", Brush.linearGradient(listOf(Color(0xFFF4F5F7), Color(0xFFE7EAF0))), Color(0xFF1A1A1A), Color(0xFFCED3DB), 18),
    CellStyle(1, "莫奈风格", Brush.linearGradient(listOf(Color(0xFFDFF1FF), Color(0xFFFFEBD9), Color(0xFFD8EAD5))), Color(0xFF314052), Color(0x99FFFFFF), 20),
    CellStyle(2, "清新风格", Brush.linearGradient(listOf(Color(0xFFD9FFF1), Color(0xFFBDE7FF))), Color(0xFF043D3D), Color(0xFF75C4C0), 16),
    CellStyle(3, "日落风格", Brush.linearGradient(listOf(Color(0xFFFFC39E), Color(0xFFFF7A91))), Color(0xFF3A1018), Color(0x66FFFFFF), 22),
    CellStyle(4, "极简黑白", Brush.linearGradient(listOf(Color(0xFFEAEAEA), Color(0xFFCFCFCF))), Color(0xFF111111), Color(0xFF8F8F8F), 10),
    CellStyle(5, "海盐风格", Brush.linearGradient(listOf(Color(0xFFDBF5FF), Color(0xFF9FD7F0))), Color(0xFF0F3B4D), Color(0xFF69AECF), 18),
    CellStyle(6, "森林风格", Brush.linearGradient(listOf(Color(0xFFC8E6C9), Color(0xFF89B68A))), Color(0xFF1E3621), Color(0xFF557D58), 14),
    CellStyle(7, "复古胶片", Brush.linearGradient(listOf(Color(0xFFEBDAB8), Color(0xFFCFB48B))), Color(0xFF4A3620), Color(0xFF9A7D56), 12),
    CellStyle(8, "糖果风格", Brush.linearGradient(listOf(Color(0xFFFFD5E5), Color(0xFFFFF3B0))), Color(0xFF5A2F44), Color(0xFFEEA6C4), 24),
    CellStyle(9, "冰川风格", Brush.linearGradient(listOf(Color(0xFFE3F3FF), Color(0xFFC8DFFF))), Color(0xFF1E3354), Color(0xFF96B7E4), 16),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeightApp(activity: ComponentActivity) {
    val context = LocalContext.current
    val haptic = remember(context) { AppHaptics(context) }
    val entries = remember { mutableStateListOf<WeightEntry>() }

    var columns by rememberSaveable { mutableIntStateOf(6) }
    var styleId by rememberSaveable { mutableIntStateOf(0) }
    var loaded by rememberSaveable { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditId by remember { mutableStateOf<Long?>(null) }
    var showChart by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }
    var tempWeight by remember { mutableStateOf("") }
    var tappedDateId by remember { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val payload = ExportPayload(columns, styleId, entries.toList())
            val data = json.encodeToString(payload)
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(data) }
            }.isSuccess
            if (ok) haptic.success() else haptic.error()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { text ->
                runCatching { json.decodeFromString<ExportPayload>(text) }.getOrNull()?.let { payload ->
                    columns = max(6, payload.columns)
                    styleId = payload.styleId.coerceIn(0, allStyles.lastIndex)
                    entries.clear()
                    entries.addAll(payload.entries.sortedBy { it.timestamp })
                    haptic.success()
                } ?: haptic.error()
            } ?: haptic.error()
        } else {
            haptic.error()
        }
    }

    LaunchedEffect(Unit) {
        val pref = activity.dataStore.data.first()
        columns = max(6, pref[columnsKey] ?: 6)
        styleId = (pref[styleKey] ?: 0).coerceIn(0, allStyles.lastIndex)
        val saved = pref[entriesKey]
        if (!saved.isNullOrBlank()) {
            runCatching { json.decodeFromString<List<WeightEntry>>(saved) }.getOrNull()?.let {
                entries.addAll(it.sortedBy { item -> item.timestamp })
            }
        }
        loaded = true
    }

    LaunchedEffect(entries.toList(), columns, styleId, loaded) {
        if (!loaded) return@LaunchedEffect
        activity.dataStore.edit {
            it[entriesKey] = json.encodeToString(entries.toList())
            it[columnsKey] = max(6, columns)
            it[styleKey] = styleId
        }
    }

    LaunchedEffect(tappedDateId) {
        if (tappedDateId != null) {
            delay(3_000)
            tappedDateId = null
        }
    }

    val style = allStyles[styleId]

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E11)).padding(10.dp)) {
        val spacing = 8.dp
        val sizePerCell = ((maxWidth - spacing * (columns + 1)) / columns)
        val textSize = ((sizePerCell.value * 0.28f).coerceIn(12f, 32f)).sp

        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(spacing)
        ) {
            items(entries, key = { it.id }) { entry ->
                WeightCell(
                    display = if (tappedDateId == entry.id) formatDate(entry.timestamp) else entry.value,
                    textSize = textSize,
                    style = style,
                    onClick = {
                        tappedDateId = entry.id
                        haptic.tap()
                    },
                    onLongPress = {
                        showEditId = entry.id
                        tempWeight = entry.value
                        haptic.longPress()
                    }
                )
            }

            item {
                AddCell(
                    textSize = textSize,
                    style = style,
                    onClick = {
                        tempWeight = ""
                        showAddDialog = true
                        haptic.tap()
                    }
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EmojiButton("📈") {
                showChart = true
                haptic.tap()
            }
            EmojiButton("⚙️") {
                showConfig = true
                haptic.tap()
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                haptic.tap()
            },
            text = {
                TextField(
                    value = tempWeight,
                    onValueChange = { tempWeight = sanitizeWeight(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempWeight.isNotBlank()) {
                        entries.add(WeightEntry(System.currentTimeMillis(), tempWeight, System.currentTimeMillis()))
                        haptic.success()
                    } else {
                        haptic.error()
                    }
                    showAddDialog = false
                }) { Text("✓") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    haptic.tap()
                }) { Text("✕") }
            }
        )
    }

    val editEntry = entries.firstOrNull { it.id == showEditId }
    if (editEntry != null) {
        AlertDialog(
            onDismissRequest = {
                showEditId = null
                haptic.tap()
            },
            text = {
                TextField(
                    value = tempWeight,
                    onValueChange = { newValue ->
                        tempWeight = sanitizeWeight(newValue)
                        val idx = entries.indexOfFirst { it.id == editEntry.id }
                        if (idx >= 0) {
                            entries[idx] = editEntry.copy(value = tempWeight)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    entries.removeAll { it.id == editEntry.id }
                    showEditId = null
                    haptic.success()
                }) { Text("🗑️") }
            }
        )
    }

    if (showChart) {
        AlertDialog(
            onDismissRequest = {
                showChart = false
                haptic.tap()
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color(0xFF11141A), RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    BezierChart(entries.mapNotNull { it.value.toFloatOrNull() })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showChart = false
                    haptic.tap()
                }) { Text("✕") }
            }
        )
    }

    if (showConfig) {
        AlertDialog(
            onDismissRequest = {
                showConfig = false
                haptic.tap()
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(text = "列数 ${max(6, columns)}", fontWeight = FontWeight.Medium)
                    Slider(
                        value = columns.toFloat(),
                        onValueChange = { columns = max(6, it.toInt()) },
                        onValueChangeFinished = { haptic.tap() },
                        valueRange = 6f..14f
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(text = "样式", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    allStyles.forEach { styleItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    brush = styleItem.background,
                                    shape = RoundedCornerShape(styleItem.corner.dp)
                                )
                                .border(
                                    width = if (styleId == styleItem.id) 2.dp else 1.dp,
                                    color = if (styleId == styleItem.id) Color.White else styleItem.borderColor,
                                    shape = RoundedCornerShape(styleItem.corner.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                                .combinedClickable(onClick = {
                                    styleId = styleItem.id
                                    haptic.tap()
                                }, onLongClick = {}),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = styleItem.name,
                                color = styleItem.textColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (styleId == styleItem.id) {
                                Text(text = "✓", color = styleItem.textColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            val filename = "weights_${System.currentTimeMillis()}.json"
                            exportLauncher.launch(filename)
                            haptic.tap()
                        }) { Text("导出") }
                        TextButton(onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                            haptic.tap()
                        }) { Text("导入") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfig = false
                    haptic.tap()
                }) { Text("✕") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiButton(emoji: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color(0xFF1E2330), RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF3A4258), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WeightCell(
    display: String,
    textSize: TextUnit,
    style: CellStyle,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(style.background, RoundedCornerShape(style.corner.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(style.corner.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = display,
            color = style.textColor,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddCell(
    textSize: TextUnit,
    style: CellStyle,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(style.background, RoundedCornerShape(style.corner.dp))
            .border(1.dp, style.borderColor, RoundedCornerShape(style.corner.dp))
            .combinedClickable(onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "➕", color = style.textColor, fontSize = textSize)
    }
}

@Composable
private fun BezierChart(values: List<Float>) {
    val sorted = values.takeIf { it.isNotEmpty() } ?: listOf(0f)
    val min = sorted.minOrNull() ?: 0f
    val maxValue = sorted.maxOrNull() ?: 0f
    val range = (maxValue - min).takeIf { it != 0f } ?: 1f

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (sorted.size == 1) {
            drawCircle(color = Color(0xFF74D3FF), radius = 6.dp.toPx(), center = center)
            return@Canvas
        }

        val stepX = size.width / (sorted.size - 1)
        val points = sorted.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - min) / range
            val y = size.height - normalized * size.height
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val start = points[i]
                val end = points[i + 1]
                val c1 = Offset(start.x + stepX / 2f, start.y)
                val c2 = Offset(end.x - stepX / 2f, end.y)
                cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF74D3FF),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("M.d", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun sanitizeWeight(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot < 0) return filtered.take(6)
    val head = filtered.substring(0, firstDot + 1)
    val tail = filtered.substring(firstDot + 1).replace(".", "")
    return (head + tail).take(7)
}

private class AppHaptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun tap() = vibrate(14L, 100)

    fun longPress() = vibrate(24L, 155)

    fun success() = vibrate(28L, 180)

    fun error() = vibrate(36L, 220)

    private fun vibrate(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(durationMs)
        }
    }
}
