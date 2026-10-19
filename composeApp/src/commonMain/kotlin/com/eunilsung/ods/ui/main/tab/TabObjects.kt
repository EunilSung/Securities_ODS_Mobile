package com.eunilsung.ods.ui.main.tab

import androidx.compose.runtime.Composable
import com.eunilsung.ods.ui.balance.BalanceScreen
import com.eunilsung.ods.ui.more.MoreScreen
import com.eunilsung.ods.ui.order.OrderScreen
import com.eunilsung.ods.ui.quote.QuoteScreen
import com.eunilsung.ods.ui.watchlist.WatchlistScreen
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.tab_balance
import ods.composeapp.generated.resources.tab_balance_icon
import ods.composeapp.generated.resources.tab_more
import ods.composeapp.generated.resources.tab_more_icon
import ods.composeapp.generated.resources.tab_order
import ods.composeapp.generated.resources.tab_order_icon
import ods.composeapp.generated.resources.tab_quote
import ods.composeapp.generated.resources.tab_quote_icon
import ods.composeapp.generated.resources.tab_watchlist
import ods.composeapp.generated.resources.tab_watchlist_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal object WatchlistTab : AppTab {
    override val key: String = "watchlist"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_watchlist),
            icon = painterResource(Res.drawable.tab_watchlist_icon)
        )

    @Composable
    override fun Content() {
        WatchlistScreen.Content()
    }
}

internal object QuoteTab : AppTab {
    override val key: String = "quote"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_quote),
            icon = painterResource(Res.drawable.tab_quote_icon)
        )

    @Composable
    override fun Content() {
        QuoteScreen.Content()
    }
}

internal object OrderTab : AppTab {
    override val key: String = "order"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_order),
            icon = painterResource(Res.drawable.tab_order_icon)
        )

    @Composable
    override fun Content() {
        OrderScreen.Content()
    }
}

internal object BalanceTab : AppTab {
    override val key: String = "balance"

    override val options: AppTabOptions
        @Composable
        get() = AppTabOptions(
            title = stringResource(Res.string.tab_balance),
            icon = painterResource(Res.drawable.tab_balance_icon)
        )

    @Composable
    override fun Content() {
        BalanceScreen.Content()
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
