package com.eunilsung.ods.ui.product.list

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.ProductType
import com.eunilsung.ods.ui.main.LocalIsTablet
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.product.color
import com.eunilsung.ods.ui.product.detail.ProductDetailScreen
import com.eunilsung.ods.ui.product.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.product_all
import ods.composeapp.generated.resources.tab_product
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 상품 탭 — 판매 상품 목록. 누르면 오른쪽 스택에 상세를 연다. */
object ProductListScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ProductListViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val openedId = (rightNavigator.items.lastOrNull { it is ProductDetailScreen } as? ProductDetailScreen)?.productId

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is ProductListEvent.OpenDetail -> {
                        rightNavigator.popToEmptyRoot()
                        rightNavigator.push(ProductDetailScreen(event.id))
                    }
                }
            }
        }

        ProductListContent(uiState, selectedId = if (LocalIsTablet.current) openedId else null, onAction = viewModel::onAction)
    }
}

@Composable
fun ProductListContent(uiState: ProductListUiState, selectedId: String?, onAction: (ProductListActions) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = stringResource(Res.string.tab_product))
        SegmentedControl(
            options = listOf<ProductType?>(null) + uiState.types,
            selected = uiState.typeFilter,
            label = { it?.label() ?: stringResource(Res.string.product_all) },
            onSelect = { onAction(ProductListActions.OnTypeFilter(it)) },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8).horizontalScroll(rememberScrollState()),
        )
        LineDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.products, key = { it.id }) { product ->
                ProductItem(product, isSelected = product.id == selectedId) { onAction(ProductListActions.OnProductClick(product.id)) }
                LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
            }
        }
    }
}

/** 상품 한 줄 — 이름, 종류·위험등급, 기대수익. */
@Composable
private fun ProductItem(product: Product, isSelected: Boolean, onClick: () -> Unit) {
    val tokens = LocalListDensity.current.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.PRODUCT_ITEM)
            .background(if (isSelected) AppColors.PrimaryBg else AppColors.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = tokens.rowVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = product.name, style = tokens.nameStyle, color = AppColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(text = product.type.label(), color = AppColors.TextSub)
            Badge(text = product.riskGrade.label(), color = product.riskGrade.color)
            Text(text = product.expectedReturn, style = tokens.subStyle, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
