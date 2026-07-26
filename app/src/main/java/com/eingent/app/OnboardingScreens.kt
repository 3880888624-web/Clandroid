package com.eingent.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    val headlineOffsetY = remember { Animatable(80f) }
    val headlineAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(80f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }
    val buttonOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        launch { headlineOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        launch { headlineAlpha.animateTo(1f, tween(500)) }
        delay(120)
        launch { subtitleOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        launch { subtitleAlpha.animateTo(1f, tween(500)) }
        delay(500)
        launch { buttonAlpha.animateTo(1f, tween(400)) }
        launch { buttonOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = "你好，我们是 Eingent",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(headlineAlpha.value)
                    .offset { IntOffset(0, headlineOffsetY.value.roundToInt()) }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "我们想要做最棒的安卓端 agent 生态，感谢您的加入！",
                color = Color(0xFF888899),
                fontSize = 14.sp,
                modifier = Modifier
                    .alpha(subtitleAlpha.value)
                    .offset { IntOffset(0, subtitleOffsetY.value.roundToInt()) }
            )
        }

        OnboardingButton(
            text = "下一步",
            alpha = buttonAlpha.value,
            offsetY = buttonOffsetY.value,
            onClick = onNext,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun ApiConfigScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val headlineOffsetY = remember { Animatable(80f) }
    val headlineAlpha = remember { Animatable(0f) }
    val doneAlpha = remember { Animatable(0f) }
    val doneOffsetY = remember { Animatable(20f) }

    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { headlineOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        launch { headlineAlpha.animateTo(1f, tween(500)) }
    }

    LaunchedEffect(showDone) {
        if (showDone) {
            launch { doneAlpha.animateTo(1f, tween(400)) }
            launch { doneOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "请您配置您的 API",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(headlineAlpha.value)
                    .offset { IntOffset(0, headlineOffsetY.value.roundToInt()) }
            )
            Spacer(Modifier.height(48.dp))
            ConfigField(value = baseUrl, onValueChange = { baseUrl = it; errorMsg = null; showDone = false }, placeholder = "Base URL（如 https://api.anthropic.com）")
            Spacer(Modifier.height(12.dp))
            ConfigField(value = apiKey, onValueChange = { apiKey = it; errorMsg = null; showDone = false }, placeholder = "API Key", isPassword = true)
            Spacer(Modifier.height(12.dp))
            ConfigField(value = model, onValueChange = { model = it; errorMsg = null; showDone = false }, placeholder = "模型名称（如 claude-haiku-4-5-20251001）")
            Spacer(Modifier.height(20.dp))
            val canTest = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank() && !isTesting
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(listOf(BubbleColor.copy(0.93f), BubbleColor.copy(0.85f))),
                        RoundedCornerShape(14.dp)
                    )
                    .glassBorder(14.dp)
                    .clickable(
                        enabled = canTest,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isTesting = true
                        errorMsg = null
                        scope.launch {
                            val config = ApiConfig(baseUrl.trim(), apiKey.trim(), model.trim())
                            ClaudeApi.testConnection(config)
                                .onSuccess {
                                    ApiConfigStore.save(context, config)
                                    showDone = true
                                }
                                .onFailure { errorMsg = it.message }
                            isTesting = false
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTesting) "连接中…" else "测试连接",
                    color = if (canTest) Color.White else Color(0xFF555566),
                    fontSize = 15.sp
                )
            }
            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
                Text(text = errorMsg!!, color = Color(0xFFFF5555), fontSize = 13.sp)
            }
        }

        if (showDone) {
            OnboardingButton(
                text = "完成",
                alpha = doneAlpha.value,
                offsetY = doneOffsetY.value,
                onClick = onComplete,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun ConfigField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
        cursorBrush = SolidColor(Color.White),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(BubbleColor.copy(0.93f), BubbleColor.copy(0.85f))),
                RoundedCornerShape(14.dp)
            )
            .glassBorder(14.dp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = Color(0x55FFFFFF), fontSize = 15.sp)
                innerTextField()
            }
        }
    )
}

@Composable
fun OnboardingButton(
    text: String,
    alpha: Float,
    offsetY: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(alpha)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .background(Color.White, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(text = text, color = BgColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
