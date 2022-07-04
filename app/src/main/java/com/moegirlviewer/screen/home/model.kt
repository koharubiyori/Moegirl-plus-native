package com.moegirlviewer.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.moegirlviewer.R
import com.moegirlviewer.compable.remember.MemoryStore
import com.moegirlviewer.component.articleView.ArticleViewState
import com.moegirlviewer.screen.drawer.CommonDrawerState
import com.moegirlviewer.screen.home.component.CarouseCardState
import com.moegirlviewer.screen.home.component.RandomPageCardState
import com.moegirlviewer.screen.home.component.RecommendationCardState
import com.moegirlviewer.screen.home.component.ArticleViewCardState
import com.moegirlviewer.screen.home.component.newPagesCard.NewPagesCardState
import com.moegirlviewer.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class HomeScreenModel @Inject constructor() : ViewModel() {
  val coroutineScope = CoroutineScope(Dispatchers.Main)
  val cachedWebViews = CachedWebViews()
  val memoryStore = MemoryStore()
  val commonDrawerState = CommonDrawerState()
  val articleViewState = ArticleViewState()
  val swipeRefreshState = SwipeRefreshState(false)
  var cardsDataStatus by mutableStateOf(LoadStatus.INITIAL)

  val moegirlHomeTopArticleCardState = ArticleViewCardState()
  val carouseCard = CarouseCardState()
  val randomPageCardState = RandomPageCardState()
  val newPagesCardState = NewPagesCardState()
  val recommendationCardState = RecommendationCardState()

  private var twoPressBackFlag = false
  fun triggerForTwoPressToExit() {
    if (!twoPressBackFlag) {
      twoPressBackFlag = true
      toast(Globals.context.getString(R.string.doubleBackToExit))

      coroutineScope.launch {
        delay(3000)
        twoPressBackFlag = false
      }
    } else {
      exitApp()
    }
  }

  suspend fun loadCardsData() = coroutineScope {
    cardsDataStatus = LoadStatus.LOADING
    val reloadList = listOf<suspend () -> Unit>(
      { if (isMoegirl())
        moegirlHomeTopArticleCardState.reload() else
        carouseCard.reload()
      },
      { randomPageCardState.reload() },
      { newPagesCardState.reload() },
      { recommendationCardState.reload() },
    )

    // 因为用移动端域名绕过了waf，暂时注释掉
//    if (isMoegirl()) {
//      // 萌百不能用并发，会导致被waf
//      try {
//        for (item in reloadList) {
//          item()
//        }
//      } catch (e: MoeRequestException) { }
//    } else {
      reloadList
        .map { launch { it() } }
        .forEach { it.join() }
//    }

    cardsDataStatus = LoadStatus.SUCCESS
  }

  override fun onCleared() {
    super.onCleared()
    coroutineScope.cancel()
    cachedWebViews.destroyAllInstance()
  }

  companion object {
    var needReload = false
  }
}

abstract class HomeScreenCardState {
  abstract suspend fun reload()
}