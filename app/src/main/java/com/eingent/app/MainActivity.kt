package com.eingent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean)

private enum class Screen { Welcome, Config, Chat }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EingentApp() }
    }
}

@Composable
fun EingentApp() {
    val context = LocalContext.current
    var screen by remember {
        mutableStateOf(
            if (ApiConfigStore.isComplete(context)) Screen.Chat else Screen.Welcome
        )
    }
    when (screen) {
        Screen.Welcome -> WelcomeScreen(onNext = { screen = Screen.Config })
        Screen.Config -> ApiConfigScreen(onComplete = { screen = Screen.Chat })
        Screen.Chat -> ChatScreen()
    }
}

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val config = remember { ApiConfigStore.load(context) }
    val messages = remember { mutableStateListOf<Message>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
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
                .padding(bottom = 88.dp)
                .bottomFade(72.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg -> MessageBubble(msg) }
        }

        InputBar(
            text = inputText,
            onTextChange = { inputText = it },
            enabled = !isLoading,
            onSend = {
                if (inputText.isNotBlank()) {
                    val userMsg = inputText.trim()
                    messages.add(Message(userMsg, true))
                    messages.add(Message("···", false))
                    val loadingIdx = messages.size - 1
                    inputText = ""
                    isLoading = true
                    scope.launch {
                        listState.animateScrollToItem(loadingIdx)
                        ClaudeApi.chat(config, messages.subList(0, loadingIdx).toList())
                            .onSuccess { messages[loadingIdx] = Message(it, false) }
                            .onFailure { messages[loadingIdx] = Message("错误: ${it.message}", false) }
                        isLoading = false
                        listState.animateScrollToItem(messages.size - 1)
                    }
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
                .glassBorder(18.dp)
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    val heightAnim = remember { Animatable(2f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(text.isNotEmpty()) {
        if (text.isNotEmpty()) {
            launch { heightAnim.animateTo(32f, tween(320, easing = FastOutSlowInEasing)) }
            launch { alphaAnim.animateTo(1f, tween(200)) }
        } else {
            launch { alphaAnim.animateTo(0f, tween(200)) }
            launch {
                delay(220)
                heightAnim.snapTo(2f)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                Brush.verticalGradient(
                    listOf(BubbleColor.copy(alpha = 0.82f), BubbleColor.copy(alpha = 0.97f))
                ),
                shape
            )
            .glassBorder(24.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = enabled,
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

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 10.dp)
                .width(32.dp)
                .height(heightAnim.value.dp)
                .alpha(alphaAnim.value)
                .background(Color.White, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = text.isNotEmpty() && enabled
                ) { onSend() }
        ) {
            if (alphaAnim.value > 0.6f && heightAnim.value > 24f) {
                Text("↑", color = BgColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
