package com.sooyeol.composeuses

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScene(
    modifier: Modifier = Modifier,
    imageList: List<Int> = MainActivity.imageList,
) {
    // 스크롤의 position의 상태를 저장.
    val lazyListState = rememberLazyListState()

    //페이지의 스크롤 되는중인지, 어떤페이지인지에 대한 정보(PagerSnapState) 즉 페이지 상태를 반환하는 메서드이다.
    val state = rememberPagerSnapState()

    //해당 composable의 lifecycle과 같은 lifecycle을 가집니다.
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val widthPx = with(LocalDensity.current) {
        screenWidth.roundToPx()
    }


    val connection = remember(state, lazyListState) {
        PagerSnapNestedScrollConnection(state, lazyListState) {

            val firstItemIndex = state.firstVisibleItemIndex.value
            val firstItemOffset = kotlin.math.abs(state.offsetInfo.value)


            val position = when {
                firstItemOffset <= widthPx.div(2) -> firstItemIndex
                else -> firstItemIndex.plus(1)
            }

            scope.launch {
                state.scrollItemToSnapPosition(lazyListState, position)
            }

        }
    }

    val padding = 16.dp
    Box(
        modifier = modifier
            .nestedScroll(connection = connection)
            .fillMaxWidth()
            .padding(horizontal = padding)
            .padding(top = padding, bottom = padding)
    ) {
        LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(padding),
        ) {

            items(imageList.size) { ind ->
                Image(
                    painter = painterResource(imageList[ind]),
                    contentDescription = "OnBoarding Images",
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .fillParentMaxHeight(),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
        LazyRow(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(imageList.size) { ind ->
                //하나는 투명하게 하나는 진하게
                if (ind == lazyListState.firstVisibleItemIndex) {
                    Text(
                        text = ".",
                        color = Color.White,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = ".",
                        modifier = Modifier
                            .alpha(.5f),
                        color = Color.White,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
fun rememberPagerSnapState(): PagerSnapState {
    return remember {
        PagerSnapState()
    }
}

//페이지가 스크롤될때 호출되는 리스너를 담은 클래스
class PagerSnapNestedScrollConnection(
    private val state: PagerSnapState,
    private val listState: LazyListState,
    private val scrollTo: () -> Unit
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        when (source) {
            NestedScrollSource.Drag -> onScroll()
            else -> Offset.Zero
        }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = when (source) {
        NestedScrollSource.Drag -> onScroll()
        else -> Offset.Zero
    }

    private fun onScroll(): Offset {

        state.isSwiping.value = true
        return Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = when {
        state.isSwiping.value -> {

            state.updateScrollToItemPosition(listState.layoutInfo.visibleItemsInfo.firstOrNull())

            scrollTo()

            Velocity.Zero
        }
        else -> {
            Velocity.Zero
        }
    }.also {
        state.isSwiping.value = false
    }

}

class PagerSnapState {

    val isSwiping = mutableStateOf(false)

    val firstVisibleItemIndex = mutableStateOf(0)

    val offsetInfo = mutableStateOf(0)

    internal fun updateScrollToItemPosition(itemPos: LazyListItemInfo?) {
        if (itemPos != null) {
            this.offsetInfo.value = itemPos.offset
            this.firstVisibleItemIndex.value = itemPos.index
        }
    }

    internal suspend fun scrollItemToSnapPosition(listState: LazyListState, position: Int) {
        listState.animateScrollToItem(position)
    }
}