package com.moegirlviewer.screen.article

import androidx.compose.runtime.*
import androidx.compose.ui.node.Ref
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.moegirlviewer.Constants
import com.moegirlviewer.DataSource
import com.moegirlviewer.R
import com.moegirlviewer.api.editingRecord.EditingRecordApi
import com.moegirlviewer.api.page.PageApi
import com.moegirlviewer.api.watchList.WatchListApi
import com.moegirlviewer.compable.remember.MemoryStore
import com.moegirlviewer.component.articleView.*
import com.moegirlviewer.component.commonDialog.ButtonConfig
import com.moegirlviewer.component.commonDialog.CommonAlertDialogProps
import com.moegirlviewer.component.customDrawer.CustomDrawerState
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.request.MoeRequestException
import com.moegirlviewer.room.browsingRecord.BrowsingRecord
import com.moegirlviewer.room.watchingPage.WatchingPage
import com.moegirlviewer.screen.article.component.header.EditAllowedStatus
import com.moegirlviewer.screen.drawer.CommonDrawerState
import com.moegirlviewer.screen.edit.EditRouteArguments
import com.moegirlviewer.screen.edit.EditType
import com.moegirlviewer.store.AccountStore
import com.moegirlviewer.store.CommentStore
import com.moegirlviewer.store.CommonSettings
import com.moegirlviewer.store.SettingsStore
import com.moegirlviewer.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import org.jsoup.Jsoup
import javax.inject.Inject

@HiltViewModel
class ArticleScreenModel @Inject constructor() : ViewModel() {
  val coroutineScope = CoroutineScope(Dispatchers.Main)
  val cachedWebViews = CachedWebViews()
  val memoryStore = MemoryStore()
  val commonDrawerState = CommonDrawerState()
  val catalogDrawerState = CustomDrawerState()
  val articleViewState = ArticleViewState()
  lateinit var routeArguments: ArticleRouteArguments

  val articleData by articleViewState::articleData
  var visibleHeader by mutableStateOf(true)
  var catalogData by mutableStateOf(emptyList<ArticleCatalog>())
  val articleInfo by articleViewState::articleInfo
  // ?????????????????????????????????????????????????????????????????????????????????articleInfo????????????????????????????????????????????????????????????????????????state??????
  var isWatched by mutableStateOf(false)
  var visibleFindBar by mutableStateOf(false)
  var visibleCommentButton by mutableStateOf(false)
  var editAllowed by mutableStateOf(EditAllowedStatus.CHECKING)
//  var swipeRefreshState = SwipeRefreshState(false)
//  var scrollState = ScrollState(0)

  // ???????????????
  val truePageName get() = articleData?.parse?.title ?:
    routeArguments.pageKey?.triedPageNameOrNull ?:
    routeArguments.readingRecord?.pageName
  // ?????????????????????(???????????????????????????????????????)
  val displayPageName get() = getTextFromHtml(
    (
      articleData?.parse?.displaytitle ?:
      routeArguments.displayName ?:
      routeArguments.pageKey?.triedPageNameOrNull ?:
      routeArguments.readingRecord?.pageName ?:
      Globals.context.getString(R.string.app_name)
    )
      .replace("_", " ")
      .replace(categoryPageNamePrefixRegex, "${Globals.context.getString(R.string.category)}???")
  )
  val pageId get() = routeArguments.pageKey?.triedPageIdOrNull ?: articleData?.parse?.pageid

  // ??????????????????????????????
  val commentButtonAllowed: Boolean @Composable get() {
//    val isLightRequestMode by SettingsStore.common.getValue { lightRequestMode }.collectAsState(initial = false)
    val isLightRequestMode = true
    return if (isLightRequestMode) {
      if (truePageName != null)
        isTalkPage(truePageName!!).not()
          && isMoegirl(true, hmoeCommentDisabledTitles.contains(truePageName).not())
        else false
    } else {
      listOf(
        MediaWikiNamespace.MAIN.code,
        MediaWikiNamespace.USER.code,
        MediaWikiNamespace.HELP.code,
        MediaWikiNamespace.PROJECT.code
      ).contains(articleInfo?.ns) && isMoegirl(true, hmoeCommentDisabledTitles.contains(truePageName).not())
    }
  }
  // ?????????????????????????????????????????????????????????????????????
  val visibleTalkButton get() = articleInfo?.ns != null && !MediaWikiNamespace.isTalkPage(articleInfo!!.ns)
  // ?????????????????????????????????
  val isTalkPageExists get() = articleInfo?.talkid != null

  // ??????????????????????????????????????????????????????????????????????????????????????????stopMediaOnLeave???????????????????????????articleViewRef.enableAllMedia()
  // ?????????????????????iframe???????????????????????????????????????iframe???src???????????????????????????
  var isMediaDisabled = false

  // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
  var isLightRequestModeWhenOpened: Boolean? = null

  suspend fun handleOnArticleLoaded() = coroutineScope {
    listOf(
      launch {
        isWatched = Globals.room.watchingPage().exists(truePageName!!)
      },
//    isWatched = articleInfo?.watched != null

      launch {
        delay(500)
        visibleCommentButton = true
      },

      launch {
        CommentStore.loadNext(pageId!!)
      },

      launch {
        val mainPageUrl = try {
          PageApi.getMainImageAndIntroduction(routeArguments.pageKey!!, size = 250).query.pages.values.first().thumbnail?.source
        } catch (e: MoeRequestException) { null }

        Globals.room.browsingRecord().insertItem(BrowsingRecord(
          pageName = truePageName!!,
          displayName = displayPageName,
          imgUrl = mainPageUrl
        ))
      },

      launch {
//      val isLightRequestMode = SettingsStore.common.getValue { lightRequestMode }.first()
        val isLightRequestMode = true
        if (isLightRequestMode) {
          // ??????????????????????????????????????????????????????????????????
          editAllowed = EditAllowedStatus.ALLOWED_FULL
        } else {
          checkEditAllowed()
        }
      },

      launch {
        if (Constants.source != DataSource.HMOE) return@launch
        val isLoggedIn = AccountStore.isLoggedIn.first()
        if (!isLoggedIn) checkIsUnfairPage()
      },

      launch {
        if (routeArguments.anchor == null && truePageName != "???????????? talk:?????????" && isTalkPage(truePageName ?: "")) {
          delay(500)
          articleViewState.injectScript("window.scrollTo(0, 999999999)")
        }
      }
    ).forEach { it.join() }
  }

  fun handleOnArticleMissed() {
    Globals.commonAlertDialog.show(CommonAlertDialogProps(
      content = {
        StyledText(
          text = stringResource(id = R.string.articleMissedHint)
        )
      },
      onPrimaryButtonClick = {
        Globals.navController.popBackStack()
      },
      onDismiss = {
        Globals.navController.popBackStack()
      }
    ))
  }

  suspend fun handleOnArticleRendered() {
    if (routeArguments.anchor != null) {
      val minusOffset = Constants.topAppBarHeight + Globals.statusBarHeight
      articleViewState.injectScript("""
          document.getElementById('${routeArguments.anchor}').scrollIntoView()
          window.scrollTo(0, window.scrollY - $minusOffset)
        """.trimIndent())
    }

    if (truePageName == "H??????:????????????") {
      articleViewState.injectScript("""
          document.getElementById('app-background').style.display = 'block'
          document.getElementById('app-background-top-padding').style.height = '${Constants.topAppBarHeight + Globals.statusBarHeight}px'
          document.body.style.maxHeight = '100%'
          document.body.style.overflowY = 'hidden'
          document.documentElement.style.overflowY = 'hidden'

          const styleTag = document.createElement('style')
          styleTag.innerHTML = '.mw-headline::after { display: none }'
          document.head.append(styleTag)          
        """.trimIndent())
    }

    if (routeArguments.readingRecord != null) {
      articleViewState.injectScript("""
          window.scrollTo(0, ${routeArguments.readingRecord!!.scrollY})
        """.trimIndent())
    }
  }

  suspend fun handleOnGotoEditClicked() {
    // ???????????????articleInfo???????????????????????????????????????????????????
    if (articleInfo == null) {
      Globals.commonLoadingDialog.show()
      try {
        loadArticleInfo()
        checkEditAllowed()
        if (!editAllowed.allowed) {
          Globals.commonAlertDialog.showText(Globals.context.getString(R.string.noAllowEditThePage))
          return
        }
        if (editAllowed == EditAllowedStatus.ALLOWED_SECTION) {
          Globals.commonAlertDialog.showText(Globals.context.getString(R.string.editFullTextDisabling))
          return
        }
      } catch (e: MoeRequestException) {
        printRequestErr(e, "??????????????????????????????????????????info?????????")
        toast(Globals.context.getString(R.string.netErr))
      } finally {
        Globals.commonLoadingDialog.hide()
      }
    }


    val isNonautoConfirmed = checkIfNonAutoConfirmedToShowEditAlert(truePageName!!)
    if (isNonautoConfirmed) return
    Globals.navController.navigate(EditRouteArguments(
      pageName = truePageName!!,
      type = EditType.FULL
    ))
  }

  suspend fun handleOnPreGotoEdit(): Boolean {
    try {
      if (articleInfo == null) {
        Globals.commonLoadingDialog.show()
        loadArticleInfo()
        checkEditAllowed()
        if (!editAllowed.allowed) {
          Globals.commonAlertDialog.showText(Globals.context.getString(R.string.noAllowEditThePage))
          return false
        }
      }
    } catch (e: MoeRequestException) {
      printRequestErr(e, "????????????????????????????????????????????????info?????????")
      toast(Globals.context.getString(R.string.netErr))
      return false
    } finally {
      Globals.commonLoadingDialog.hide()
    }

    return true
  }

  suspend fun handleOnAddSectionClicked() {
    val isNonautoConfirmed = checkIfNonAutoConfirmedToShowEditAlert(truePageName!!, "new")
    if (isNonautoConfirmed) return
    Globals.navController.navigate(EditRouteArguments(
      pageName = truePageName!!,
      type = EditType.SECTION,
      section = "new"
    ))
  }

  fun handleOnGotoTalk() {
    val talkPageName = if (articleInfo!!.ns == 0) {
      "${Globals.context.getString(R.string.talk)}:$truePageName"
    } else {
      truePageName!!.replaceFirst(":", if (isMoegirl()) "_talk:" else "??????:")
    }

    if (isTalkPageExists) {
      gotoArticlePage(talkPageName)
    } else {
      Globals.commonAlertDialog.show(CommonAlertDialogProps(
        content = { StyledText(Globals.context.getString(R.string.talkPageMissedHint)) },
        secondaryButton = ButtonConfig.cancelButton(),
        onPrimaryButtonClick = {
          coroutineScope.launch {
            try {
              checkIsLoggedIn(Globals.context.getString(R.string.notLoggedInHint))
              val isNonautoConfirmed = checkIfNonAutoConfirmedToShowEditAlert(truePageName!!, "new")
              if (isNonautoConfirmed) return@launch
              Globals.navController.navigate(EditRouteArguments(
                pageName = talkPageName,
                type = EditType.SECTION,
                section = "new"
              ))
            } catch (e: NotLoggedInException) {}
          }
        }
      ))
    }
  }

  // ?????????????????????articleInfo???????????????????????????????????????????????????
  suspend fun loadArticleInfo() {
    if (articleInfo != null) return
    articleViewState.core.articleInfo = PageApi.getPageInfo(routeArguments.pageKey!!)
  }

  suspend fun getEditAllowed(): Boolean? {
    return try {
      val userInfo = AccountStore.loadUserInfo()
      val isUnprotectednessPage = articleInfo!!.protection.all { it.type != "edit" } || articleInfo!!.protection.isEmpty()
      val isSysop = userInfo.groups.contains("sysop")
      val isPatroller = userInfo.groups.contains("patroller")

      // ??????????????????
      isUnprotectednessPage ||
        // ??????????????????
        isSysop ||
        // ??????????????????????????????????????????????????????????????????
        (isPatroller && articleInfo!!.protection.first { it.type == "edit" }.level == "patrolleredit")
    } catch (e: MoeRequestException) {
      printRequestErr(e, "?????????????????????????????????")
      null
    }
  }

  suspend fun checkEditAllowed() {
    if (!AccountStore.isLoggedIn.first() || articleInfo == null) return

    if (routeArguments.revId != null) {
      val lastEditingRecordRes = EditingRecordApi.getPageRevisions(routeArguments.pageKey!!)

      val lastEditingRecord = lastEditingRecordRes.query.pages.values.first().revisions?.first() ?: return
      if (lastEditingRecord.revid != routeArguments.revId) {
        editAllowed = EditAllowedStatus.DISABLED
        toast(Globals.context.getString(R.string.historyModeEditDisabledHint))
        return
      }
    }

    // ???????????????????????????????????????????????????
    val editFullDisabled = MediaWikiNamespace.isTalkPage(articleInfo!!.ns)

    editAllowed = when(getEditAllowed()) {
      true -> if (editFullDisabled) EditAllowedStatus.ALLOWED_SECTION else EditAllowedStatus.ALLOWED_FULL
      false -> EditAllowedStatus.DISABLED
      null -> EditAllowedStatus.CHECKING
    }
  }

  suspend fun togglePageIsInWatchList() {
    Globals.commonLoadingDialog.show()
    try {
      WatchListApi.setWatchStatus(truePageName!!, !isWatched)
      isWatched = !isWatched
      Globals.commonLoadingDialog.hide()

      val joinWord = Globals.context.getString(R.string.join)
      val removeWord = Globals.context.getString(R.string.remove)
      toast(Globals.context.getString(R.string.watchListOperatedHint, if (isWatched) joinWord else removeWord))
      if (isWatched) {
        Globals.room.watchingPage().insertItem(WatchingPage(truePageName!!))
      } else {
        Globals.room.watchingPage().deleteItem(WatchingPage(truePageName!!))
      }
    } catch(e: MoeRequestException) {
      printRequestErr(e, "????????????????????????")
      toast(e.message)
    }
  }

  suspend fun jumpToAnchor(anchor: String) {
//    val minusOffset = (Constants.topAppBarHeight + Globals.statusBarHeight)
//    val anchorPosition = articleViewRef.value!!.htmlWebViewRef!!.injectScript("moegirl.method.link.getAnchorPosition('$anchor')").toFloatOrNull()?.roundToInt()
//    if (anchorPosition != null) {
//      val scrollValue = (anchorPosition * Globals.activity.resources.displayMetrics.density - minusOffset).roundToInt()
//      scrollState.animateScrollTo(scrollValue)
//    }

    val minusOffset = Constants.topAppBarHeight + Globals.statusBarHeight
    articleViewState.injectScript(
      "moegirl.method.link.gotoAnchor('$anchor', -$minusOffset)"
    )
  }

  fun share() {
    val siteName = Globals.context.getString(R.string.siteName)
    shareText("$siteName - $truePageName ${Constants.shareUrl}$pageId")
  }

  suspend fun checkIsUnfairPage() = withContext(Dispatchers.Default) {
    val articleDoc = Jsoup.parse(articleData!!.parse.text._asterisk)
    val isUnfairPage = articleDoc.getElementById("EditUnfairWarning") != null
    if (isUnfairPage) {
      Globals.commonAlertDialog.show(
        CommonAlertDialogProps(
          title = Globals.context.getString(R.string.hmoeUnfairHintTitle),
          content = {
            StyledText(text = Globals.context.getString(R.string.hmoeUnfairHintBody))
          }
        )
      )
    }
  }

  companion object {
    // ????????????article screen?????????????????????????????????true?????????reload
    var needReload = false
  }

  override fun onCleared() {
    cachedWebViews.destroyAllInstance()
    coroutineScope.cancel()
    routeArguments.removeReferencesFromArgumentPool()
    super.onCleared()
  }
}

private val hmoeCommentDisabledTitles = listOf(
  "H??????:????????????",
  "H??????:????????????",
  "H??????:???????????????",
  "H??????:????????????",
  "H??????:??????????????????",
  "H??????:???????????????",
  "H??????:??????????????????",
  "???????????????",
  "?????????",
  "??????",
)

object BodyDoubleClickJs {
  private const val messageName = "bodyDoubleClicked"
  val scriptContent = """
    (() => {
      let doubleClickFlag = false
      $('body').on('click', e => {
        if (doubleClickFlag) {
          doubleClickFlag = false
          _postMessage('$messageName')
        } else {
          doubleClickFlag = true
          setTimeout(() => doubleClickFlag = false, 400)
        }
      })
    })()
  """.trimIndent()

  val messageHandler: Pair<String, (JsonObject?) -> Unit> @Composable get() {
    val model: ArticleScreenModel = hiltViewModel()
    val isFocusMode by SettingsStore.common.getValue { focusMode }.collectAsState(initial = false)

    return remember(isFocusMode) {
      messageName to {
        if (isFocusMode) {
          model.visibleHeader = !model.visibleHeader
          model.visibleCommentButton = model.visibleHeader
        }
      }
    }
  }
}