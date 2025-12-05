package com.harichselvamc.grammo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GrammoScreen()
            }
        }
    }
}

@Composable
fun GrammoScreen() {
    var inputText by remember { mutableStateOf("") }
    var correctedText by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("0 issues found") }
    var isLoading by remember { mutableStateOf(false) }

    // Now using our own GrammarMatch type
    var matches by remember { mutableStateOf<List<LanguageToolHelper.GrammarMatch>>(emptyList()) }

    var showSuggestionDialog by remember { mutableStateOf(false) }
    var selectedMatchIndex by remember { mutableStateOf<Int?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val wordCount = inputText.trim()
        .split("\\s+".toRegex())
        .filter { it.isNotEmpty() }
        .size
    val charCount = inputText.length

    val annotatedOriginalText = remember(inputText, matches) {
        buildAnnotatedErrorText(inputText, matches)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Grammo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Start
        )

        Text(
            text = "100% Grammar Checking (LanguageTool engine)",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .background(Color.DarkGray, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White
        )

        Text(
            text = "Words: $wordCount  •  Characters: $charCount",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Type or paste text here…") },
            maxLines = Int.MAX_VALUE
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Errors in text",
            style = MaterialTheme.typography.titleSmall
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(vertical = 4.dp)
                .background(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            if (inputText.isBlank() || matches.isEmpty()) {
                Text(
                    text = if (inputText.isBlank())
                        "No text yet."
                    else
                        "No issues found or not checked yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            } else {
                ClickableText(
                    text = annotatedOriginalText,
                    onClick = { offset ->
                        val annotation = annotatedOriginalText.getStringAnnotations(
                            tag = "ERROR",
                            start = offset,
                            end = offset
                        ).firstOrNull()

                        annotation?.let {
                            val index = it.item.toIntOrNull()
                            if (index != null && index in matches.indices) {
                                selectedMatchIndex = index
                                showSuggestionDialog = true
                            }
                        }
                    }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (inputText.isBlank()) return@Button

                    coroutineScope.launch {
                        isLoading = true
                        correctedText = ""
                        summary = "Checking…"

                        val result = withContext(Dispatchers.IO) {
                            LanguageToolHelper.checkAndAutoCorrect(
                                text = inputText,
                                languageCode = "en-US",
                                autoFix = true
                            )
                        }

                        correctedText = result.correctedText
                        summary =
                            "${result.totalIssues} issues found • " +
                                    "${result.autoCorrected} auto-corrected • " +
                                    "${result.needsReview} needs review"

                        matches = result.matches
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                Text("Check Grammar")
            }

            if (isLoading) {
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Corrected Text",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Copy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    if (correctedText.isNotBlank()) {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Corrected Text", correctedText)
                        )
                    }
                }
            )
        }

        Text(
            text = correctedText,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }

    if (showSuggestionDialog && selectedMatchIndex != null) {
        val match = matches.getOrNull(selectedMatchIndex!!)
        if (match != null) {
            SuggestionDialog(
                match = match,
                onDismiss = { showSuggestionDialog = false },
                onSuggestionChosen = { suggestion ->
                    val from = match.offset.coerceAtLeast(0)
                    val to = (match.offset + match.length).coerceIn(0, inputText.length)
                    if (from in 0..to && to <= inputText.length) {
                        inputText = inputText.replaceRange(from, to, suggestion)
                    }
                    showSuggestionDialog = false
                }
            )
        }
    }
}

fun buildAnnotatedErrorText(
    text: String,
    matches: List<LanguageToolHelper.GrammarMatch>
): AnnotatedString {
    if (text.isEmpty() || matches.isEmpty()) return AnnotatedString(text)

    val builder = AnnotatedString.Builder()
    var currentIndex = 0
    val sorted = matches.sortedBy { it.offset }

    sorted.forEachIndexed { index, match ->
        val from = match.offset.coerceIn(0, text.length)
        val to = (match.offset + match.length).coerceIn(0, text.length)

        if (from > currentIndex) {
            builder.append(text.substring(currentIndex, from))
        }

        if (from < to) {
            val errorText = text.substring(from, to)
            val start = builder.length
            builder.append(errorText)
            val end = builder.length

            builder.addStyle(
                SpanStyle(
                    color = Color.Red,
                    textDecoration = TextDecoration.Underline
                ),
                start,
                end
            )

            builder.addStringAnnotation(
                tag = "ERROR",
                annotation = index.toString(),
                start = start,
                end = end
            )

            currentIndex = to
        }
    }

    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }

    return builder.toAnnotatedString()
}

@Composable
fun SuggestionDialog(
    match: LanguageToolHelper.GrammarMatch,
    onDismiss: () -> Unit,
    onSuggestionChosen: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix suggestion") },
        text = {
            Column {
                Text(
                    text = match.message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (match.replacements.isEmpty()) {
                    Text(
                        text = "No suggestions available.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    match.replacements.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionChosen(suggestion) }
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
