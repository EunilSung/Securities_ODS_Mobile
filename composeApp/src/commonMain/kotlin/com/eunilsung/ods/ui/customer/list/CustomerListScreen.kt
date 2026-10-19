package com.eunilsung.ods.ui.customer.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.ui.customer.detail.CustomerDetailScreen
import com.eunilsung.ods.ui.customer.register.NewCustomerScreen
import com.eunilsung.ods.ui.main.LocalIsTablet
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.textfield.SearchField
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.customer.list.item.CustomerItem
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.add_icon
import ods.composeapp.generated.resources.customer_count
import ods.composeapp.generated.resources.customer_empty
import ods.composeapp.generated.resources.customer_new
import ods.composeapp.generated.resources.customer_search_hint
import ods.composeapp.generated.resources.tab_customer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 고객 탭 — 담당 고객 목록. 고객을 누르면 오른쪽 스택에 상세를 연다. */
object CustomerListScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: CustomerListViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val isTablet = LocalIsTablet.current
        val openedId = (rightNavigator.items.lastOrNull { it is CustomerDetailScreen } as? CustomerDetailScreen)?.customerId

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is CustomerListEvent.OpenDetail -> rightNavigator.openCustomerDetail(event.id)
                    CustomerListEvent.OpenNewCustomer -> {
                        rightNavigator.popToEmptyRoot()
                        rightNavigator.push(NewCustomerScreen())
                    }
                }
            }
        }

        CustomerListContent(
            uiState = uiState,
            selectedId = if (isTablet) openedId else null,
            onAction = viewModel::onAction,
        )
    }
}

/** 오른쪽 스택을 비우고 고객 상세를 연다. 이미 그 고객이 열려 있으면 그대로 둔다. */
fun Navigator.openCustomerDetail(customerId: String) {
    val top = lastItem
    if (top is CustomerDetailScreen && top.customerId == customerId) return
    popToEmptyRoot()
    push(CustomerDetailScreen(customerId))
}

/**
 * 고객 목록 전체 — 상태 없이 값과 콜백만 받는다.
 *
 * @param selectedId 태블릿에서 오른쪽에 열린 고객. 폰에서는 null.
 */
@Composable
fun CustomerListContent(
    uiState: CustomerListUiState,
    selectedId: String?,
    onAction: (CustomerListActions) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(Res.string.tab_customer),
            actions = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag(TestTags.CUSTOMER_NEW)
                        .clickable(cornerRadius = 20.dp, onClick = { onAction(CustomerListActions.OnNewCustomerClick) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add_icon),
                        contentDescription = stringResource(Res.string.customer_new),
                        tint = AppColors.Text,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )
        SearchField(
            value = uiState.query,
            onValueChange = { onAction(CustomerListActions.OnQueryChange(it)) },
            placeholder = stringResource(Res.string.customer_search_hint),
            testTag = TestTags.CUSTOMER_SEARCH,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
        )
        Text(
            text = stringResource(Res.string.customer_count, uiState.customers.size, uiState.totalCount),
            style = AppTypography.Caption,
            color = AppColors.TextSub,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space4),
        )
        LineDivider()
        val today = uiState.today
        when {
            today == null || (uiState.totalCount == 0 && uiState.isRefreshing) ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            uiState.customers.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(Res.string.customer_empty), color = AppColors.TextSub)
                }

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.customers, key = { it.id }) { customer ->
                    CustomerItem(
                        customer = customer,
                        today = today,
                        maskPersonalInfo = uiState.maskPersonalInfo,
                        isSelected = customer.id == selectedId,
                        isConsulting = customer.id == uiState.activeCustomerId,
                        onClick = { onAction(CustomerListActions.OnCustomerClick(customer.id)) },
                    )
                    LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
                }
            }
        }
    }
}
