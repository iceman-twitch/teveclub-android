package com.iceman.teveclub.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

        // === TOP STATUS BAR with progress bars ===
        status.value?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Food progress bar
                FeedProgressBar(
                    label = "Kaja",
                    count = s.foodCount,
                    max = s.foodMax,
                    percent = s.foodPercent,
                    color = Color(0xFFFF8A65),
                    iconUrl = s.foodImageUrl,
                    imageLoader = imageLoader
                )

                // Drink progress bar
                FeedProgressBar(
                    label = "Ital",
                    count = s.drinkCount,
                    max = s.drinkMax,
                    percent = s.drinkPercent,
                    color = Color(0xFF4FC3F7),
                    iconUrl = s.drinkImageUrl,
                    imageLoader = imageLoader
                )

                // Status text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (s.canFeed) "Éhes!" else "Tevéd jóllakott",
                        color = if (s.canFeed) Color(0xFFFFD54F) else Color(0xFF81C784),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    s.trick?.let { trick ->
                        Text(
                            trick,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
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
                                        actionMessage = "Tanulj teve! (nincs választható lecke)"
                                    }
                                    is TeveApiRepository.LearnPageState.AlreadyLearnedAll -> {
                                        actionMessage = "Már mindent megtanult!"
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
                title = "Kaja beállítása",
                onDismiss = { showFoodPicker = false }
            ) {
                FOOD_LIST.forEach { food ->
                    PickerRow(emoji = food.emoji, name = food.name) {
                        vm.setFood(food.id) { ok, msg2 ->
                            actionMessage = if (ok) "${food.name} beállítva!" else msg2
                        }
                        showFoodPicker = false
                    }
                }
            }
        }

        // === DRINK PICKER DIALOG ===
        if (showDrinkPicker) {
            PickerDialog(
                title = "Ital beállítása",
                onDismiss = { showDrinkPicker = false }
            ) {
                DRINK_LIST.forEach { drink ->
                    PickerRow(emoji = drink.emoji, name = drink.name) {
                        vm.setDrink(drink.id) { ok, msg2 ->
                            actionMessage = if (ok) "${drink.name} beállítva!" else msg2
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
                title = { Text("Számjáték", fontWeight = FontWeight.Bold) },
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
                                    guessResult = msg2
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
            var selectedLesson by remember { mutableStateOf<TeveApiRepository.LearnOption?>(null) }
            AlertDialog(
                onDismissRequest = { showLearnDialog = false },
                title = { Text("Tanítás", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        learnOptions.forEach { option ->
                            val isSelected = selectedLesson == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) TeveColors.ButtonBlue.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TeveColors.ButtonBlue else Color.LightGray,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedLesson = option }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLesson = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = TeveColors.ButtonBlue
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    option.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = TeveColors.BodyText
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedLesson?.let { lesson ->
                                vm.submitLearn(lesson.value) { _, _ ->
                                    // silently reload status, no message
                                }
                                showLearnDialog = false
                            }
                        },
                        enabled = selectedLesson != null && !isLoading.value,
                        colors = ButtonDefaults.buttonColors(backgroundColor = TeveColors.ButtonBlue)
                    ) {
                        Text("Tanítás!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLearnDialog = false }) {
                        Text("Mégse")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
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
fun FeedProgressBar(
    label: String,
    count: Int,
    max: Int,
    percent: Int,
    color: Color,
    iconUrl: String?,
    imageLoader: ImageLoader
) {
    val progress = percent.toFloat() / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Food/drink icon from teveclub
        if (iconUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconUrl)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = label,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Text(
                if (label == "Kaja") "🍖" else "💧",
                fontSize = 18.sp
            )
            Spacer(Modifier.width(8.dp))
        }

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
            // Count text centered on bar
            Text(
                "$count / $max",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Percentage
        Text(
            "$percent%",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
fun StatusMessageBox(message: String) {
    Text(
        message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(12.dp),
        color = Color.White,
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}
