package com.app.biashara.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DesktopScrollColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier) {
        Column(Modifier.fillMaxWidth().padding(end = 12.dp).verticalScroll(scrollState), verticalArrangement, horizontalAlignment, content)
        VerticalScrollbar(rememberScrollbarAdapter(scrollState), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
internal fun DesktopLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    Box(modifier) {
        LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state = state, contentPadding = contentPadding,
            verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment, content = content)
        VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
internal fun DesktopLazyGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyGridScope.() -> Unit
) {
    Box(modifier) {
        LazyVerticalGrid(columns, Modifier.fillMaxSize().padding(end = 12.dp), state = state, contentPadding = contentPadding,
            verticalArrangement = verticalArrangement, horizontalArrangement = horizontalArrangement, content = content)
        VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}
