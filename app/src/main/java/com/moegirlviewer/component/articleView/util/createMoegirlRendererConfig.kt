package com.moegirlviewer.component.articleView.util

import com.google.gson.Gson
import com.moegirlviewer.Constants

fun createMoegirlRendererConfig(
  pageName: String? = null,
  language: String,
  site: String,
  enabledCategories: Boolean,
  categories: List<String>,
  enabledHeightObserver: Boolean,
  heimu: Boolean,
  nightMode: Boolean,
  addCopyright: Boolean,
  isTalkPage: Boolean,
  hideTopInfoBox: Boolean,
): String {
  val categoriesStr = Gson().toJson(categories)

  val heightObserverCodes = """
    moegirl.config.hostScrollMode.enabled = true
    moegirl.config.hostScrollMode.onResize = height => _postMessage('pageHeightChange', { value: height })
  """.trimIndent()

  return """
    moegirl.data.pageName = ${Gson().toJson(pageName)}
    moegirl.data.language = '$language'
    moegirl.data.site = '$site'
    moegirl.data.pageType = '${if (isTalkPage) "talk" else "article"}'
    moegirl.data.mainUrl = '${Constants.mainUrl}'
    moegirl.config.heimu.${'$'}enabled = $heimu
    moegirl.config.addCopyright.enabled = $addCopyright
    moegirl.config.nightTheme.${'$'}enabled = $nightMode
    moegirl.config.tops.${'$'}hideTopInfoBox = $hideTopInfoBox
  
    moegirl.config.link.onClick = (data) => _postMessage('link', data)
    moegirl.config.biliPlayer.onClick = (data) => _postMessage('biliPlayer', data)
    moegirl.config.biliPlayer.onLongPress = (data) => _postMessage('biliPlayerLongPress', data)
    moegirl.config.request.onRequested = (data) => _postMessage('request', data)
    moegirl.config.vibrate.onCalled = () => _postMessage('vibrate')
    moegirl.config.addCategories.enabled = $enabledCategories
    moegirl.config.addCategories.categories = $categoriesStr
    moegirl.config.dataCollector.catalogData = data => _postMessage('catalogData', { value: data })
    moegirl.config.poll.onPoll = (data) => _postMessage('poll', data)
    ${if (enabledHeightObserver) heightObserverCodes else ""}
    moegirl.init()
  """.trimIndent()
}