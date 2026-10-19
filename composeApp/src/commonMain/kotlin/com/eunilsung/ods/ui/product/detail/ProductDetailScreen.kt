package com.eunilsung.ods.ui.product.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.product.SuitabilityCard
import com.eunilsung.ods.ui.product.color
import com.eunilsung.ods.ui.product.label
import com.eunilsung.ods.ui.product.subscribe.SubscriptionScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.section.InfoRow
import com.eunilsung.ods.ui.uikit.section.SectionGap
import com.eunilsung.ods.ui.uikit.section.SectionHeader
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.product_detail
import ods.composeapp.generated.resources.product_expected_return
import ods.composeapp.generated.resources.product_fee
import ods.composeapp.generated.resources.product_key_points
import ods.composeapp.generated.resources.product_min_amount
import ods.composeapp.generated.resources.product_subscribe
import ods.composeapp.generated.resources.won
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 상품 상세 — 오른쪽 스택에 뜬다. 가입 진행을 누르면 위에 가입 절차가 쌓인다. */
data class ProductDetailScreen(val productId: String) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: ProductDetailViewModel = koinViewModel(key = "product-$productId")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(productId) { viewModel.onAction(ProductDetailActions.OnEnter(productId)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is ProductDetailEvent.OpenSubscription -> rightNavigator.push(SubscriptionScreen(event.productId, event.customerId))
                }
            }
        }

        ProductDetailContent(uiState, onAction = viewModel::onAction, onBack = { rightNavigator.popToEmptyRoot() })
    }
}

@Composable
fun ProductDetailContent(uiState: ProductDetailUiState, onAction: (ProductDetailActions) -> Unit, onBack: () -> Unit) {
    val product = uiState.product
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding()) {
        TopBar(title = product?.name ?: stringResource(Res.string.product_detail), onBack = onBack)
        LineDivider()
        if (product == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(modifier = Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                Text(text = product.name, style = AppTypography.Title, color = AppColors.Text)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge(text = product.type.label(), color = AppColors.TextSub)
                    Badge(text = product.riskGrade.label(), color = product.riskGrade.color)
                }
                Text(text = product.summary, style = AppTypography.Body, color = AppColors.Text)
            }
            InfoRow(stringResource(Res.string.product_expected_return), product.expectedReturn)
            InfoRow(stringResource(Res.string.product_fee), product.feeInfo)
            InfoRow(stringResource(Res.string.product_min_amount), stringResource(Res.string.won, product.minAmount.withComma()))
            SectionGap()
            SectionHeader(stringResource(Res.string.product_key_points))
            product.keyPoints.forEachIndexed { index, point ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp)) {
                    Text(text = "${index + 1}.", style = AppTypography.BodyNumber, color = AppColors.Caution, modifier = Modifier.padding(end = Dimens.Space8))
                    Text(text = point, style = AppTypography.Body, color = AppColors.Text)
                }
            }
            val customer = uiState.customer
            val suitability = uiState.suitability
            if (customer != null && suitability != null) {
                SuitabilityCard(customer, product, suitability, modifier = Modifier.padding(Dimens.ScreenPadding))
            }
        }
        LineDivider()
        PrimaryButton(
            text = stringResource(Res.string.product_subscribe),
            onClick = { onAction(ProductDetailActions.OnSubscribeClick) },
            modifier = Modifier.padding(Dimens.ScreenPadding).testTag(TestTags.PRODUCT_SUBSCRIBE),
        )
    }
}
