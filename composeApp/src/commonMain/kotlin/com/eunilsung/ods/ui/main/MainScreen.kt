package com.eunilsung.ods.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.Config
import com.eunilsung.ods.ui.login.LoginScreen
import com.eunilsung.ods.ui.main.tab.CurrentAppTab
import com.eunilsung.ods.ui.main.tab.MoreTab
import com.eunilsung.ods.ui.main.tab.TabItem
import com.eunilsung.ods.ui.main.tab.MarketTab
import com.eunilsung.ods.ui.main.tab.rememberAppTabState
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTheme
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.uikit.BackHandler
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.swipe.LocalSwipeDismiss
import com.eunilsung.ods.ui.uikit.swipe.SwipeDismissController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 앱의 뿌리 화면. 로그인 전에는 로그인 화면만 그린다(Talk 처럼 로그인 여부로 갈라 그린다).
 *
 * 탭 화면을 아래에 깔아 두지 않는다 — 가려진 화면이 눌리거나 읽히지 않고, 로그아웃하면 탭·스크롤 위치가 버려져
 * 다음 사람이 앞 사람의 화면을 이어받지 않는다. 오른쪽 스택은 로그아웃 때 비운다.
 */
object MainScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: MainViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.onAction(MainActions.OnForeground)
        }

        AppTheme {
            Navigator(EmptyScreen) { rightNavigator ->
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            MainScreenEvent.LoggedOut -> rightNavigator.popToEmptyRoot()
                        }
                    }
                }
                CompositionLocalProvider(LocalListDensity provides uiState.listDensity) {
                    AppHosts(rightNavigator = rightNavigator) {
                        if (uiState.isLoggedIn) {
                            MainContent()
                        } else {
                            LoginScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberImeHeight(): Dp {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    return with(density) { imeBottom.toDp() }
}

/**
 * 화면 폭으로 폰 · 태블릿(2칸) 레이아웃을 가른다.
 *
 * 기준은 기기 종류가 아니라 **지금 창의 폭**이다 — 폴더블을 펼치거나 태블릿을 돌리거나 분할 화면으로 줄이면 그 자리에서 바뀐다.
 * 오른쪽 화면 스택([LocalRightNavigator])은 모든 레이아웃이 같이 쓰므로 전환해도 보던 상세 화면이 유지된다.
 */
@Composable
fun MainContent() {
    val imeHeight = rememberImeHeight()
    val isKeyboardVisible = imeHeight > 0.dp
    val rightNavigator = LocalRightNavigator.current
    val rightPaneOpen = rightNavigator.lastItem !is EmptyScreen

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= Config.Layout.TABLET_MIN_WIDTH_DP.dp

        CompositionLocalProvider(
            LocalRightPaneOpen provides rightPaneOpen,
            LocalIsTablet provides isTablet,
        ) {
            when {
                isTablet -> Tablet(rightNavigator, isKeyboardVisible, imeHeight)
                else -> Phone(rightNavigator, isKeyboardVisible, imeHeight)
            }
        }
    }
}

@Composable
fun TabletLayout(main: @Composable () -> Unit, sub: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { main() }
        LineDivider(modifier = Modifier.fillMaxHeight(), isWidth = true)
        Box(modifier = Modifier.weight(1.4f).fillMaxHeight().clipToBounds()) { sub() }
    }
}

/** 태블릿 — 왼쪽은 탭 목록, 오른쪽은 고른 종목의 상세. 상세는 좌우로 밀려 바뀐다. */
@Composable
fun Tablet(
    rightNavigator: Navigator,
    isKeyboardVisible: Boolean,
    imeHeight: Dp,
) {
    TabletLayout(
        main = {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { LeftPane() }
                KeyBoardPane(
                    modifier = Modifier.background(AppColors.Bg),
                    isKeyboardVisible = isKeyboardVisible,
                    imeHeight = imeHeight,
                    fillIdleBottomInset = true,
                )
            }
        },
        sub = { RightPaneContent(rightNavigator) }
    )
}

/** 오른쪽 스택의 맨 위 화면. 쌓이면 오른쪽에서, 닫히면 왼쪽에서 밀려 들어온다. */
@Composable
private fun RightPaneContent(rightNavigator: Navigator) {
    AnimatedContent(
        targetState = rightNavigator.lastItem,
        transitionSpec = { stackTransition(rightNavigator.lastEvent) },
        label = "rightPane",
    ) { screen ->
        screen.Content()
    }
}

private fun stackTransition(event: StackEvent): ContentTransform {
    val (enter, exit) = if (event == StackEvent.Pop) {
        (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))) to
                (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
    } else {
        (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))) to
                (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
    }
    return enter togetherWith exit
}

/** 하단 탭과 탭 본문. */
@Composable
fun LeftPane() {
    val tabList = listOf(MarketTab, MoreTab)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val tabState = rememberAppTabState(tabList, MarketTab)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) {
                LineDivider(modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .height(Dimens.TabBarHeight)
                        .background(AppColors.Bg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (tab in tabList) {
                        TabItem(
                            tab = tab,
                            modifier = Modifier.weight(1f),
                            isSelected = tabState.current == tab,
                            onClick = { tabState.current = tab }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            CurrentAppTab(tabState)
        }
    }
}

private const val OverlayParallaxFraction = 0.3f

/**
 * 폰 — 탭 목록이 바닥에 깔리고, 상세 화면은 오른쪽에서 밀려 들어와 덮는다.
 *
 * 상세 위에 다른 화면이 쌓이면 뒤로 가기는 한 단계씩 돌아가고, 왼쪽에서 오른쪽으로 밀면 쌓인 화면을 모두 닫는다.
 * 덮개 안에서 쌓이거나 한 단계 돌아갈 때만 화면을 밀고, 덮개가 새로 열릴 때는 덮개 자체가 밀려 들어오므로 안쪽은 그대로 둔다.
 * 뒤 목록은 [OverlayParallaxFraction] 만큼 함께 움직인다.
 */
@Composable
fun Phone(
    rightNavigator: Navigator,
    isKeyboardVisible: Boolean,
    imeHeight: Dp,
) {
    val current = rightNavigator.lastItem
    val hasOverlay = current !is EmptyScreen
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    if (hasOverlay) {
        BackHandler { rightNavigator.popOrClose() }
    }

    var stickyOverlay by remember { mutableStateOf<Screen?>(null) }
    SideEffect {
        if (current !is EmptyScreen) stickyOverlay = current
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val swipeStartThresholdPx = with(density) { 60.dp.toPx() }

        val offsetX = remember { Animatable(screenWidthPx) }

        LaunchedEffect(hasOverlay, screenWidthPx) {
            val target = if (hasOverlay) 0f else screenWidthPx
            offsetX.animateTo(target, tween(durationMillis = 300))
        }

        var swipeStartOffset by remember { mutableStateOf(0f) }
        val swipeController = remember(screenWidthPx) {
            SwipeDismissController(
                onSwipeStart = { swipeStartOffset = offsetX.value },
                onSwipeProgress = { totalDx ->
                    scope.launch { offsetX.snapTo((swipeStartOffset + totalDx).coerceAtLeast(0f)) }
                },
                onSwipeEnd = { velocityX ->
                    val shouldDismiss = offsetX.value > screenWidthPx * 0.15f || velocityX > 1000f
                    scope.launch {
                        if (shouldDismiss) {
                            offsetX.animateTo(screenWidthPx, tween(durationMillis = 200))
                            rightNavigator.popToEmptyRoot()
                        } else {
                            offsetX.animateTo(0f, spring())
                        }
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    val bg = -OverlayParallaxFraction * (screenWidthPx - offsetX.value)
                    IntOffset(bg.roundToInt(), 0)
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { LeftPane() }
                KeyBoardPane(
                    modifier = Modifier.background(AppColors.Bg),
                    isKeyboardVisible = isKeyboardVisible,
                    imeHeight = imeHeight,
                    fillIdleBottomInset = true,
                )
            }
        }

        /** offsetX.value 를 직접 읽으면 슬라이드 동안 Phone 전체가 매 프레임 재구성된다. */
        val overlayVisible by remember(offsetX, screenWidthPx) {
            derivedStateOf { offsetX.value < screenWidthPx }
        }
        if (overlayVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(hasOverlay, screenWidthPx, swipeStartThresholdPx) {
                        if (!hasOverlay) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPointerInputChange(down)

                            var dragStarted = false
                            while (!dragStarted) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: return@awaitEachGesture
                                if (!change.pressed) return@awaitEachGesture
                                if (change.isConsumed) return@awaitEachGesture
                                velocityTracker.addPointerInputChange(change)
                                val dx = change.position.x - down.position.x
                                val dy = change.position.y - down.position.y
                                if (abs(dy) > swipeStartThresholdPx && abs(dy) > abs(dx)) return@awaitEachGesture
                                if (dx >= swipeStartThresholdPx) {
                                    dragStarted = true
                                    change.consume()
                                    scope.launch { offsetX.snapTo((offsetX.value + dx).coerceAtLeast(0f)) }
                                }
                            }

                            horizontalDrag(down.id) { change ->
                                velocityTracker.addPointerInputChange(change)
                                val delta = change.positionChange().x
                                scope.launch { offsetX.snapTo((offsetX.value + delta).coerceAtLeast(0f)) }
                                change.consume()
                            }

                            val velocityX = velocityTracker.calculateVelocity().x
                            val shouldDismiss = offsetX.value > screenWidthPx * 0.3f || velocityX > 1500f
                            scope.launch {
                                if (shouldDismiss) {
                                    offsetX.animateTo(screenWidthPx, tween(durationMillis = 200))
                                    rightNavigator.popToEmptyRoot()
                                } else {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        }
                    }
            ) {
                CompositionLocalProvider(LocalSwipeDismiss provides swipeController) {
                    AnimatedContent(
                        targetState = stickyOverlay,
                        transitionSpec = {
                            val withinOverlay = rightNavigator.size > 2 ||
                                (rightNavigator.lastEvent == StackEvent.Pop && rightNavigator.size == 2)
                            if (withinOverlay) stackTransition(rightNavigator.lastEvent)
                            else EnterTransition.None togetherWith ExitTransition.None
                        },
                        label = "phoneOverlay",
                    ) { screen ->
                        screen?.Content()
                    }
                }
            }
        }
    }
}
