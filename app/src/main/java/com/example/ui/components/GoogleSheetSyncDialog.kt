package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppsScriptSyncService
import com.example.data.SheetSyncConfig
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalLedgerColors

@Composable
fun GoogleSheetSyncDialog(
    config: SheetSyncConfig,
    isSyncing: Boolean,
    hasWorkers: Boolean,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onToggleAutoPush: (Boolean) -> Unit,
    onTestConnection: (String) -> Unit,
    onPushToSheet: (String) -> Unit,
    onPullFromSheet: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    val clipboard = LocalClipboardManager.current
    var urlInput by remember(config.webAppUrl) { mutableStateOf(config.webAppUrl) }
    var showScriptGuide by remember { mutableStateOf(config.webAppUrl.isBlank()) }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Firm Google Sheet Sync",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Apne firm ke Google Sheet par Apps Script Web App URL (/exec) se direct update & dono phones ke beech sync karein.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Apps Script Web App URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = true,
                    trailingIcon = {
                        if (urlInput.isNotBlank()) {
                            IconButton(
                                onClick = { urlInput = "" },
                                enabled = !isSyncing
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear URL")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val clip = clipboard.getText()?.text.orEmpty()
                                    if (clip.isNotBlank()) {
                                        urlInput = AppsScriptSyncService.normalizeInputUrl(clip)
                                    }
                                },
                                enabled = !isSyncing
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste URL")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_script_url_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedButton(
                    onClick = {
                        val cleaned = AppsScriptSyncService.normalizeInputUrl(urlInput)
                        urlInput = cleaned
                        onSaveUrl(cleaned)
                        onTestConnection(cleaned)
                    },
                    enabled = !isSyncing && urlInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("test_sheet_link_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Test Link (Verify Connection)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cleaned = AppsScriptSyncService.normalizeInputUrl(urlInput)
                            urlInput = cleaned
                            onSaveUrl(cleaned)
                            onPushToSheet(cleaned)
                        },
                        enabled = !isSyncing && hasWorkers && urlInput.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("push_to_sheet_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Push Sheet")
                    }

                    FilledTonalButton(
                        onClick = {
                            val cleaned = AppsScriptSyncService.normalizeInputUrl(urlInput)
                            urlInput = cleaned
                            onSaveUrl(cleaned)
                            onPullFromSheet(cleaned)
                        },
                        enabled = !isSyncing && urlInput.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pull_from_sheet_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pull Sync")
                    }
                }

                if (!hasWorkers) {
                    Text(
                        text = "Note: Abhi is phone me 0 workers hain, isliye 'Push Sheet' band hai. Pehle 1 worker add karein (ya 2nd phone par 'Pull Sync' dabaye).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Push Changes", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Hajri / Advance save hote hi Google Sheet auto-update ho",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.autoPush,
                        onCheckedChange = {
                            val cleaned = AppsScriptSyncService.normalizeInputUrl(urlInput)
                            urlInput = cleaned
                            onSaveUrl(cleaned)
                            onToggleAutoPush(it)
                        },
                        modifier = Modifier.testTag("auto_push_switch")
                    )
                }

                if (config.lastSyncMessage.isNotBlank()) {
                    val isOk = config.lastSyncSuccess
                    val bgColor = when (isOk) {
                        true -> ledgerColors.presentBg
                        false -> MaterialTheme.colorScheme.errorContainer
                        null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    }
                    val textColor = when (isOk) {
                        true -> ledgerColors.presentText
                        false -> MaterialTheme.colorScheme.onErrorContainer
                        null -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (isOk == false) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = config.lastSyncMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }

                HorizontalDivider(color = ledgerColors.subtleDivider)

                Surface(
                    onClick = { showScriptGuide = !showScriptGuide },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, ledgerColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Apps Script Code & Setup",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Icon(
                            imageVector = if (showScriptGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle guide"
                        )
                    }
                }

                AnimatedVisibility(visible = showScriptGuide) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "1. Firm ki Google Sheet me Extensions → Apps Script kholein.\n" +
                                "2. Purana saara code delete karke neeche diya naya code paste karein aur SAVE (Ctrl+S / Cmd+S) dabaye.\n" +
                                "3. Deploy → New deployment → Type: Web app → Execute as: Me → Who has access: ANYONE chunein.\n" +
                                "4. Authorize karke '/exec' wala Web app URL copy karein aur upar paste karke 'Test Link' dabaye.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(AppsScriptSyncService.APPS_SCRIPT_CODE))
                                onShowMessage("Apps Script code copied! Paste in Extensions → Apps Script.")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("copy_apps_script_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy Apps Script Code")
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ledgerColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = AppsScriptSyncService.APPS_SCRIPT_CODE.take(420) + "\n// ... (tap Copy above for full script)",
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleaned = AppsScriptSyncService.normalizeInputUrl(urlInput)
                    urlInput = cleaned
                    onSaveUrl(cleaned)
                    onDismiss()
                },
                enabled = !isSyncing
            ) {
                Text("Save & Close")
            }
        }
    )
}
