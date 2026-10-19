package com.eunilsung.ods.ui.main.tab

import androidx.compose.runtime.Composable
import com.eunilsung.ods.ui.customer.list.CustomerListScreen
import com.eunilsung.ods.ui.more.MoreScreen
import com.eunilsung.ods.ui.watchlist.WatchlistScreen
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.tab_customer
import ods.composeapp.generated.resources.tab_customer_icon
import ods.composeapp.generated.resources.tab_market
import ods.composeapp.generated.resources.tab_market_icon
import ods.composeapp.generated.resources.tab_more
import ods.composeapp.generated.resources.tab_more_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 고객 — 담당 고객 찾기, 상세, 상담 이력. */
internal object CustomerTab : AppTab {
    override val key: String = "customer"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_customer),
            icon = painterResource(Res.drawable.tab_customer_icon)
        )

    @Composable
    override fun Content() {
        CustomerListScreen.Content()
    }
}

/** 시장 — 상담 중 고객에게 보여 줄 관심종목 시세와 차트. */
internal object MarketTab : AppTab {
    override val key: String = "market"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_market),
            icon = painterResource(Res.drawable.tab_market_icon)
        )

    @Composable
    override fun Content() {
        WatchlistScreen.Content()
    }
}

internal object MoreTab : AppTab {
    override val key: String = "more"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_more),
            icon = painterResource(Res.drawable.tab_more_icon)
        )

    @Composable
    override fun Content() {
        MoreScreen.Content()
    }
}
