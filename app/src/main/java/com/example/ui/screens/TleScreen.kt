package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SatelliteTle
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TleScreen(
    viewModel: TleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var showSettingsDialog by remember { mutableStateOf(false) }
    val passResult by viewModel.passResult.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TLE Finder",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات ایستگاه زمینی",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Render in Right-to-Left (Persian) by default, only TLE text box is forced LTR
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (showSettingsDialog) {
                SettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSettingsDialog = false }
                )
            }

            passResult?.let { (satName, passes) ->
                PassResultDialog(
                    satelliteName = satName,
                    passes = passes,
                    onDismiss = { viewModel.clearPassResult() },
                    viewModel = viewModel
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro banner
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .widthIn(max = 600.dp)
                    ) {
                        Text(
                            text = "جستجوی مداری ماهواره‌ها",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نام ماهواره (مثلاً ISS) یا Norad ID (مانند 25544) را وارد کنید تا آخرین TLE آن مستقیماً دریافت شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Search Panel
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { viewModel.onQueryChanged(it) },
                                label = { Text("نام یا شناسه ماهواره") },
                                placeholder = { Text("مثلاً ISS یا 25544") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "پاک کردن"
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search,
                                    keyboardType = KeyboardType.Text
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { viewModel.searchSatellite() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("satellite_query_input"),
                                shape = RoundedCornerShape(16.dp)
                            )

                            Button(
                                onClick = { viewModel.searchSatellite() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("search_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "دریافت اطلاعات TLE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Favorite Satellites List (Local State)
                if (favorites.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "ماهواره‌های نشان‌شده (علاقه‌مندی‌ها):",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                favorites.forEach { (noradId, name) ->
                                    AssistChip(
                                        onClick = { viewModel.quickSearch(noradId) },
                                        label = { Text(name, fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.toggleFavorite(noradId, name) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف از علاقه‌مندی‌ها",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.testTag("favorite_chip_$noradId")
                                    )
                                }
                            }
                        }
                    }
                }

                // Visual separator or Spacer
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // UI Screen State Selector
                when (val state = uiState) {
                    is TleUiState.Idle -> {
                        item {
                            EmptyPlaceholder(
                                title = "آماده دریافت داده",
                                subtitle = "شناسه یا نام ماهواره را در بالا وارد کنید تا اطلاعات مداری زنده آن نمایش داده شود."
                            )
                        }
                    }
                    is TleUiState.Loading -> {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "درحال اتصال به سیستم Celestrak و تحلیل داده‌ها...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    is TleUiState.Error -> {
                        item {
                            ErrorBlock(
                                message = state.message,
                                onRetry = { viewModel.searchSatellite() }
                            )
                        }
                    }
                    is TleUiState.Success -> {
                        if (state.satellites.isEmpty()) {
                            item {
                                EmptyPlaceholder(
                                    title = "ماهواره‌ای یافت نشد",
                                    subtitle = "لطفاً مجدداً با عبارتی دیگر جستجو کنید."
                                )
                            }
                        } else {
                            items(state.satellites) { tle ->
                                val isFav = favorites.any { it.first == tle.noradId }
                                TleCard(
                                    tle = tle,
                                    isFavorite = isFav,
                                    onToggleFavorite = { viewModel.toggleFavorite(tle.noradId, tle.name) },
                                    onShare = { shareTleAsText(context, tle.name, tle.rawTle) },
                                    onDownload = { downloadTleAsTxt(context, tle.name, tle.rawTle) },
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(tle.rawTle))
                                        Toast.makeText(context, "کدهای TLE به حافظه منتقل شد", Toast.LENGTH_SHORT).show()
                                    },
                                    onCalculatePass = { viewModel.calculatePass(tle) }
                                )
                            }
                        }
                    }
                }
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(top = 16.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            thickness = 1.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نسخه " + viewModel.getAppVersion(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/bahram-pouralibaba-1a992239"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "خطا در باز کردن لینک: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = "توسعه‌دهنده: بهرام پورعلی‌بابا",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF0A66C2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "in",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaceholder(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "بروز خطا در یافتن اطلاعات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "تلاش مجدد")
                Spacer(modifier = Modifier.width(8.dp))
                Text("تلاش مجدد", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TleCard(
    tle: SatelliteTle,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onCopy: () -> Unit,
    onCalculatePass: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }
    var expandedHistory by remember { mutableStateOf(false) }
    var selectedHistoryTle by remember { mutableStateOf<SatelliteTle?>(null) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer
    val cardOnBg = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(vertical = 8.dp)
            .testTag("tle_data_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tle.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = cardOnBg
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NORAD ID: ${tle.noradId}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = cardOnBg.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .testTag("favorite_toggle_${tle.noradId}")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else cardOnBg.copy(alpha = 0.35f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Surface(
                        color = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "بروزرسانی جدید (LATEST)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Monospaced Code Terminal Block for TLE text (FORCED LTR Layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دریافت شده از Celestrak (TLE)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Force Left to Right
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = tle.rawTle,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontSize = 12.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = LocalTextStyle.current.copy(
                            textDirection = TextDirection.Ltr
                        )
                    )
                }
            }

            // Quick actions buttons layout (Share, Download, Copy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val buttonBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
                val buttonContentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary

                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonContentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("کپی کدهای TLE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonContentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اشتراک‌گذاری", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("download_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                     imageVector = Icons.Default.Download,
                     contentDescription = null,
                     modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("دانلود فایل TLE.txt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = onCalculatePass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("calculate_pass_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                     imageVector = Icons.Default.MyLocation,
                     contentDescription = null,
                     modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("محاسبه زمان گذر و بیشترین الویشن در ایستگاه شما", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Expandable Analyzed/Parsed TLE Dashboard (Space Craft physics)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expandedDetails = !expandedDetails },
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحلیل پارامترهای فیزیکی و مداری",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = cardOnBg
                        )
                    }
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = cardOnBg.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(
                visible = expandedDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.50f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val labelColor = cardOnBg.copy(alpha = 0.7f)
                    val valueColor = cardOnBg

                    Row(modifier = Modifier.fillMaxWidth()) {
                        ParameterItem(
                            label = "شناسه مداری NORAD",
                            value = tle.noradId,
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                        ParameterItem(
                            label = "طبقه‌بندی اطلاعاتی",
                            value = tle.classification,
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                    }
                    HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.4f), thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ParameterItem(
                            label = "کد بین‌المللی پرتاب (Designator)",
                            value = tle.designator.ifEmpty { "ندارد" },
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                        ParameterItem(
                            label = "زمان مرجع اندازه‌گیری (Epoch)",
                            value = tle.epoch,
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                    }
                    HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.4f), thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ParameterItem(
                            label = "زاویه تمایل مداری (Inclination)",
                            value = "${tle.inclination}°",
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                        ParameterItem(
                            label = "خروج از مرکز (Eccentricity)",
                            value = tle.eccentricity,
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                    }
                    HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.4f), thickness = 1.dp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ParameterItem(
                            label = "سرعت دَوران (Mean Motion)",
                            value = "${tle.meanMotion} دور در روز",
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                        ParameterItem(
                            label = "شماره دَوران کل (Orbit Rev)",
                            value = tle.revNumber,
                            modifier = Modifier.weight(1f),
                            labelColor = labelColor,
                            valueColor = valueColor
                        )
                    }
                }
            }

            // Expandable Historical TLEs
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expandedHistory = !expandedHistory }
                    .testTag("history_button"),
                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تاریخچه ردیابی کدهای TLE (۵ مدرک اخیر)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = cardOnBg
                        )
                    }
                    Icon(
                        imageVector = if (expandedHistory) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand History",
                        tint = cardOnBg.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(
                visible = expandedHistory,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.50f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val historyList = remember(tle) { tle.generateHistory(5) }
                    
                    historyList.forEachIndexed { index, historicalTle ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedHistoryTle = historicalTle }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (index == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (index == 0) "آخرین بروزرسانی (تازه)" else "دوره مداری قبلی (${index})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cardOnBg
                                    )
                                    Text(
                                        text = "تاریخ اندازه‌گیری: ${historicalTle.epochDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cardOnBg.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "جزئیات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "View Details",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        if (index < historyList.lastIndex) {
                            HorizontalDivider(
                                color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.4f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Historical details Dialog
    if (selectedHistoryTle != null) {
        val histTle = selectedHistoryTle!!
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        
        AlertDialog(
            onDismissRequest = { selectedHistoryTle = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "جزئیات تاریخچه TLE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تاریخ: ${histTle.epochDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "داده مداری TLE (تاریخچه)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(histTle.rawTle))
                                        Toast.makeText(context, "کدهای TLE به حافظه منتقل شد", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "کپی کدهای TLE",
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = histTle.rawTle,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                )
                            }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "پارامترهای فیزیکی محاسبه‌شده:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            
                            ParameterRow(label = "زمان اندازه‌گیری (Epoch)", value = histTle.epoch)
                            ParameterRow(label = "تمایل مداری (Inclination)", value = "${histTle.inclination}°")
                            ParameterRow(label = "خروج از مرکز (Eccentricity)", value = histTle.eccentricity)
                            ParameterRow(label = "سرعت دوران (Mean Motion)", value = "${histTle.meanMotion} دور/روز")
                            ParameterRow(label = "شماره مدار (Rev Number)", value = histTle.revNumber)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHistoryTle = null }) {
                    Text("بستن", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ParameterRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ParameterItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.Unspecified,
    valueColor: Color = Color.Unspecified
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 11.sp, color = labelColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = if (value.all { it.isDigit() || it == '.' || it == ' ' || it == '-' || it == '°' }) FontFamily.Monospace else FontFamily.Default
        )
    }
}

// Inline implementation of FlowRow for simpler package imports & maximum compatibility
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val xSpace = 8.dp.roundToPx()
        val ySpace = 8.dp.roundToPx()

        var currentX = 0
        var currentY = 0
        var maxRowHeight = 0

        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            if (currentX + placeable.width > constraints.maxWidth) {
                currentX = 0
                currentY += maxRowHeight + ySpace
                maxRowHeight = 0
            }
            maxRowHeight = maxOf(maxRowHeight, placeable.height)
            currentX += placeable.width + xSpace
            placeable
        }

        currentX = 0
        currentY = 0
        maxRowHeight = 0
        val positions = placeables.map { placeable ->
            if (currentX + placeable.width > constraints.maxWidth) {
                currentX = 0
                currentY += maxRowHeight + ySpace
                maxRowHeight = 0
            }
            val pos = androidx.compose.ui.unit.IntOffset(currentX, currentY)
            maxRowHeight = maxOf(maxRowHeight, placeable.height)
            currentX += placeable.width + xSpace
            pos
        }

        val totalHeight = currentY + maxRowHeight
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(positions[index])
            }
        }
    }
}

// MediaStore helper and download function
fun downloadTleAsTxt(context: Context, satelliteName: String, tleContent: String) {
    val cleanName = satelliteName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val filename = "${cleanName}_TLE.txt"
    var outputStream: OutputStream? = null

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            outputStream = FileOutputStream(file)
        }

        if (outputStream != null) {
            outputStream.use { out ->
                out.write(tleContent.toByteArray())
            }
            Toast.makeText(context, "فایل با موفقیت ذخیره شد:\nDownloads/$filename", Toast.LENGTH_LONG).show()
        } else {
            throw Exception("عدم دسترسی به حافظه جهت ایجاد فایل")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "خطا در دانلود فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// Sharing helper
fun shareTleAsText(context: Context, satelliteName: String, tleContent: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "دهنه مداری (TLE) ماهواره $satelliteName")
            putExtra(Intent.EXTRA_TEXT, tleContent)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری کد TLE")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در تلاش برای اشتراک‌گذاری: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsDialog(
    viewModel: TleViewModel,
    onDismiss: () -> Unit
) {
    val groundStations by viewModel.groundStations.collectAsState()
    
    var nameState by remember { mutableStateOf("") }
    var latState by remember { mutableStateOf("") }
    var lngState by remember { mutableStateOf("") }
    var altState by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تنظیمات ایستگاه زمینی و موقعیت",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "موقعیت مکان‌گیر را برای اندازه‌گیری دقیق زاویه دید و زمان‌های عبور ماهواره‌ها ذخیره کنید. حداکثر ۱۰ ایستگاه مجاز است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    "ایستگاه‌های ثبت‌شده:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (groundStations.isEmpty()) {
                    Text(
                        "هیچ ایستگاهی یافت نشد.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    groundStations.forEach { station ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (station.isPrimary) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ground_station_card_${station.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = station.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (station.isPrimary) 
                                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            if (station.isPrimary) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "پرکاربرد",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "عرض: ${station.latitude}° | طول: ${station.longitude}° | ارتفاع: ${station.altitude}متر",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (station.isPrimary) 
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (!station.isPrimary) {
                                            IconButton(
                                                onClick = { viewModel.setPrimaryGroundStation(station.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "انتخاب به عنوان پرکاربرد",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        if (groundStations.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.deleteGroundStation(station.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف ایستگاه",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (!showAddForm && groundStations.size < 10) {
                    OutlinedButton(
                        onClick = { showAddForm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن ایستگاه زمینی جدید", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showAddForm) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "افزودن ایستگاه نو:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = nameState,
                                onValueChange = { nameState = it },
                                label = { Text("نام ایستگاه") },
                                placeholder = { Text("مثلاً: رصدخانه تهران") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = latState,
                                onValueChange = { latState = it },
                                label = { Text("عرض جغرافیایی (Latitude)") },
                                placeholder = { Text("بین ۹۰ تا ۹۰- درجه") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = lngState,
                                onValueChange = { lngState = it },
                                label = { Text("طول جغرافیایی (Longitude)") },
                                placeholder = { Text("بین ۱۸۰ تا ۱۸۰- درجه") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = altState,
                                onValueChange = { altState = it },
                                label = { Text("ارتفاع از سطح دریا (متر)") },
                                placeholder = { Text("به عدد، مثلاً ۱۲۰۰") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            errorMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val latVal = latState.toDoubleOrNull()
                                        val lngVal = lngState.toDoubleOrNull()
                                        val altVal = altState.toDoubleOrNull() ?: 0.0

                                        if (nameState.trim().isEmpty()) {
                                            errorMessage = "نام ایستگاه نمی‌تواند خالی باشد."
                                            return@Button
                                        }
                                        if (latVal == null || latVal < -90.0 || latVal > 90.0) {
                                            errorMessage = "عرض جغرافیایی معتبر بین -۹۰ و ۹۰ وارد کنید."
                                            return@Button
                                        }
                                        if (lngVal == null || lngVal < -180.0 || lngVal > 180.0) {
                                            errorMessage = "طول جغرافیایی معتبر بین -۱۸۰ و ۱۸۰ وارد کنید."
                                            return@Button
                                        }

                                        val success = viewModel.addGroundStation(
                                            name = nameState,
                                            lat = latVal,
                                            lng = lngVal,
                                            alt = altVal
                                        )

                                        if (success) {
                                            nameState = ""
                                            latState = ""
                                            lngState = ""
                                            altState = ""
                                            errorMessage = null
                                            showAddForm = false
                                        } else {
                                            errorMessage = "امکان افزودن ایستگاه بیشتر ندارید (حداکثر ۱۰ ایستگاه)."
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ذخیره ایستگاه")
                                }

                                OutlinedButton(
                                    onClick = {
                                        showAddForm = false
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("انصراف")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تایید و بستن")
            }
        }
    )
}

@Composable
fun PassResultDialog(
    satelliteName: String,
    passes: List<com.example.data.SatellitePass>,
    onDismiss: () -> Unit,
    viewModel: TleViewModel
) {
    val groundStations by viewModel.groundStations.collectAsState()
    val activeStation = groundStations.firstOrNull { it.isPrimary } ?: groundStations.firstOrNull()

    fun formatTime(epochMs: Long): String {
        return try {
            val instant = java.time.Instant.ofEpochMilli(epochMs)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            "--:--:--"
        }
    }

    fun formatDate(epochMs: Long): String {
        return try {
            val instant = java.time.Instant.ofEpochMilli(epochMs)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            "----/--/--"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "پیش‌بینی گذر ماهواره",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ماهواره: $satelliteName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                activeStation?.let { station ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ایستگاه محاسباتی فعال:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${station.name} (${station.latitude}°, ${station.longitude}°)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (passes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "در ۲۴ ساعت آینده گذری با زاویه بیش از ۱۰ درجه در این ایستگاه یافت نشد.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    Text(
                        text = "تعداد گذر محاسبه شده: ${passes.size} گذر در ۲۴ ساعت آینده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    passes.forEachIndexed { index, pass ->
                        val durationSeconds = (pass.endEpochMs - pass.startEpochMs) / 1000L
                        val durationMin = durationSeconds / 60
                        val durationSec = durationSeconds % 60

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = " گذر شماره ${index + 1} ",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Table Header
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(vertical = 8.dp, horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "بخش / رویداد",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1.2f),
                                            textAlign = TextAlign.Right
                                        )
                                        Text(
                                            text = "زمان / جزئیات",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1.5f),
                                            textAlign = TextAlign.Left
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                                    // Row 1: Date
                                    PassTableRow(
                                        icon = Icons.Default.DateRange,
                                        label = "تاریخ گذر",
                                        value = formatDate(pass.startEpochMs)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Row 2: Sunrise (Start)
                                    PassTableRow(
                                        icon = Icons.Default.Schedule,
                                        label = "طلوع (شروع گذر)",
                                        value = formatTime(pass.startEpochMs)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Row 3: Apex (Peak Elevation)
                                    PassTableRow(
                                        icon = Icons.Default.MyLocation,
                                        label = "اوج (بیشترین ارتفاع)",
                                        value = formatTime(pass.maxElevationTimeMs),
                                        accentContent = {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = String.format(java.util.Locale.US, " %.1f° ", pass.maxElevation),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Row 4: Sunset (End)
                                    PassTableRow(
                                        icon = Icons.Default.Schedule,
                                        label = "غروب (پایان گذر)",
                                        value = formatTime(pass.endEpochMs)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Row 5: Duration
                                    PassTableRow(
                                        icon = Icons.Default.Info,
                                        label = "مدت حضور در آسمان",
                                        value = "$durationMin دقیقه و " + String.format(java.util.Locale.US, "%02d", durationSec) + " ثانیه"
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
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("متوجه شدم")
            }
        },
        dismissButton = {
            if (passes.isNotEmpty()) {
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val activeStationCoords = activeStation?.let { "${it.latitude}°, ${it.longitude}°" }
                        com.example.data.PassPdfHelper.generateAndSharePassesPdf(
                            context = context,
                            satelliteName = satelliteName,
                            passes = passes,
                            activeStationName = activeStation?.name,
                            activeStationCoords = activeStationCoords
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("اشتراک‌گذاری PDF")
                    }
                }
            }
        }
    )
}

@Composable
fun PassTableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Right
        )
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (accentContent != null) {
                accentContent()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
