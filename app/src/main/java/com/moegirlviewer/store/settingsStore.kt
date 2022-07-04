package com.moegirlviewer.store

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.moegirlviewer.DataStoreName
import com.moegirlviewer.screen.article.ReadingRecord
import com.moegirlviewer.screen.home.component.newPagesCard.NewPagesCardViewMode
import com.moegirlviewer.screen.splashSetting.SplashImageMode
import com.moegirlviewer.util.Globals
import com.moegirlviewer.util.isMoegirl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 这里保存的是app中所有的设置项，不仅仅是settings页面中的设置
// 每个设置项组应该继承Settings，所有成员使用var声明，带有默认值，之后在SettingsStore中声明client
sealed class Settings

// 设置页面里的设置项
data class CommonSettings(
  var heimu: Boolean = true,
  var stopMediaOnLeave: Boolean = false,
  var syntaxHighlight: Boolean = true,
  var darkThemeBySystem: Boolean = false,
  var useSpecialCharSupportedFontInApp: Boolean = false,
  var useSpecialCharSupportedFontInArticle: Boolean = false,
  var useSerifFontInArticle: Boolean = false,
  var usePureTheme: Boolean = !isMoegirl(),
  var splashImageMode: SplashImageMode = SplashImageMode.NEW,
  var selectedSplashImages: List<String> = emptyList(),
  var lightRequestMode: Boolean = false,  // 轻请求模式，这个选项主要作用是减少请求数，将一些预加载的东西改为点击时才加载。现在这个选项的ui已经隐藏，默认为开启
  var focusMode: Boolean = false,
  var hideTopTemplates: Boolean = false,
  var straightConnectMode: Boolean = false,   // 直连模式，只有hmoe会用到，不过哪天萌百也要用了也说不定
) : Settings()

// 最近更改右上角按钮打开的设置
data class RecentChangesSettings(
  var daysAgo: Int = 7,
  var totalLimit: Int = 500,
  var includeSelf: Boolean = true,
  var includeMinor: Boolean = true,
  var includeRobot: Boolean = false,
  var includeLog: Boolean = false,
  var isWatchListMode: Boolean = false
) : Settings()

//  var randomPage: Boolean = true,
data class CardsHomePageSettings(
  var newPagesCardViewMode: NewPagesCardViewMode = NewPagesCardViewMode.LIST,
  var useCardsHome: Boolean = true,
) : Settings()

// 一些杂项，基本不算是设置，只是用来记录的持久化变量
data class OtherSettings(
  // 拒绝的版本，在给用户提示有更新后，如果用户点击拒绝，则记录到这里
  var rejectedVersionName: String = "",
  // 用户上次的阅读记录
  var readingRecord: ReadingRecord? = null
) : Settings()

object SettingsStore {
  val common = SettingsStoreClient(CommonSettings::class.java)
  val recentChanges = SettingsStoreClient(RecentChangesSettings::class.java)
  val other = SettingsStoreClient(OtherSettings::class.java)
  val cardsHomePage = SettingsStoreClient(CardsHomePageSettings::class.java)
}

private val Context.dataStore by preferencesDataStore(DataStoreName.SETTINGS.name)
private val dataStore get() = Globals.context.dataStore

class SettingsStoreClient<T : Settings>(
  private val entity: Class<out T>
) {
  private val preferencesKey = stringPreferencesKey(entity.simpleName)

  fun <SelectedValue> getValue(
    getter: (T.() -> SelectedValue)
  ) = dataStore.data.map {
    val jsonStr = it[preferencesKey]
    val entityInstance = if (jsonStr != null) Gson().fromJson(jsonStr, entity) else entity.newInstance()
    getter.invoke(entityInstance)
  }

  suspend fun setValue(setter: T.() -> Unit) {
    val jsonStr = dataStore.data.first()[preferencesKey]
    val entityInstance = if (jsonStr != null) Gson().fromJson(jsonStr, entity) else entity.newInstance()
    setter.invoke(entityInstance)
    dataStore.updateData {
      it.toMutablePreferences().apply { this[preferencesKey] = Gson().toJson(entityInstance) }
    }
  }

  suspend fun setValue(value: T) {
    dataStore.updateData {
      it.toMutablePreferences().apply { this[preferencesKey] = Gson().toJson(value) }
    }
  }
}

