package com.soumanko.budgetwise.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding() // Automatically resize to remain above the keyboard
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
                if (isTyping) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Assistant is typing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Input is extracted to isolate recomposition!
            MessageInputRow(
                isTyping = isTyping,
                onSendMessage = { text -> viewModel.sendMessage(text) }
            )
        }
    }
}

@Composable
fun MessageInputRow(isTyping: Boolean, onSendMessage: (String) -> Unit) {
    var inputText by remember { mutableStateOf("") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Ask me anything...") },
            modifier = Modifier.weight(1f),
            singleLine = false, // Allow multiline
            maxLines = 4, // Up to 4 lines before scrolling inside the input
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                val textToSend = inputText
                inputText = ""
                onSendMessage(textToSend)
            },
            enabled = inputText.isNotBlank() && !isTyping,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (inputText.isNotBlank() && !isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, 
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send",
                tint = if (inputText.isNotBlank() && !isTyping) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = color,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = parseMarkdownToAnnotatedString(message.text),
                color = textColor,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Simple markdown parser to handle **bold** and *italic*
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // **Bold**
                text.startsWith("**", i) -> {
                    val endToken = text.indexOf("**", i + 2)
                    if (endToken != -1) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, endToken))
                        }
                        i = endToken + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // *Italic*
                text.startsWith("*", i) -> {
                    val endToken = text.indexOf("*", i + 1)
                    if (endToken != -1) {
                        withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, endToken))
                        }
                        i = endToken + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
