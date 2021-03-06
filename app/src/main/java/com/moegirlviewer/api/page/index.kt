package com.moegirlviewer.api.page

import com.moegirlviewer.Constants
import com.moegirlviewer.api.page.bean.PageProfileBean
import com.moegirlviewer.api.page.bean.*
import com.moegirlviewer.request.MoeRequestMethod
import com.moegirlviewer.request.moeRequest
import com.moegirlviewer.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object PageApi {
  suspend fun getTruePageName(pageKey: PageKey) : String? {
    val res = moeRequest(
      entity = PageInfoResBean::class.java,
      params = mutableMapOf<String, Any>().apply {
        this["action"] = "query"
        this["converttitles"] = "1"
        addQueryApiParamsByPageKey(pageKey)
      }
    )

    return res.query.pages.values.toList().first().title
  }

  suspend fun getPageContent(
    pageKey: PageKey,
    revId: Int? = null,
    previewMode: Boolean = false,
  ) = moeRequest(
    entity = PageContentResBean::class.java,
    params = mutableMapOf<String, Any>().apply {
      this["action"] = "parse"
      this["prop"] = "text|categories|templates|sections|images|displaytitle"
      if (previewMode) this["preview"] = "1"
      if (revId == null) {
        when (pageKey) {
          is PageIdKey -> this["pageid"] = pageKey.triedPageIdOrNull!!
          is PageNameKey -> {
            this["page"] = pageKey.triedPageNameOrNull!!
            this["redirects"] = "1"
          }
        }
      } else {
        this["oldid"] = revId
      }
    }
  )

  suspend fun getMainImageAndIntroduction(
    pageKey: PageKey,
    size: Int = 500
  ) = moeRequest(
    entity = MainImagesAndIntroductionResBean::class.java,
    params = mutableMapOf<String, Any>().apply {
      this["action"] = "query"
      this["prop"] = "extracts|pageimages"
      addQueryApiParamsByPageKey(pageKey)
      this["pithumbsize"] = size.toString()
      this["exsentences"] = "10"
      this["exlimit"] = "max"
      this["exintro"] = 1
      this["explaintext"] = 1
      this["exsectionformat"] = "plain"
    }
  )

  suspend fun getImagesUrl(imageNames: List<String>): Map<String, String> = coroutineScope {
     val defers = imageNames.chunked(50).map {
      async {
        moeRequest(
          entity = ImageInfoResBean::class.java,
          method = MoeRequestMethod.POST,
          params = mutableMapOf<String, Any>().apply {
            this["action"] = "query"
            this["prop"] = "imageinfo"
            this["iiprop"] = "url"
            this["titles"] = it.joinToString("|") { "${Constants.filePrefix}$it" }
          }
        )
      }
    }

    defers
      .map { it.await() }
      .flatMap { (it.query?.pages?.values) ?: emptyList() }
      .fold(mutableMapOf()) { result, item ->
        val fileName = item.title.replaceFirst(Constants.filePrefix, "")
        if (item.imageinfo != null && item.imageinfo.isNotEmpty()) {
          val fileUrl = item.imageinfo[0].url
          result[fileName] = fileUrl
        }
        result
      }
  }

  suspend fun getPageInfo(pageKey: PageKey): PageInfoResBean.Query.MapValue {
    val res = moeRequest(
      entity = PageInfoResBean::class.java,
      params = mutableMapOf<String, Any>().apply {
        this["action"] = "query"
        this["prop"] = "info"
        this["inprop"] = "protection|watched|talkid"
        addQueryApiParamsByPageKey(pageKey)
      }
    )

    return res.query.pages.values.toList().first()
  }

  suspend fun getRandomPage(
    count: Int = 1,
    mainImageSize: Int = Globals.activity.resources.displayMetrics.widthPixels,
    continueKey: String? = null
  ) = moeRequest(
    entity = RandomPageResBean::class.java,
    params = mutableMapOf<String, Any>().apply {
      this["action"] = "query"
      this["prop"] = "extracts|pageimages"
      this["generator"] = "random"
      this["exsentences"] = "10"
      this["exlimit"] = "max"
      this["exintro"] = 1
      this["explaintext"] = 1
      this["exsectionformat"] = "plain"
      this["grnnamespace"] = "0"
      this["grnfilterredir"] = "nonredirects"
      this["pithumbsize"] = mainImageSize
      this["grnlimit"] = count
      if (continueKey != null) {
        this["continue"] = "grncontinue||"
        this["grncontinue"] = continueKey
      }
    }
  )

  suspend fun getPageProfile(pageKey: PageKey) = moeRequest(
    entity = PageProfileBean::class.java,
    params = mutableMapOf<String, Any>().apply {
      this["action"] = "query"
      this["prop"] = "pageimages|extracts"
      addQueryApiParamsByPageKey(pageKey)
      this["pithumbsize"] = 500
      this["exsentences"] = 10
      this["exlimit"] = 20
      this["exintro"] = 1
      this["explaintext"] = 1
    }
  )
}