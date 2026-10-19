package com.eunilsung.ods.ui.main.tab

import androidx.compose.runtime.Composable
import com.eunilsung.ods.ui.more.MoreScreen
import com.eunilsung.ods.ui.watchlist.WatchlistScreen
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.tab_market
import ods.composeapp.generated.resources.tab_market_icon
import ods.composeapp.generated.resources.tab_more
import ods.composeapp.generated.resources.tab_more_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
