package com.iceman.teveclub.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.iceman.teveclub.TeveApiRepository
import com.iceman.teveclub.TeveViewModel

data class FoodItem(val id: String, val name: String, val emoji: String)
data class DrinkItem(val id: String, val name: String, val emoji: String)

val FOOD_LIST = listOf(
    FoodItem("0", "Széna", "🌾"),
    FoodItem("1", "Hamburger", "🍔"),
    FoodItem("9", "Csoki", "🍫"),
    FoodItem("10", "Gomba", "🍄"),
    FoodItem("12", "Szaloncukor", "🍬")
)

val DRINK_LIST = listOf(
    DrinkItem("0", "Víz", "💧"),
    DrinkItem("1", "Kóla", "🥤"),
    DrinkItem("8", "Pezsgő", "🍾"),
    DrinkItem("9", "Banánturmix", "🍌"),
    DrinkItem("21", "Cherry Coke", "🍒")
)

@Composable
fun DashboardScreen(vm: TeveViewModel) {
    val status = vm.camelStatus.collectAsState()
    val statusMsg = vm.statusMessage.collectAsState()
    val isLoading = vm.isLoading.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var showDrinkPicker by remember { mutableStateOf(false) }
    var guessInput by remember { mutableStateOf("") }
    var showGuessDialog by remember { mutableStateOf(false) }
    var guessResult by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var showLearnDialog by remember { mutableStateOf(false) }
    var learnOptions by remember { mutableStateOf<List<TeveApiRepository.LearnOption>>(emptyList()) }

    val context = LocalContext.current

    // GIF-capable image loader
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    LaunchedEffect(Unit) { vm.loadCamelStatus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TeveColors.Background)
    ) {
        // === FULL-SCREEN PET IMAGE/GIF ===
        val petUrl = status.value?.petImageUrl
        if (petUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(petUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Tevéd",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // Placeholder while loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        color = TeveColors.ButtonBlue,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                } else {
                    Text(
                        "🐪",
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // === TOP STATUS BAR (overlay) ===
        status.value?.let { s ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: status info
                    Column {
                        s.feedCountText?.let {
                            Text(
                                "🍖 $it",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (s.canFeed) {
                            Text(
                                "⚠️ Éhes!",
                                color = Color(0xFFFFD54F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "✅ Jóllakott",
                                color = Color(0xFF81C784),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Right side: trick info
                    s.trick?.let { trick ->
                        Text(
                            "🎓 $trick",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // === STATUS MESSAGE (bottom overlay) ===
        val msg = statusMsg.value ?: actionMessage
        if (msg != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 32.dp, end = 32.dp)
            ) {
                StatusMessageBox(msg)
            }
        }

        // === LOADING INDICATOR ===
        if (isLoading.value) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
                strokeWidth = 4.dp
            )
        }

        // === FAB MENU (bottom-right corner) ===
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Expandable action buttons
            AnimatedVisibility(
                visible = showMenu,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Logout
                    MiniFab(
                        emoji = "🚪",
                        label = "Kilépés",
                        onClick = {
                            showMenu = false
                            vm.logout { _, _ -> }
                        }
                    )

                    // Guess game
                    MiniFab(
                        emoji = "🎲",
                        label = "Számjáték",
                        onClick = {
                            showMenu = false
                            showGuessDialog = true
                        }
                    )

                    // Learn
                    MiniFab(
                        emoji = "📚",
                        label = "Tanítás",
                        onClick = {
                            showMenu = false
                            vm.loadLearnPage { state ->
                                when (state) {
                                    is TeveApiRepository.LearnPageState.HasOptions -> {
                                        learnOptions = state.options
                                        showLearnDialog = true
                                    }
                                    is TeveApiRepository.LearnPageState.NoOptionsButCanLearn -> {
                                        actionMessage = "📚 Tanulj teve! (nincs választható lecke)"
                                    }
                                    is TeveApiRepository.LearnPageState.AlreadyLearnedAll -> {
                                        actionMessage = "🎓 Már mindent megtanult!"
                                    }
                                    null -> {}
                                }
                            }
                        }
                    )

                    // Set drink
                    MiniFab(
                        emoji = "🥤",
                        label = "Ital",
                        onClick = {
                            showMenu = false
                            showDrinkPicker = true
                        }
                    )

                    // Set food
                    MiniFab(
                        emoji = "🥘",
                        label = "Kaja",
                        onClick = {
                            showMenu = false
                            showFoodPicker = true
                        }
                    )

                    // Feed
                    MiniFab(
                        emoji = "🍖",
                        label = "Etet",
                        onClick = {
                            showMenu = false
                            actionMessage = null
                            vm.feedPet()
                        },
                        highlight = status.value?.canFeed == true
                    )

                    // Refresh
                    MiniFab(
                        emoji = "🔄",
                        label = "Frissít",
                        onClick = {
                            showMenu = false
                            vm.loadCamelStatus()
                        }
                    )
                }
            }

            // Main FAB
            FloatingActionButton(
                onClick = { showMenu = !showMenu },
                backgroundColor = TeveColors.ButtonBlue,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (showMenu) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menü",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // === FOOD PICKER DIALOG ===
        if (showFoodPicker) {
            PickerDialog(
                title = "🥘 Kaja beállítása",
                onDismiss = { showFoodPicker = false }
            ) {
                FOOD_LIST.forEach { food ->
                    PickerRow(emoji = food.emoji, name = food.name) {
                        vm.setFood(food.id) { ok, msg2 ->
                            actionMessage = if (ok) "✅ ${food.name} beállítva!" else "❌ $msg2"
                        }
                        showFoodPicker = false
                    }
                }
            }
        }

        // === DRINK PICKER DIALOG ===
        if (showDrinkPicker) {
            PickerDialog(
                title = "🥤 Ital beállítása",
                onDismiss = { showDrinkPicker = false }
            ) {
                DRINK_LIST.forEach { drink ->
                    PickerRow(emoji = drink.emoji, name = drink.name) {
                        vm.setDrink(drink.id) { ok, msg2 ->
                            actionMessage = if (ok) "✅ ${drink.name} beállítva!" else "❌ $msg2"
                        }
                        showDrinkPicker = false
                    }
                }
            }
        }

        // === GUESS GAME DIALOG ===
        if (showGuessDialog) {
            AlertDialog(
                onDismissRequest = { showGuessDialog = false },
                title = { Text("🎲 Számjáték", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = guessInput,
                            onValueChange = { guessInput = it.filter { c -> c.isDigit() } },
                            placeholder = { Text("Szám (pl. 403)") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        guessResult?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, fontSize = 14.sp, color = TeveColors.BodyText)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (guessInput.isNotEmpty()) {
                                vm.guessNumber(guessInput) { ok, msg2 ->
                                    guessResult = if (ok) "✅ $msg2" else "❌ $msg2"
                                }
                            }
                        },
                        enabled = !isLoading.value && guessInput.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = TeveColors.ButtonBlue)
                    ) {
                        Text("Tipp!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGuessDialog = false; guessResult = null }) {
                        Text("Bezár")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // === LEARN PICKER DIALOG ===
        if (showLearnDialog && learnOptions.isNotEmpty()) {
            PickerDialog(
                title = "📚 Tanítás választás",
                onDismiss = { showLearnDialog = false }
            ) {
                learnOptions.forEach { option ->
                    PickerRow(emoji = "🎓", name = option.name) {
                        vm.submitLearn(option.value) { ok, msg2 ->
                            actionMessage = if (ok) "✅ $msg2" else "❌ $msg2"
                        }
                        showLearnDialog = false
                    }
                }
            }
        }
    }
}

@Composable
fun MiniFab(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Label chip
        Text(
            label,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = TeveColors.BodyText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        // Mini FAB
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            backgroundColor = if (highlight) Color(0xFFFF7043) else TeveColors.PanelBackground,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
        }
    }
}

@Composable
fun MiniFabWithImage(
    imageUrl: String,
    imageLoader: ImageLoader,
    label: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            label,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = TeveColors.BodyText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            backgroundColor = TeveColors.PanelBackground,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Trükk",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun PickerDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PickerRow(emoji: String, name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            color = TeveColors.BodyText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusMessageBox(message: String) {
    val bgColor = when {
        message.startsWith("✅") -> Color(0xFFD4EDDA)
        message.startsWith("❌") -> Color(0xFFF8D7DA)
        else -> Color(0xFFD1ECF1)
    }
    val textColor = when {
        message.startsWith("✅") -> Color(0xFF155724)
        message.startsWith("❌") -> Color(0xFF721C24)
        else -> Color(0xFF0C5460)
    }
    Text(
        message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor.copy(alpha = 0.95f))
            .padding(12.dp),
        color = textColor,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}
