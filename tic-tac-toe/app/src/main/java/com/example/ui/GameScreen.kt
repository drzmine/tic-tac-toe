package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameMatch
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val gameStatus by viewModel.gameStatus.collectAsState()
    val gameSetup by viewModel.gameSetup.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val matchHistory by viewModel.matchHistory.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFF10121B)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        AnimatedContent(
            targetState = gameStatus,
            transitionSpec = {
                if (targetState is GameStatus.Menu) {
                    slideInVertically { height -> -height } + fadeIn() with
                            slideOutVertically { height -> height } + fadeOut()
                } else {
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                }
            },
            label = "ScreenTransition"
        ) { status ->
            when (status) {
                is GameStatus.Menu -> {
                    MenuScreen(
                        matchHistory = matchHistory,
                        onStartGame = { setup -> viewModel.startNewGame(setup) },
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }
                is GameStatus.Active, is GameStatus.Win, is GameStatus.Draw -> {
                    BattleScreen(
                        board = board,
                        currentTurn = currentTurn,
                        gameStatus = status,
                        gameSetup = gameSetup,
                        isAiThinking = isAiThinking,
                        onCellClick = { index -> viewModel.makeMove(index) },
                        onRestart = { viewModel.startNewGame(gameSetup) },
                        onBackToMenu = { viewModel.goToMenu() }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuScreen(
    matchHistory: List<GameMatch>,
    onStartGame: (GameSetup) -> Unit,
    onClearHistory: () -> Unit
) {
    var isVsAi by remember { mutableStateOf(true) }
    var playerXName by remember { mutableStateOf("Player X") }
    var playerOName by remember { mutableStateOf("AI Bot") }
    var aiDifficulty by remember { mutableStateOf("Medium") }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Sync O name if single player vs multiplayer
    LaunchedEffect(isVsAi) {
        if (isVsAi) {
            playerOName = "AI Opponent"
        } else if (playerOName == "AI Opponent") {
            playerOName = "Player O"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Banner Image
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ImageFallback(
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "TIC TAC TOE",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "SLEEK EDITION",
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Mode Selector Tab
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isVsAi) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = if (isVsAi) 1.dp else 0.dp,
                            color = if (isVsAi) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isVsAi = true }
                        .padding(vertical = 12.dp)
                        .testTag("mode_single_player_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI Mode",
                            tint = if (isVsAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "vs AI Bot",
                            fontWeight = FontWeight.Bold,
                            color = if (isVsAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isVsAi) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = if (!isVsAi) 1.dp else 0.dp,
                            color = if (!isVsAi) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isVsAi = false }
                        .padding(vertical = 12.dp)
                        .testTag("mode_pass_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Pass & Play Mode",
                            tint = if (!isVsAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pass & Play",
                            fontWeight = FontWeight.Bold,
                            color = if (!isVsAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Inputs Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PLAYER SETUP",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = playerXName,
                        onValueChange = { playerXName = it },
                        label = { Text("Player X Name") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PlayerXColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlayerXColor,
                            focusedLabelColor = PlayerXColor,
                            cursorColor = PlayerXColor
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("player_x_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = playerOName,
                        onValueChange = { if (!isVsAi) playerOName = it },
                        label = { Text(if (isVsAi) "AI Opponent" else "Player O Name") },
                        enabled = !isVsAi,
                        leadingIcon = {
                            Icon(
                                imageVector = if (isVsAi) Icons.Default.SmartToy else Icons.Default.Person,
                                contentDescription = null,
                                tint = PlayerOColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlayerOColor,
                            focusedLabelColor = PlayerOColor,
                            cursorColor = PlayerOColor
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("player_o_input"),
                        singleLine = true
                    )

                    if (isVsAi) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI DIFFICULTY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val difficulties = listOf("Easy", "Medium", "Hard")
                            val difficultyColors = listOf(Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFE57373))

                            difficulties.forEachIndexed { idx, diff ->
                                val isSelected = aiDifficulty == diff
                                val diffColor = difficultyColors[idx]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) diffColor.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isSelected) diffColor else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { aiDifficulty = diff }
                                        .padding(vertical = 12.dp)
                                        .testTag("diff_$diff"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) diffColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Start Button
        item {
            val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.03f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )

            Button(
                onClick = {
                    onStartGame(
                        GameSetup(
                            playerXName = playerXName.trim().ifEmpty { "Player X" },
                            playerOName = if (isVsAi) "AI Bot ($aiDifficulty)" else playerOName.trim().ifEmpty { "Player O" },
                            isVsAi = isVsAi,
                            aiDifficulty = aiDifficulty
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = MaterialTheme.colorScheme.primary, spotColor = MaterialTheme.colorScheme.primary)
                    .testTag("start_battle_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "START THE BATTLE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
            }
        }

        // Stats Display
        if (matchHistory.isNotEmpty()) {
            item {
                StatsPanel(matchHistory)
            }
        }

        // History Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH HISTORY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (matchHistory.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }
        }

        if (matchHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No games played yet.",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Start a battle above to build your stats!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(matchHistory.take(10)) { match ->
                MatchHistoryCard(match)
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Match History?") },
            text = { Text("This will permanently delete your match history and stats. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatsPanel(matchHistory: List<GameMatch>) {
    val total = matchHistory.size
    val winsX = matchHistory.count { it.winner != "Draw" && it.winner == it.playerXName }
    val winsO = matchHistory.count { it.winner != "Draw" && it.winner == it.playerOName }
    val draws = matchHistory.count { it.winner == "Draw" }

    val rateX = if (total > 0) (winsX * 100f / total) else 0f
    val rateO = if (total > 0) (winsO * 100f / total) else 0f
    val rateDraw = if (total > 0) (draws * 100f / total) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), Color.Transparent)
                ),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "BATTLE METRICS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            // Canvas segmented progress bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val barWidth = size.width
                val barHeight = size.height

                if (total == 0) {
                    drawRect(color = Color.Gray, size = Size(barWidth, barHeight))
                } else {
                    val widthX = barWidth * (winsX.toFloat() / total)
                    val widthO = barWidth * (winsO.toFloat() / total)
                    val widthDraw = barWidth * (draws.toFloat() / total)

                    // Draw X section (Cyan)
                    drawRect(color = PlayerXColor, size = Size(widthX, barHeight))
                    // Draw Draw section (Grey)
                    drawRect(
                        color = DrawColor,
                        topLeft = Offset(widthX, 0f),
                        size = Size(widthDraw, barHeight)
                    )
                    // Draw O section (Pink)
                    drawRect(
                        color = PlayerOColor,
                        topLeft = Offset(widthX + widthDraw, 0f),
                        size = Size(widthO, barHeight)
                    )
                }
            }

            // Segment labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // X Wins
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PlayerXColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("X Wins", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "$winsX (%.1f%%)".format(rateX),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayerXColor
                    )
                }

                // Draws
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DrawColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Draws", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "$draws (%.1f%%)".format(rateDraw),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = DrawColor
                    )
                }

                // O Wins
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PlayerOColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("O Wins", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "$winsO (%.1f%%)".format(rateO),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayerOColor
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Battles", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text("$total", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                val avgMoves = if (total > 0) matchHistory.map { it.moveCount }.average() else 0.0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg Moves", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text("%.1f".format(avgMoves), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                val avgTime = if (total > 0) matchHistory.map { it.durationSeconds }.average() else 0.0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg Match Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text("${avgTime.toInt()}s", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MatchHistoryCard(match: GameMatch) {
    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(match.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = match.playerXName,
                        fontWeight = FontWeight.Bold,
                        color = PlayerXColor,
                        fontSize = 14.sp
                    )
                    Text("vs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Text(
                        text = match.playerOName,
                        fontWeight = FontWeight.Bold,
                        color = PlayerOColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    )
                    Text(
                        text = "${match.moveCount} moves",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    )
                    Text(
                        text = "${match.durationSeconds}s",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }

            // Winner badge
            val winnerColor = when (match.winner) {
                match.playerXName -> PlayerXColor
                match.playerOName -> PlayerOColor
                else -> DrawColor
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(winnerColor.copy(alpha = 0.15f))
                    .border(1.dp, winnerColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (match.winner == "Draw") "DRAW" else "WINNER",
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    color = winnerColor,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun BattleScreen(
    board: List<String?>,
    currentTurn: String,
    gameStatus: GameStatus,
    gameSetup: GameSetup,
    isAiThinking: Boolean,
    onCellClick: (Int) -> Unit,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper row: Menu and Game setup info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("back_to_menu_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BATTLE ARENA",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (gameSetup.isVsAi) {
                    Text(
                        text = "AI Level: ${gameSetup.aiDifficulty}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onRestart,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("restart_game_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Mid-upper row: Players details card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player X Name Indicator
            val activeX = currentTurn == "X" && gameStatus is GameStatus.Active
            PlayerIndicator(
                name = gameSetup.playerXName,
                symbol = "X",
                color = PlayerXColor,
                isActive = activeX
            )

            // VS Text
            Text(
                text = "VS",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )

            // Player O Name Indicator
            val activeO = currentTurn == "O" && gameStatus is GameStatus.Active
            PlayerIndicator(
                name = gameSetup.playerOName,
                symbol = "O",
                color = PlayerOColor,
                isActive = activeO,
                isThinking = activeO && isAiThinking
            )
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        // Main Board Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw grid lines in the background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridWidth = size.width
                val gridHeight = size.height
                val cellWidth = gridWidth / 3f
                val cellHeight = gridHeight / 3f

                val brush = Brush.linearGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.08f), secondaryColor.copy(alpha = 0.08f))
                )

                // Grid background glow box
                drawRoundRect(
                    brush = brush,
                    size = size,
                    cornerRadius = CornerRadius(24f, 24f)
                )

                // Grid Lines
                val lineStroke = 6f
                val lineBrush = Brush.linearGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.3f), tertiaryColor.copy(alpha = 0.3f))
                )

                // Vertical Lines
                drawLine(
                    brush = lineBrush,
                    start = Offset(cellWidth, 24f),
                    end = Offset(cellWidth, gridHeight - 24f),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = lineBrush,
                    start = Offset(cellWidth * 2f, 24f),
                    end = Offset(cellWidth * 2f, gridHeight - 24f),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )

                // Horizontal Lines
                drawLine(
                    brush = lineBrush,
                    start = Offset(24f, cellHeight),
                    end = Offset(gridWidth - 24f, cellHeight),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = lineBrush,
                    start = Offset(24f, cellHeight * 2f),
                    end = Offset(gridWidth - 24f, cellHeight * 2f),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )
            }

            // Cell contents & clickable buttons
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val cellValue = board[index]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onCellClick(index)
                                    }
                                    .testTag("grid_cell_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = cellValue != null,
                                    enter = fadeIn(animationSpec = spring()) + slideInVertically { -30 },
                                    exit = fadeOut()
                                ) {
                                    if (cellValue != null) {
                                        SymbolRenderer(
                                            symbol = cellValue,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Draw Victory Connection Line if GameStatus is Win
            if (gameStatus is GameStatus.Win) {
                val winCells = gameStatus.cells
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellWidth = size.width / 3f
                    val cellHeight = size.height / 3f

                    // Find coordinates of start and end cells
                    val startIdx = winCells.first()
                    val endIdx = winCells.last()

                    val startX = (startIdx % 3) * cellWidth + cellWidth / 2f
                    val startY = (startIdx / 3) * cellHeight + cellHeight / 2f

                    val endX = (endIdx % 3) * cellWidth + cellWidth / 2f
                    val endY = (endIdx / 3) * cellHeight + cellHeight / 2f

                    drawLine(
                        color = WinLineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 12f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                }
            }
        }

        // Bottom row: Interactive Game state dialog or status report
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            when (gameStatus) {
                is GameStatus.Active -> {
                    val statusText = if (currentTurn == "O" && gameSetup.isVsAi) {
                        "AI is formulating move..."
                    } else {
                        "It's $currentTurn's turn"
                    }
                    val textColor = if (currentTurn == "X") PlayerXColor else PlayerOColor

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        ),
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
                is GameStatus.Win -> {
                    val isPlayerXWin = gameStatus.winner == "X"
                    val winMessage = when {
                        gameSetup.isVsAi && !isPlayerXWin -> "Defeat! The AI has conquered."
                        gameSetup.isVsAi && isPlayerXWin -> "Victory! You beat the AI."
                        else -> "Victory! ${if (isPlayerXWin) gameSetup.playerXName else gameSetup.playerOName} Wins!"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = winMessage,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = if (isPlayerXWin) PlayerXColor else PlayerOColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("win_message")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onRestart,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Again", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = onBackToMenu,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Menu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                is GameStatus.Draw -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "It's a Stalemate!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = DrawColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("draw_message")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onRestart,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Again", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = onBackToMenu,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Menu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PlayerIndicator(
    name: String,
    symbol: String,
    color: Color,
    isActive: Boolean,
    isThinking: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 0.95f,
        animationSpec = spring(),
        label = "IndicatorScale"
    )

    val borderGlow by animateColorAsState(
        targetValue = if (isActive) color else Color.Transparent,
        animationSpec = tween(500),
        label = "Glow"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(
                elevation = if (isActive) 12.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = color,
                spotColor = color
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
            )
            .border(
                width = 1.5.dp,
                color = borderGlow.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = symbol,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            if (isThinking) {
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = color,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun SymbolRenderer(symbol: String, modifier: Modifier = Modifier) {
    val symbolColor = if (symbol == "X") PlayerXColor else PlayerOColor

    Canvas(modifier = modifier) {
        val sizeVal = size.minDimension
        val strokeWidth = sizeVal * 0.16f

        if (symbol == "X") {
            // Draw beautiful glowing dynamic X
            val pad = sizeVal * 0.15f
            drawLine(
                color = symbolColor,
                start = Offset(pad, pad),
                end = Offset(sizeVal - pad, sizeVal - pad),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = symbolColor,
                start = Offset(sizeVal - pad, pad),
                end = Offset(pad, sizeVal - pad),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        } else {
            // Draw beautiful glowing dynamic O
            val radius = (sizeVal - strokeWidth) / 2f * 0.8f
            drawCircle(
                color = symbolColor,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun ImageFallback(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hasImage = remember {
        val resId = context.resources.getIdentifier("tictactoe_hero_banner", "drawable", context.packageName)
        resId != 0
    }

    if (hasImage) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.tictactoe_hero_banner),
            contentDescription = "Tic Tac Toe Banner",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback stylish mesh drawing on Canvas
        Canvas(modifier = modifier) {
            val width = size.width
            val height = size.height

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            )

            // Draw technical grid background lines
            val numLines = 15
            val stepX = width / numLines
            val stepY = height / numLines

            for (i in 0..numLines) {
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(i * stepX, 0f),
                    end = Offset(i * stepX, height),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, i * stepY),
                    end = Offset(width, i * stepY),
                    strokeWidth = 1f
                )
            }
        }
    }
}
