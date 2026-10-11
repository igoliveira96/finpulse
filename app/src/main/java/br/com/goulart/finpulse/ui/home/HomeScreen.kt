package br.com.goulart.finpulse.ui.home

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.goulart.finpulse.R
import br.com.goulart.finpulse.data.model.HomeSnapshot
import br.com.goulart.finpulse.data.model.Portfolio
import br.com.goulart.finpulse.data.model.Stock
import br.com.goulart.finpulse.data.remote.mock.MockMarketData
import br.com.goulart.finpulse.ui.theme.FinNegative
import br.com.goulart.finpulse.ui.theme.FinPositive
import br.com.goulart.finpulse.ui.theme.FinPositiveContainer
import br.com.goulart.finpulse.ui.theme.FinPulseTheme

@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.revalidateFavoritesWhileVisible()
        }
    }
    HomeScreen(state)
}

@Composable
fun HomeScreen(state: HomeUiState, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            when (state) {
                HomeUiState.Loading -> StatusContent(loading = true)
                HomeUiState.Error -> StatusContent(loading = false)
                is HomeUiState.Ready -> HomeContent(state.data, state.favoritesRefresh)
            }
        }
    }
}

@Composable
private fun StatusContent(loading: Boolean) {
    Column(
        Modifier.widthIn(max = 720.dp).fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeHeader()
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(20.dp))
            }
            Text(
                stringResource(if (loading) R.string.loading_portfolio else R.string.loading_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeContent(data: HomeSnapshot, refreshStatus: FavoritesRefreshStatus) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("home_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item(key = "header") {
            HomeHeader(Modifier.padding(horizontal = 24.dp), data.investor.initials)
        }
        item(key = "greeting") {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 24.dp)) {
                Text(
                    stringResource(R.string.greeting, data.investor.firstName),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    stringResource(R.string.portfolio_subtitle),
                    Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "portfolio") { PortfolioSummary(data.portfolio) }
        item(key = "favorites") {
            SectionHeader(
                title = stringResource(R.string.favorites),
                modifier = Modifier.padding(top = 28.dp, bottom = 14.dp),
            ) {
                FavoriteRefreshLabel(refreshStatus)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardWidth = ((maxWidth - 68.dp) / 3).coerceIn(112.dp, 200.dp)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(data.favorites, key = Stock::symbol) {
                        FavoriteCard(it, cardWidth, refreshStatus == FavoritesRefreshStatus.Refreshing)
                    }
                }
            }
        }
        item(key = "stocks_heading") {
            SectionHeader(
                title = stringResource(R.string.stocks),
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
            ) {
                StaticIcon(R.drawable.ic_search, stringResource(R.string.search_stocks))
            }
        }
        // The complete list includes favorites, just as a market catalogue would.
        val marketStocks = data.stocks.sortedBy { it.isFavorite }
        items(marketStocks, key = { "stock_${it.symbol}" }) { stock ->
            val first = stock.symbol == marketStocks.first().symbol
            val last = stock.symbol == marketStocks.last().symbol
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = if (first) 22.dp else 0.dp,
                    topEnd = if (first) 22.dp else 0.dp,
                    bottomStart = if (last) 22.dp else 0.dp,
                    bottomEnd = if (last) 22.dp else 0.dp,
                ),
            ) {
                Column {
                    StockRow(stock)
                    if (!last) HorizontalDivider(
                        Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
        item(key = "demo_note") {
            Text(
                stringResource(R.string.illustrative_quotes),
                Modifier.padding(start = 26.dp, top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier, initials: String? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_fin_market), contentDescription = null,
            modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary,
        )
        Text("FinPulse", Modifier.padding(start = 9.dp), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        StaticIcon(R.drawable.ic_bell, stringResource(R.string.notifications))
        Spacer(Modifier.width(12.dp))
        val profileDescription = stringResource(R.string.profile)
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = profileDescription; disabled() },
            contentAlignment = Alignment.Center,
        ) {
            Text(initials ?: "··", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PortfolioSummary(portfolio: Portfolio) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.invested_wealth), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            StaticIcon(R.drawable.ic_eye, stringResource(R.string.balance_visibility), size = 19)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Text(
                currency(portfolio.balanceInCents), Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.displaySmall,
                fontSize = if (maxWidth < 330.dp) 32.sp else 38.sp,
            )
        }
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Surface(color = FinPositiveContainer, shape = RoundedCornerShape(9.dp)) {
                Text("↗ ${percent(portfolio.monthlyChangePercent)}",
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    color = FinPositive, style = MaterialTheme.typography.labelMedium)
            }
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+${currency(portfolio.monthlyGainInCents)}",
                    color = FinPositive, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.in_month), Modifier.padding(start = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        MarketChart(
            portfolio.history, MaterialTheme.colorScheme.primary,
            Modifier.fillMaxWidth().height(118.dp).padding(top = 16.dp), filled = true,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("1M", "3M", "6M", "1A", stringResource(R.string.all_period)).forEachIndexed { index, label ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (index == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                    modifier = Modifier.weight(1f).semantics { disabled() },
                ) {
                    Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label,
                            color = if (index == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
        action()
    }
}

@Composable
private fun FavoriteRefreshLabel(status: FavoritesRefreshStatus) {
    val refreshing = status == FavoritesRefreshStatus.Refreshing
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (refreshing) CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        Text(
            stringResource(when (status) {
                FavoritesRefreshStatus.Refreshing -> R.string.refreshing_quotes
                FavoritesRefreshStatus.Failed -> R.string.cached_quotes
                else -> R.string.automatic_quotes
            }),
            style = MaterialTheme.typography.labelSmall,
            color = if (refreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteCard(stock: Stock, width: Dp, refreshing: Boolean) {
    val changeColor = if (stock.dailyChangePercent >= 0) FinPositive else FinNegative
    val blurRadius by animateDpAsState(if (refreshing) 4.dp else 0.dp, tween(200), label = "quoteBlur")
    val quoteState = stringResource(if (refreshing) R.string.refreshing_quotes else R.string.validated_quotes)
    // RenderEffect blur is available from Android 12. Older versions use an obscured placeholder.
    val maskValues = refreshing && Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.width(width)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stock.symbol, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                StaticIcon(R.drawable.ic_star_filled, stringResource(R.string.favorite_stock, stock.symbol), size = 15)
            }
            Column(
                Modifier.testTag("favorite_values_${stock.symbol}")
                    .semantics { stateDescription = quoteState }
                    .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded),
            ) {
                Text(if (maskValues) "R$ •••••" else currency(stock.priceInCents), Modifier.padding(top = 7.dp),
                    style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(if (maskValues) "••••%" else percent(stock.dailyChangePercent), Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = changeColor)
            }
            MarketChart(stock.history, changeColor, Modifier.fillMaxWidth().height(26.dp).padding(top = 6.dp))
        }
    }
}

@Composable
private fun StockRow(stock: Stock) {
    val changeColor = if (stock.dailyChangePercent >= 0) FinPositive else FinNegative
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 17.dp)) {
        val showChart = maxWidth >= 310.dp
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center) {
                Text(stock.company.take(1), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).padding(start = 11.dp, end = 6.dp)) {
                Text(stock.symbol, style = MaterialTheme.typography.titleSmall)
                Text(stock.company, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showChart) {
                MarketChart(stock.history, changeColor, Modifier.width(52.dp).height(25.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(currency(stock.priceInCents), style = MaterialTheme.typography.titleSmall)
                Text(percent(stock.dailyChangePercent), style = MaterialTheme.typography.labelMedium, color = changeColor)
            }
            Spacer(Modifier.width(12.dp))
            StaticIcon(
                if (stock.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star,
                stringResource(R.string.favorite_stock, stock.symbol), size = 18,
            )
        }
    }
}

/** Decorative chart: amounts and changes are exposed as text alongside it. */
@Composable
private fun MarketChart(values: List<Float>, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val low = values.min()
        val range = (values.max() - low).coerceAtLeast(1f)
        val inset = 3.dp.toPx()
        val points = values.mapIndexed { index, value ->
            Offset(
                inset + index.toFloat() / values.lastIndex * (size.width - inset * 2),
                size.height - inset - (value - low) / range * (size.height - inset * 2),
            )
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.zipWithNext().forEach { (previous, next) ->
                val middle = (previous.x + next.x) / 2
                cubicTo(middle, previous.y, middle, next.y, next.x, next.y)
            }
        }
        if (filled) {
            val fill = Path().apply {
                addPath(line)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.01f))))
        }
        drawPath(line, color, style = Stroke(
            width = if (filled) 2.dp.toPx() else 1.4.dp.toPx(),
            cap = StrokeCap.Round, join = StrokeJoin.Round,
        ))
    }
}

/** Deliberately non-interactive in this Home-only prototype. */
@Composable
private fun StaticIcon(@DrawableRes icon: Int, description: String, size: Int = 22) {
    Icon(painterResource(icon), contentDescription = description,
        modifier = Modifier.size(size.dp).semantics { disabled() },
        tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview(name = "Home · Phone", widthDp = 412, heightDp = 917, showBackground = true)
@Preview(name = "Home · Compact", widthDp = 320, heightDp = 740, showBackground = true)
@Preview(name = "Home · Tablet", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun HomePreview() {
    FinPulseTheme { HomeScreen(HomeUiState.Ready(MockMarketData.home)) }
}

@Preview(name = "Loading", widthDp = 412, heightDp = 917)
@Composable
private fun LoadingPreview() {
    FinPulseTheme { HomeScreen(HomeUiState.Loading) }
}
