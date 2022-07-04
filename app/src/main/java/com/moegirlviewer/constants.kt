package com.moegirlviewer

import com.moegirlviewer.store.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SourceConstants(
  val source: DataSource,
  val domain: String,
  val mainUrl: String,
  val mainPageUrl: String,
  val apiUrl: String,
  val avatarUrl: String,
  val shareUrl: String,
  val registerUrl: String,
  val filePrefix: String,
  val disclaimerPageName: String,
  val privacyPageName: String
)

private const val moegirlMainUrl = "https://mzh.moegirl.org.cn"

object CrossWikiUrlPrefix {
  const val moegirl = "$moegirlMainUrl/"
}

private val isStraightConnectMode = if (BuildConfig.FLAVOR_source == "hmoe") runBlocking {
  SettingsStore.common.getValue { straightConnectMode }.first()
} else false

private val hmoeMainUrl = if (isStraightConnectMode)
  "https://hmoe-relay.koharu.top" else
  "https://m.hmoegirl.com"

private val sourceConstants = when (BuildConfig.FLAVOR_source) {
  "moegirl" -> SourceConstants(
    source = DataSource.MOEGIRL,
    domain = "https://moegirl.org.cn",
    mainUrl = moegirlMainUrl,
    mainPageUrl = "https://mzh.moegirl.org.cn/Mainpage",
    apiUrl = "https://mzh.moegirl.org.cn/api.php",
    avatarUrl = "https://commons.moegirl.org.cn/extensions/Avatar/avatar.php?user=",
    shareUrl = "https://mzh.moegirl.org.cn/index.php?curid=",
    registerUrl = "https://mzh.moegirl.org.cn/index.php?title=Special:创建账户",
    filePrefix = "File:",
    disclaimerPageName = "萌娘百科:免责声明",
    privacyPageName = "萌娘百科:隐私权政策",
  )

  "hmoe" -> SourceConstants(
    source = DataSource.HMOE,
    domain = "https://hmoegirl.com",
    mainUrl = hmoeMainUrl,
    mainPageUrl = "https://m.hmoegirl.com/Mainpage",
    apiUrl = "$hmoeMainUrl/api.php",
    avatarUrl = "https://m.hmoegirl.com/extensions/Avatar/avatar.php?user=",
    shareUrl = "https://m.hmoegirl.com/index.php?curid=",
    registerUrl = "https://m.hmoegirl.com/index.php?title=%E7%89%B9%E6%AE%8A:%E5%88%9B%E5%BB%BA%E8%B4%A6%E6%88%B7&returnto=Mainpage",
    filePrefix = "文件:",
    disclaimerPageName = "H萌娘:免责声明",
    privacyPageName = "H萌娘:隐私政策",
  )

  else -> error("数据源'${BuildConfig.FLAVOR_source}'的常量集未配置！")
}

object Constants {
  val source = sourceConstants.source
  val domain = sourceConstants.domain
  val mainUrl = sourceConstants.mainUrl
  val mainPageUrl = sourceConstants.mainPageUrl
  val apiUrl = sourceConstants.apiUrl
  val avatarUrl = sourceConstants.avatarUrl
  val shareUrl = sourceConstants.shareUrl
  val registerUrl = sourceConstants.registerUrl
//  val appDownloadUrl = sourceConstants.appDownloadUrl
  val filePrefix = sourceConstants.filePrefix
  val disclaimerPageName = sourceConstants.disclaimerPageName
  val privacyPageName = sourceConstants.privacyPageName

  val straightConnectMode = isStraightConnectMode
  const val topAppBarHeight = 56
  val targetStore = if (BuildConfig.FLAVOR_targetStore == "common")
    TargetStore.COMMON else TargetStore.FDROID
}

enum class DataSource(val code: String) {
  MOEGIRL("moegirl"),
  HMOE("hmoe")
}

enum class DataStoreName {
  ACCOUNT,
  SEARCH_RECORDS,
  SETTINGS,
}

enum class TargetStore {
  COMMON,
  FDROID
}