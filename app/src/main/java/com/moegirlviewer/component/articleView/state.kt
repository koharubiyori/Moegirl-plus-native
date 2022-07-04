package com.moegirlviewer.component.articleView

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.Ref
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moegirlviewer.Constants
import com.moegirlviewer.CrossWikiUrlPrefix
import com.moegirlviewer.DataSource
import com.moegirlviewer.api.page.PageApi
import com.moegirlviewer.api.page.bean.PageContentResBean
import com.moegirlviewer.api.page.bean.PageInfoResBean
import com.moegirlviewer.component.articleView.util.collectCategoryDataFromHtml
import com.moegirlviewer.component.articleView.util.createDefaultMessageHandlers
import com.moegirlviewer.component.articleView.util.createMoegirlRendererConfig
import com.moegirlviewer.component.articleView.util.preProcessArticleHtml
import com.moegirlviewer.component.htmlWebView.HtmlWebViewContent
import com.moegirlviewer.component.htmlWebView.HtmlWebViewMessageHandlers
import com.moegirlviewer.component.htmlWebView.HtmlWebViewRef
import com.moegirlviewer.component.htmlWebView.HtmlWebViewScrollChangeHandler
import com.moegirlviewer.request.MoeRequestException
import com.moegirlviewer.request.MoeRequestWikiException
import com.moegirlviewer.request.cookieJar
import com.moegirlviewer.request.moeOkHttpClient
import com.moegirlviewer.screen.article.ReadingRecord
import com.moegirlviewer.screen.category.CategoryRouteArguments
import com.moegirlviewer.store.CommonSettings
import com.moegirlviewer.store.SettingsStore
import com.moegirlviewer.theme.background2
import com.moegirlviewer.theme.currentIsUseDarkTheme
import com.moegirlviewer.theme.currentThemeColors
import com.moegirlviewer.util.*
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.CookieManager
import com.tencent.smtt.sdk.WebView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.stream.Collectors

typealias ArticleData = PageContentResBean
typealias ArticleInfo = PageInfoResBean.Query.MapValue

val defaultInjectedFiles = listOf("main.css", "main.js")

class ArticleViewState {
  val core = ArticleViewStateCore()
  val articleData by core::articleData
  val articleInfo by core::articleInfo
  val status by core::status
  val htmlWebViewRef by core::htmlWebViewRef

  suspend fun updateHtmlView(force: Boolean = false) = core.updateHtmlView(force)
  suspend fun reload(force: Boolean = true) = core.loadArticleContent(forceLoad = force)
  suspend fun injectScript(script: String) = core.htmlWebViewRef.value!!.injectScript(script)
  suspend fun enableAllMedia() = core.enableAllMedia()
  suspend fun disableAllMedia() = core.disableAllMedia()
}

class ArticleViewStateCore() {
  var pageKey: PageKey? = null // 不传html时，pageKey必传
  var html: String? = null
  var revId: Int? = null
  var readingRecord: ReadingRecord? = null
  var injectedStyles: List<String>? = null
  var injectedScripts: List<String>? = null
  var visibleLoadStatusIndicator: Boolean = true
  var linkDisabled: Boolean = false
  var fullHeight: Boolean = false  // 用于外部容器代理滚动的模式
  var inDialogMode: Boolean = false
  var editAllowed: Boolean = false
  var addCopyright: Boolean = false
  var addCategories: Boolean = true
  var cacheEnabled: Boolean = false
  var previewMode: Boolean = false   // 这个参数对应的就是api的preview参数，没有其他功能，使用这个会获得不带缓存的渲染结果
  var visibleEditButton: Boolean = true
  var contentTopPadding: Dp = 0.dp
  var renderDelay: Long = 0
  var themeBackground2: Color = Color.Transparent
  var messageHandlers: HtmlWebViewMessageHandlers? = null
  var emitCatalogData: ((data: List<ArticleCatalog>) -> Unit)? = null
  var onArticleLoaded: ((articleData: ArticleData, articleInfo: ArticleInfo?) -> Unit)? = null
  var onScrollChanged: HtmlWebViewScrollChangeHandler? = null
  var onPreGotoEdit: (suspend () -> Boolean)? = null
  var onArticleRendered: (() -> Unit)? = null
  var onArticleMissed: (() -> Unit)? = null
  var onArticleError: (() -> Unit)? = null

  val density = Globals.activity.resources.displayMetrics.density
  val themeColors get() = currentThemeColors
  val isDarkTheme get() = currentIsUseDarkTheme

  val coroutineScope = CoroutineScope(Dispatchers.Main)
  var articleData by mutableStateOf<ArticleData?>(null)
  var articleInfo by mutableStateOf<ArticleInfo?>(null)
  var status by mutableStateOf(LoadStatus.INITIAL)
  var contentHeight by mutableStateOf(0f)

  var imgOriginalUrls = mapOf<String, String>()
  var isInitialized = false

  val htmlWebViewRef = Ref<HtmlWebViewRef>()

  val articleHtml: String
    get() = html ?: articleData?.parse?.text?._asterisk ?: ""

  val pageName get() = articleData?.parse?.title ?: run {
    (pageKey as? PageNameKey)?.pageName?.firstOrNull()
  }

  suspend fun updateHtmlView(force: Boolean = false) {
    if (isInitialized && !force) { return }

    val moegirlRendererConfig = createMoegirlRendererConfig(
      pageName = pageName,
      language = if(isTraditionalChineseEnv()) "zh-hant" else "zh-hans",
      site = Constants.source.code,
      enabledCategories = addCategories,
      heimu = SettingsStore.common.getValue { this.heimu }.first(),
      enabledHeightObserver = fullHeight,
      addCopyright = addCopyright,
      nightMode = isDarkTheme,
      categories = articleData?.parse?.categories
        ?.filter { it.hidden == null }
        ?.map { it._asterisk } ?: emptyList(),
      isTalkPage = isTalkPage(pageName ?: ""),
      hideTopInfoBox = SettingsStore.common.getValue { this.hideTopTemplates }.first()
    )

    val useSpecialCharSupportedFont = SettingsStore.common.getValue { this.useSpecialCharSupportedFontInArticle }.first()
    val useSerifFont = SettingsStore.common.getValue { useSerifFontInArticle }.first()
    val usingFonts = mutableListOf<String>().apply {
      if (useSpecialCharSupportedFont) add("NospzGothicMoe")
      if (useSerifFont) add("serif")
    }
      .joinToString(", ")

    val styles = """
      @font-face {
        font-family: "NospzGothicMoe";
        src: url("nospz_gothic_moe.ttf");
      }     

      body {
        font-family: ${if (usingFonts != "") usingFonts else "initial"};
        padding-top: ${contentTopPadding.value}px;
        word-break: ${if (inDialogMode) "break-all" else "initial"};
        ${if (inDialogMode) """
          margin: 0 !important;
          padding: 0;
          max-width: 100% !important;
        """.trimIndent() else ""}
        ${if (inDialogMode && isDarkTheme)
      "background-color: ${themeColors.surface.toCssRgbaString()} !important"
    else ""}
      }
      
      ${if (inDialogMode) """
        p {
          margin: 0;
        }
      """.trimIndent() else ""}
      
      ${if (!visibleEditButton) """
        .mw-editsection {
          display: none;
        }
      """.trimIndent() else ""}
      
      :root {
        --color-primary: ${themeColors.primaryVariant.toCssRgbaString()};
        --color-dark: ${themeColors.primaryVariant.darken(0.3F).toCssRgbaString()};
        --color-light: ${themeColors.primaryVariant.lighten(0.3F).toCssRgbaString()};
        --color-background2: ${themeBackground2.toCssRgbaString()};
      }
      
      ::selection {
        background-color: ${themeColors.primaryVariant.copy(0.3f).toCssRgbaString()};
      }
    """.trimIndent()

    val cookieManager = CookieManager.getInstance()
    val cookies = cookieJar.loadForRequest(Constants.domain.toHttpUrl())

    coroutineScope {
      for (cookie in cookies) {
        launch {
//          val completableDeferred = CompletableDeferred<Unit>()
          cookieManager.setCookie(Constants.domain, cookie.toString())
        }
      }
    }

    val messageForLoaded = """
      _postMessage('loaded')
    """.trimIndent()

    val injectedStyles = listOf(styles) + (injectedStyles ?: emptyList())
    val injectedScripts = listOf(
      messageForLoaded,
      moegirlRendererConfig,
      *(injectedScripts ?: emptyList()).toTypedArray(),
    )
    val preProcessedArticleHtml = preProcessArticleHtml(articleHtml)

//    val imageProxyArticleHtml = articleHtml.imageProxy()
    htmlWebViewRef.value?.updateContent?.invoke {
      HtmlWebViewContent(
        body = preProcessedArticleHtml,
        title = pageName,
        injectedStyles = injectedStyles,
        injectedScripts = injectedScripts,
        injectedFiles = defaultInjectedFiles
      )
    }

    isInitialized = true
  }

  suspend fun loadImgOriginalUrls() {
    try {
      imgOriginalUrls = PageApi.getImagesUrl(articleData!!.parse.images)
    } catch (e: MoeRequestException) {
      printRequestErr(e, "获取条目内全部图片原始链接失败")
    }
  }

  suspend fun loadArticleContent(
    pageKey: PageKey = this.pageKey!!,
    revId: Int? = this.revId,
    forceLoad: Boolean = false
  ) = coroutineScope {
    suspend fun consumeArticleData(articleData: ArticleData, articleInfo: ArticleInfo?) {
      this@ArticleViewStateCore.articleData = articleData
      this@ArticleViewStateCore.articleInfo = articleInfo
      updateHtmlView(true)
//      val isLightRequestMode = SettingsStore.common.getValue { lightRequestMode }.first()
      val isLightRequestMode = true
      if (!isLightRequestMode) loadImgOriginalUrls()
//      onArticleLoaded?.invoke(articleData, articleInfo)
    }

    status = LoadStatus.LOADING

    launch {
      try {
        val isLightRequestMode = true
        var pageInfo: PageInfoResBean.Query.MapValue? = null

        val isCategoryPage = if (isLightRequestMode && pageName != null) {
          pageName!!.contains(categoryPageNamePrefixRegex)
        } else {
          pageInfo = PageApi.getPageInfo(pageKey)
          MediaWikiNamespace.CATEGORY.code == pageInfo.ns
        }

        val articleData = PageApi.getPageContent(pageKey, revId, previewMode = previewMode)

        if (isCategoryPage) {
          val collectedCategoryData = collectCategoryDataFromHtml(articleData.parse.text._asterisk)
          delay(300)
          Globals.navController.replace(
            CategoryRouteArguments(
              categoryName = pageName!!.replaceFirst(categoryPageNamePrefixRegex, ""),
              parentCategories = collectedCategoryData.parentCategories,
              categoryExplainPageName = collectedCategoryData.categoryExplainPageName
            )
          )

          return@launch
        }

        consumeArticleData(articleData, pageInfo)

//        条目缓存
//        Globals.room.pageContentCache().insertItem(PageContentCache(
//          pageName = pageName!!,
//          content = articleData,
//          pageInfo = pageInfo
//        ))
//        if (pageName != null && pageName != pageName!!) {
//          Globals.room.pageNameRedirect().insertItem(PageNameRedirect(
//            redirectName = if (pageKey is PageNameKey) pageKey.pageName.first() else "pageId::" + (pageKey as PageIdKey).pageId.first(),
//            pageName = pageName!!
//          ))
//        }
      } catch (e: MoeRequestException) {
        printRequestErr(e, "加载文章失败")
        val getCrossWikiTitleFromErrorMessageRegex = Regex(""""萌百:(.+?)"""")
        when {
          e is MoeRequestWikiException && e.code == "missingtitle" -> onArticleMissed?.invoke()
          e is MoeRequestWikiException && e.code == "invalidtitle" && e.message.contains(getCrossWikiTitleFromErrorMessageRegex) -> {
            val crossWikiTitle = getCrossWikiTitleFromErrorMessageRegex.find(e.message)?.groupValues?.get(1)
            if (crossWikiTitle != null) {
              openHttpUrl("https://zh.moegirl.org.cn/$crossWikiTitle")
              Globals.navController.popBackStack()
            } else {
              onArticleError?.invoke()
              status = LoadStatus.FAIL
            }
          }
        }

        val crossWikiMoegirlMsgRegex = Regex("""^Bad title "zhmoe:([\s\S]+)"\.$""")
        if (e is MoeRequestWikiException) {
          when {
            e.code == "missingtitle" -> {
              onArticleMissed?.invoke()
              return@launch
            }
            e.code == "invalidtitle" && e.message.contains(crossWikiMoegirlMsgRegex) -> {
              val moegirlPageName = crossWikiMoegirlMsgRegex.find(e.message)!!.groupValues[1]
              openHttpUrl(CrossWikiUrlPrefix.moegirl + moegirlPageName)
              Globals.navController.popBackStack()
              return@launch
            }
          }
        }

        onArticleError?.invoke()
        status = LoadStatus.FAIL
      }
    }
  }

  suspend fun disableAllMedia() {
    htmlWebViewRef.value!!.injectScript("""
      $disableAllIframeJsStr;
      $pauseAllAudioJsStr
    """.trimIndent())
  }

  suspend fun enableAllMedia() {
    htmlWebViewRef.value!!.injectScript(enableAllIframeJsStr)
  }

  suspend fun resetFonts() {
    val useSpecialCharSupportedFont = SettingsStore.common.getValue { this.useSpecialCharSupportedFontInArticle }.first()
    val useSerifFont = SettingsStore.common.getValue { useSerifFontInArticle }.first()
    val usingFonts = mutableListOf<String>().apply {
      if (useSpecialCharSupportedFont) add("NospzGothicMoe")
      if (useSerifFont) add("serif")
    }
      .joinToString(", ")

    htmlWebViewRef.value!!.injectScript("""
      document.body.style.fontFamily = '${if (usingFonts != "") usingFonts else "initial"}'
    """.trimIndent())
  }

  // 暂不使用了，移动版接口可以绕过waf
  // h萌娘在被waf拦截后所有资源需要带cookie，由于不同源cookie没法带过去，这里需要代理加载资源
  fun shouldInterceptRequest(webView: WebView, request: WebResourceRequest): WebResourceResponse? {
    if (request.url.toString().contains(Constants.domain.replace("https://", "")).not()) return null

    val urlStr = request.url.toString()
      .replace(Regex("""^file://"""), "https://")

    val okhttpRequest = Request.Builder()
      .url(urlStr)
      .build()

    val res = try {
      moeOkHttpClient.newCall(okhttpRequest).execute()
    } catch (e: Exception) {
      printRequestErr(e, "宿主代理webView加载资源失败")
      return null
    }

    val byteStream = res.body?.byteStream()

    return if (byteStream != null) {
      WebResourceResponse(
        res.headers["content-type"],
        "utf-8",
        byteStream
      )
    } else {
      null
    }
  }

  private val userConfig = ArticleViewUserConfig()
  suspend fun checkUserConfig() {
    val heimu = SettingsStore.common.getValue { this.heimu }.first()
    val useSpecialCharSupportedFont = SettingsStore.common.getValue { this.useSpecialCharSupportedFontInArticle }.first()
    val userSerifFont = SettingsStore.common.getValue { this.useSerifFontInArticle }.first()
    val hideTopTemplates = SettingsStore.common.getValue { this.hideTopTemplates }.first()
    // stopMediaOnLeave没法在这里处理，articleView不知道什么时候离开页面，这部分逻辑写在了articleScreen

    if (heimu != userConfig.heimu) {
      htmlWebViewRef.value!!.injectScript("moegirl.config.heimu.\$enabled = $heimu")
      userConfig.heimu = heimu
    }

    if (
      useSpecialCharSupportedFont != userConfig.useSpecialCharSupportedFont ||
      userSerifFont != userConfig.userSerifFont
    ) {
      resetFonts()
      userConfig.useSpecialCharSupportedFont = useSpecialCharSupportedFont
      userConfig.userSerifFont = userSerifFont
    }

    if (hideTopTemplates != userConfig.hideTopTemplates) {
      htmlWebViewRef.value!!.injectScript("moegirl.config.tops.\$hideTopInfoBox = $hideTopTemplates")
    }
  }

  val defaultMessageHandlers = createDefaultMessageHandlers()
}

class ArticleViewUserConfig(
  var heimu: Boolean = CommonSettings().heimu,
  var useSpecialCharSupportedFont: Boolean = CommonSettings().useSpecialCharSupportedFontInArticle,
  var userSerifFont: Boolean = CommonSettings().useSerifFontInArticle,
  var hideTopTemplates: Boolean = CommonSettings().hideTopTemplates
)

@ProguardIgnore
class MoegirlImage(
  val fileName: String = "",
  val title: String = "",
  var fileUrl: String,
)

const val disableAllIframeJsStr = """
  (() => {
    const iframeList = document.querySelectorAll('iframe')
    iframeList.forEach(item => {
      // 通过清空src的方式停止播放
      const src = item.src
      item.src = ''
      item.dataset.src = src
    })
  })()
"""

const val enableAllIframeJsStr = """
  (() => {
    const iframeList = document.querySelectorAll('iframe')
    iframeList.forEach(item => {
      item.src = item.dataset.src
    })
  })()
"""

const val pauseAllAudioJsStr = """
  (() => {
    const audioList = document.querySelectorAll('audio')
    audioList.forEach(item => item.pause())
  })()
"""

private val commonUrlRegex = if (Constants.source == DataSource.MOEGIRL)
  Regex("""https://img\.moegirl\.org\.cn/common/\S+""") else
  Regex("""https://www\.hmoegirl\.com/thumb\.php\S+""")
private suspend fun String.imageProxy(): String = withContext(Dispatchers.Default) {
  val doc = Jsoup.parse(this@imageProxy)
  fun replaceToProxyUrl(url: String) = url.replace(commonUrlRegex) { "/commonRes/" + URLEncoder.encode(it.value, "UTF-8") }
  fun Element.replaceSourceUrl() {
    val src = attr("src")
    val srcset = attr("srcset")
    val srcTestResult = src.contains(commonUrlRegex)
    val srcsetTestResult = srcset.contains(commonUrlRegex)
    if (!srcTestResult && !srcsetTestResult) return
    if (src != "") attr("src", replaceToProxyUrl(src))
    if (srcset != "") attr("srcset", replaceToProxyUrl(srcset))
  }

  doc.getElementsByTag("img").forEach { it.replaceSourceUrl() }
  doc.getElementsByTag("source").forEach { it.replaceSourceUrl() }
  doc.html()
}