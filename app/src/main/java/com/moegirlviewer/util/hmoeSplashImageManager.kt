package com.moegirlviewer.util

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.os.Environment
import com.google.gson.Gson
import com.moegirlviewer.R
import com.moegirlviewer.api.app.AppApi
import com.moegirlviewer.api.app.bean.HmoeSplashImageConfigBean
import com.moegirlviewer.request.CommonRequestException
import com.moegirlviewer.request.commonOkHttpClient
import com.moegirlviewer.request.moeOkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.time.LocalDate
import kotlin.math.absoluteValue

private const val configFileName = "config.json"

@SuppressLint("UseCompatLoadingForDrawables")
object HmoeSplashImageManager {
  private val rootDir = File(
    Globals.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "splashImages"
  )
  private val configFile = File(rootDir, configFileName)
  private lateinit var config: HmoeSplashImageConfigBean

  init {
    if (rootDir.exists().not()) rootDir.mkdirs()
    if (configFile.exists()) {
      val configJson = configFile.readText()
      if (configJson != "") {
        config = Gson().fromJson(configJson, HmoeSplashImageConfigBean::class.java)
      }
    } else {
      configFile.createNewFile()
    }
  }

  suspend fun getRandomImage(): SplashImage = withContext(Dispatchers.IO) {
    val localImages = rootDir.listFiles { _, fileName -> fileName != configFileName }!!
    val localImagesMap = localImages.associateBy { it.name }
    val fallbackImage = R.mipmap.splash_fallback

    val usableImages = if (this@HmoeSplashImageManager::config.isInitialized) {
      config.checkFestivalImages(localImagesMap)
        ?: (
          config.images
            .asSequence()
            .filter { !it.disabled }
            .map { it.imageUrl.localImageFileName() }
            .filter { localImagesMap.containsKey(it) }
            .map { localImagesMap[it] }
            .toList() + listOf(fallbackImage)
          )
    } else {
      localImagesMap.values
    }

    val randomImage = usableImages.randomOrNull() ?: fallbackImage
    SplashImage.onlyUseInSplashScreen(randomImage)
  }

  suspend fun loadConfig() = withContext(Dispatchers.IO) {
    try {
      config = AppApi.getHmoeSplashImageConfig()
      configFile.writeText(Gson().toJson(config))
    } catch (e: CommonRequestException) {
      printRequestErr(e, "H萌娘：读取启动屏图片配置失败")
    }
  }

  suspend fun syncImagesByConfig() = withContext(Dispatchers.IO) {
    if (this@HmoeSplashImageManager::config.isInitialized.not()) {
      printPlainLog("H萌娘：同步启动屏图片，但没有找到config")
      return@withContext
    }

    val allReferencedImageUrls = config.images.map { it.imageUrl } +
      config.festivals.flatMap { it.imageUrls }

    // 下载线上配置新增的图片
    allReferencedImageUrls
      .filter { imageUrl -> rootDir.existsChild(imageUrl.localImageFileName()).not() }
      .map { imageUrl ->
        launch {
          val request = Request.Builder()
            .url(imageUrl)
            .build()
          val res = try {
            moeOkHttpClient.newCall(request).execute()
          } catch (e: Exception) {
            printRequestErr(e, "H萌娘：启动屏图片下载失败：$imageUrl")
            return@launch
          }

          if (!res.isSuccessful) return@launch
          val imageByteArray = res.body!!.bytes()

          val file = File(rootDir, imageUrl.localImageFileName())
          file.createNewFile()
          file.writeBytes(imageByteArray)
          printPlainLog("H萌娘：启动屏图片下载完毕：$imageUrl")
        }
      }
      .forEach { it.join() }

    printPlainLog("H萌娘：启动屏图片全部下载完毕")

    // 清除线上配置已经不存在的图片
    val allReferencedImageNames = allReferencedImageUrls.map { it.localImageFileName() }
    val localImages = rootDir.listFiles { _, fileName -> fileName != configFileName }!!
    localImages
      .filter { allReferencedImageNames.contains(it.name).not() }
      .forEach { it.deleteOnExit() }

    printPlainLog("H萌娘：无用启动屏图片清理完毕")
  }
}

private fun HmoeSplashImageConfigBean.checkFestivalImages(
  localImageFiles: Map<String, File>
): List<String>? {
  val localDate = LocalDate.now()
  val foundFestival = this.festivals.firstOrNull {
    val (month, date) = it.date.split("-")
    !it.disabled &&
      localDate.month.value == month.toInt() &&
      localDate.dayOfMonth.absoluteValue == date.toInt()
  }

  val localFestivalImagePaths = foundFestival?.imageUrls?.mapNotNull {
    val festivalImageLocalName = it.localImageFileName()
    localImageFiles[festivalImageLocalName]?.path
  }

  return if (localFestivalImagePaths != null && localFestivalImagePaths.isNotEmpty()) localFestivalImagePaths else null
}

private fun String.localImageFileName() = computeMd5(this)