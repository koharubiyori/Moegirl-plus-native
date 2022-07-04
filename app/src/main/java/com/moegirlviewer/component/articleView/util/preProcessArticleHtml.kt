package com.moegirlviewer.component.articleView.util

import com.moegirlviewer.Constants
import com.moegirlviewer.util.isMoegirl
import com.moegirlviewer.util.printDebugLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

suspend fun preProcessArticleHtml(html: String): String = withContext(Dispatchers.Default) {
  if (!Constants.straightConnectMode) return@withContext html

  val assetsUrl = isMoegirl(
    "https://img.moegirl.org.cn/common/",
    "https://www.hmoegirl.com/"
  )

  // const replaceToProxyUrl = (url) => '/commonRes/' + encodeURIComponent(url)
  fun String.replaceToProxyUrl() = this.replace("https://www.hmoegirl.com", Constants.mainUrl)

  val doc = Jsoup.parse(html)
  doc.body().select("source, img").forEach {
    val src = it.attr("src")
    val srcset = it.attr("srcset")
    val srcTestResult = src.contains(assetsUrl)
    val srcsetTestResult = srcset.contains(assetsUrl)
    if (!srcTestResult && !srcsetTestResult) return@forEach
    if (src != "") it.attr("src", src.replaceToProxyUrl())
    if (srcset != "") it.attr("srcset", srcset.replaceToProxyUrl())
  }

  return@withContext doc.html()
}