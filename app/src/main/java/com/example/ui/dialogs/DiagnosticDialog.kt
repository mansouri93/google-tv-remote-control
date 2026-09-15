package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiagnosticStatus
import com.example.model.TvDiagnosticResult
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun DiagnosticDialog(
    result: TvDiagnosticResult?,
    isLoading: Boolean,
    onRetry: (String) -> Unit,
    onConnectIfReady: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (result == null && !isLoading) return

    val ip = result?.ip ?: "192.168.1.101"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        titleContentColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (result?.status) {
                                DiagnosticStatus.READY_TO_CONNECT -> EmeraldAccent.copy(alpha = 0.2f)
                                DiagnosticStatus.ADB_DEBUGGING_DISABLED -> AmberAccent.copy(alpha = 0.2f)
                                else -> RoseAccent.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (result?.status) {
                            DiagnosticStatus.READY_TO_CONNECT -> Icons.Default.CheckCircle
                            DiagnosticStatus.ADB_DEBUGGING_DISABLED -> Icons.Default.Warning
                            else -> Icons.Default.HelpOutline
                        },
                        contentDescription = null,
                        tint = when (result?.status) {
                            DiagnosticStatus.READY_TO_CONNECT -> EmeraldAccent
                            DiagnosticStatus.ADB_DEBUGGING_DISABLED -> AmberAccent
                            else -> RoseAccent
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "عیب‌یابی اتصال به تلویزیون",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "آدرس: $ip",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = CyanAccent)
                    Text(
                        text = "در حال تست پورت‌ها و بررسی پاسخ‌دهی تلویزیون ($ip)...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            } else if (result != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Port status cards
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate850),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PortStatusRow(
                                title = "پاسخ‌دهی شبکه (Ping / ICMP)",
                                isOpen = result.isReachable,
                                details = if (result.isReachable) "تلویزیون در شبکه پاسخ می‌دهد (${result.latencyMs}ms)" else "پاسخی دریافت نشد"
                            )
                            PortStatusRow(
                                title = "سرویس Google Cast (پورت ۸۰۰۸)",
                                isOpen = result.isCastOpen,
                                details = if (result.isCastOpen) "سرویس کست فعال است" else "غیرفعال"
                            )
                            PortStatusRow(
                                title = "سرویس ریموت نسخه ۲ (پورت ۶۴۶۶)",
                                isOpen = result.isRemoteV2Open,
                                details = if (result.isRemoteV2Open) "پورت ریموت باز است" else "بسته"
                            )
                            PortStatusRow(
                                title = "اشکال‌زدایی ADB (پورت ۵۵۵۵)",
                                isOpen = result.isAdbOpen,
                                details = if (result.isAdbOpen) "فعال و آماده اتصال ریموت" else "خاموش (کنترل نیازمند این پورت است)"
                            )
                        }
                    }

                    // Explanation and instructions
                    when (result.status) {
                        DiagnosticStatus.READY_TO_CONNECT -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "همه چیز آماده است!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldAccent
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "پورت ۵۵۵۵ تلویزیون شما باز است و اپلیکیشن می‌تواند فوراً متصل شود.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        DiagnosticStatus.ADB_DEBUGGING_DISABLED -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = AmberAccent.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "تلویزیون شما روشن است، اما دسترسی کنترل خاموش است!",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AmberAccent
                                    )
                                    Text(
                                        text = "برای رفع خطای connection failed، مراحل زیر را در تلویزیون انجام دهید:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "۱. با ریموت تلویزیون به تنظیمات (Settings) > سیستم (System) > درباره (About) بروید.\n" +
                                                "۲. روی گزینه «بیلد سیستم‌عامل Android TV» دقیقاً ۷ بار کلیک کنید تا پیام 'شما برنامه‌نویس شدید' ظاهر شود.\n" +
                                                "۳. یک مرحله به عقب برگشته، وارد منوی جدید «گزینه‌های توسعه‌دهنده» (Developer options) شوید.\n" +
                                                "۴. گزینه «اشکال‌زدایی شبکه» (Network Debugging) یا USB Debugging را روشن کنید.\n" +
                                                "۵. دکمه 'تست مجدد' را لمس کنید و در صورت نمایش پیام روی تلویزیون، گزینه Always Allow را تایید کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RoseAccent.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "تلویزیون در آدرس $ip پاسخ نمی‌دهد",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = RoseAccent
                                    )
                                    Text(
                                        text = "• مطمئن شوید تلویزیون روشن است.\n" +
                                                "• بررسی کنید گوشی و تلویزیون به یک مودم وای‌فای متصل باشند.\n" +
                                                "• در تنظیمات شبکه تلویزیون، آی‌پی فعلی آن را بررسی کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result?.status == DiagnosticStatus.READY_TO_CONNECT) {
                Button(
                    onClick = {
                        onConnectIfReady(ip)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent, contentColor = Slate950),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("diag_btn_connect")
                ) {
                    Text("اتصال فوری", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onRetry(ip) },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Slate950),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("diag_btn_retry")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تست مجدد اتصال", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400)
            ) {
                Text("بستن")
            }
        }
    )
}

@Composable
private fun PortStatusRow(title: String, isOpen: Boolean, details: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )
            Text(
                text = details,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOpen) EmeraldAccent else Slate400
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isOpen) EmeraldAccent.copy(alpha = 0.2f) else Slate700.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOpen) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (isOpen) EmeraldAccent else Slate400,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
