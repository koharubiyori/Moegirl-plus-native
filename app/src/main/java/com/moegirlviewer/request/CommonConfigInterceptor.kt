package com.moegirlviewer.request

import com.moegirlviewer.Constants
import com.moegirlviewer.util.Globals
import com.moegirlviewer.util.isMoegirl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CommonConfigInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val newRequest = chain
      .withConnectTimeout(10, TimeUnit.SECONDS)
      .withReadTimeout(10, TimeUnit.SECONDS)
      .withWriteTimeout(10, TimeUnit.SECONDS)
      .request().newBuilder()
      .addHeader("user-agent", Globals.httpUserAgent)
      .addHeader("sec-ch-ua", "\"Chromium\";v=\"100\", \" Not A;Brand\";v=\"99\"")
      .addHeader("sec-ch-ua-mobile", "?0")
      .addHeader("sec-ch-ua-platform", "Windows")
      .addHeader("sec-fetch-dest", "empty")
      .addHeader("sec-fetch-mode", "cors")
      .addHeader("sec-fetch-site", "same-origin")
      .addHeader("x-requested-with", "XMLHttpRequest")
      .addHeader("dnt", "1")
      .addHeader("accept", "*/*")
      .addHeader("Referer", Constants.mainUrl)

    if (!isMoegirl()) {
      val originalRequest = chain.request()
      val newUrl = Constants.mainUrl +
        originalRequest.url.encodedPath +
        (originalRequest.url.encodedQuery?.let { "?$it" } ?: "")
      newRequest.url(newUrl)
    }

    return chain.proceed(newRequest.build())
  }
}
