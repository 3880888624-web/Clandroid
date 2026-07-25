package com.clawd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val BgColor = Color(0xFF0F0F11)
private val BubbleColor = Color(0xFF282930)
private val GlassBorder = Color(0x26FFFFFF)

data class Message(val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ChatScreen() }
    }
}

@Composable
fun ChatScreen() {
    val messages = remember { mutableStateListOf<Message>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg -> MessageBubble(msg) }
        }

        InputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    val userMsg = inputText.trim()
                    messages.add(Message(userMsg, true))
                    messages.add(Message("我明白了", false))
                    inputText = ""
                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun MessageBubble(msg: Message) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(BubbleColor.copy(alpha = 0.93f), BubbleColor.copy(alpha = 0.85f))
                    ),
                    shape
                )
                .border(0.5.dp, GlassBorder, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = msg.text, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                Brush.verticalGradient(
                    listOf(BubbleColor.copy(alpha = 0.93f), BubbleColor.copy(alpha = 0.85f))
                ),
                shape
            )
            .border(0.5.dp, GlassBorder, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text("消息", color = Color(0x66FFFFFF), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )

        AnimatedVisibility(
            visible = text.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .clickable { onSend() }
            ) {
                Text("↑", color = BgColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
