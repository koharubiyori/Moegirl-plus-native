package com.moegirlviewer.util

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moegirlviewer.Constants
import com.moegirlviewer.DataSource
import com.moegirlviewer.R
import com.moegirlviewer.screen.article.ArticleRouteArguments
import kotlinx.coroutines.*
import okhttp3.internal.toHexString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess

@Target(AnnotationTarget.CLASS)
annotation class ProguardIgnore

val globalCoroutineScope = CoroutineScope(Dispatchers.Main)

fun isMoegirl() = Constants.source == DataSource.MOEGIRL
fun isTalkPage(pageName: String?) = (pageName ?: "").contains(Regex("""
  ^([Tt]alk|讨论|討論|[Tt]emplate( talk|)|模板(讨论|討論|)|[Mm]odule( talk|)|模块(讨论|討論|)|[Cc]ategory( talk|)|分[类類](讨论|討論|)|[Uu]ser talk|用户讨论|用戶討論|萌娘百科 talk|H萌娘讨论):
""".trimIndent()))

fun <T> isMoegirl(ifIsTrue: T, ifIsFalse: T): T {
  return if (isMoegirl()) ifIsTrue else ifIsFalse
}

fun isDebugEnv() = (Globals.context.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE) != 0

fun openHttpUrl(url: String) {
  val intent = Intent(Intent.ACTION_VIEW)
  intent.data = Uri.parse(url)
  Globals.activity.startActivity(intent)
}

// 主要用于在向webView注入脚本之前，将脚本内容转义
fun String.toUnicodeForInjectScriptInWebView(): String {
  fun encode(char: Char) = "\\u" + char.code.toHexString().padStart(4, '0')
  return this.toCharArray().joinToString("") { encode(it) }
}

fun shareText(content: String) {
  val intent = Intent(Intent.ACTION_SEND)
  intent.type = "text/plain"
  intent.putExtra(Intent.EXTRA_TEXT, content)
  val shareText = Globals.context.getString(R.string.share)
  Globals.activity.startActivity(Intent.createChooser(intent, shareText))
}

fun shareImage(
  imageUri: Uri,
  type: String = "image/png"
) {
  val intent = Intent(Intent.ACTION_SEND)
  intent.type = type
  intent.putExtra(Intent.EXTRA_STREAM, imageUri)
  val shareText = Globals.context.getString(R.string.share)
  Globals.activity.startActivity(Intent.createChooser(intent, shareText))
}

@Composable
fun Int.toDp(): Dp {
  return (this / LocalDensity.current.density).dp
}

@Composable
fun Float.toDp(): Dp {
  return (this / LocalDensity.current.density).dp
}

fun getTextFromHtml(html: String): String {
  return html
    .replace(Regex("""(<.+?>|<\\/.+?>)"""), "")
    .replace(Regex("&(.+?);")) {
      mapOf(
        "gt" to ">",
        "lt" to "<",
        "amp" to "&"
      )[it.groupValues[1]] ?: it.groupValues[0]
    }
    .trim()
}

fun copyContentToClipboard(content: String) {
  val cm = Globals.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clipData = ClipData.newPlainText("", content)
  cm.setPrimaryClip(clipData)
}

fun vibrate() {
  val vibrator = Globals.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
  if (Build.VERSION.SDK_INT >= 26) {
    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
  } else {
    vibrator.vibrate(50)
  }
}

fun gotoArticlePage(pageName: String) {
  Globals.navController.navigate(ArticleRouteArguments(
    pageKey = PageNameKey(pageName)
  ))
}

fun gotoUserPage(userName: String) {
  gotoArticlePage("User:$userName")
}

fun Char.isHalfWidth(): Boolean {
  return this.toString().contains(Regex("[\\u0000-\\u00ff]"))
}

fun Long.toLocalDateTime(): LocalDateTime {
  return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun Long.toLocalDate(): LocalDate {
  return this.toLocalDateTime().toLocalDate()
}

fun LocalDateTime.toEpochMilli(): Long {
  return this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun LocalDate.toEpochMilli(): Long {
  return this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun isTraditionalChineseEnv() = listOf("zh-HK", "zh-MO", "zh-TW", "zh-Hant-HK", "zh-Hant-MO", "zh-Hant-tw")
  .contains(Locale.getDefault().toLanguageTag())

fun computeMd5(content: String): String {
  val md = MessageDigest.getInstance("MD5")
  return BigInteger(1, md.digest(content.toByteArray())).toString(16).padStart(32, '0')
}

val moegirlNormalTimestampDateFormatter: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

// 萌百通常api返回这种格式的时间戳，评论api返回秒数时间戳(注意不是毫秒)
fun parseMoegirlNormalTimestamp(timestamp: String): LocalDateTime {
  // 这里不知道为什么DateTimeFormatter.ISO_INSTANT不行，看文档明明应该可以的
  return LocalDateTime.parse(timestamp, moegirlNormalTimestampDateFormatter).plusHours(8)
}

fun Date.toLocalDate(): LocalDate {
  val instant = this.toInstant()
  val zoneId = ZoneId.systemDefault()
  return instant.atZone(zoneId).toLocalDate()
}

fun LocalDate.toDate(): Date {
  val zoneId = ZoneId.systemDefault()
  val zdt: ZonedDateTime = this.atStartOfDay(zoneId)
  return Date.from(zdt.toInstant())
}

fun TextGeometricTransform.Companion.Italic() = TextGeometricTransform(
  skewX = -0.3f
)

fun File.existsChild(fileName: String) = this.list { _, existsFileName -> existsFileName == fileName }!!.isNotEmpty()

@Throws(IOException::class)
fun InputStream.readAllBytes(): ByteArray {
  val bufLen = 4 * 0x400 // 4KB
  val buf = ByteArray(bufLen)
  var readLen: Int = 0

  ByteArrayOutputStream().use { o ->
    this.use { i ->
      while (i.read(buf, 0, bufLen).also { readLen = it } != -1)
        o.write(buf, 0, readLen)
    }

    return o.toByteArray()
  }
}

fun <T> List<T>.randomList(count: Int): List<T> {
  if (this.size <= count) return this
  val randomIndexes = mutableListOf<Int>()
  val randomRange = 0 until this.size
  while (randomIndexes.size != count) {
    val randomIndex = randomRange.random()
    if (randomIndexes.contains(randomIndex).not()) randomIndexes.add(randomIndex)
  }
  return randomIndexes.map { this[it] }
}

val categoryPageNamePrefixRegex = Regex("^([Cc]ategory|分类|分類):")

fun closeKeyboard() {
  val inputMethodService = Globals.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  val windowToken = Globals.activity.window.currentFocus?.windowToken
  if (windowToken != null) {
    inputMethodService.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
  }
}

fun showDebugAlert(message: String) {
  AlertDialog.Builder(Globals.activity)
    .setTitle("debug")
    .setMessage(message)
    .show()
}