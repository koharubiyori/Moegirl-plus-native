package com.moegirlviewer.initialization

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref
import androidx.navigation.NavHostController
import coil.ImageLoaderFactory
import coil.compose.LocalImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.moegirlviewer.component.commonDialog.*
import com.moegirlviewer.request.imageOkHttpClient
import com.moegirlviewer.util.Globals
import my.google.accompanist.navigation.animation.rememberAnimatedNavController

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun OnComposeWillCreate(
  content: @Composable (NavHostController) -> Unit
) {
  val themeColors = MaterialTheme.colors
  val defaultImageLoader = LocalImageLoader.current
  val navController = rememberAnimatedNavController()
  val overScrollConfig = remember {
    OverscrollConfiguration(
      glowColor = themeColors.primaryVariant
    )
  }


  val imageLoader = remember {
    defaultImageLoader.newBuilder()
      .components {
        add(SvgDecoder.Factory())
        add(ImageDecoderDecoder.Factory())
      }
      .okHttpClient(imageOkHttpClient)
      .build()
  }
  val commonAlertDialogRef = remember { Ref<CommonAlertDialogRef>() }
  val commonAlertDialog2Ref = remember { Ref<CommonAlertDialogRef>() }  // 这里为了能显示最多两个全局共用Dialog所以弄成这样了，虽然有点丑
  val commonLoadingDialogRef = remember { Ref<CommonLoadingDialogRef>() }
  val commonDatePickerDialogState = remember { CommonDatePickerDialogState() }

  LaunchedEffect(true) {
    Globals.navController = navController
    Globals.imageLoader = imageLoader
    Globals.commonAlertDialog = commonAlertDialogRef.value!!
    Globals.commonAlertDialog2 = commonAlertDialog2Ref.value!!
    Globals.commonLoadingDialog = commonLoadingDialogRef.value!!
    Globals.commonDatePickerDialog = commonDatePickerDialogState
    onComposeCreated()
  }

  CompositionLocalProvider(
    LocalImageLoader provides imageLoader,
    LocalOverscrollConfiguration provides overScrollConfig,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(themeColors.background)
    ) {
      content(navController)
    }

    CommonDatePickerDialog(state = commonDatePickerDialogState)
    CommonAlertDialog(ref = commonAlertDialogRef)
    CommonAlertDialog(ref = commonAlertDialog2Ref)
    CommonLoadingDialog(ref = commonLoadingDialogRef)
  }
}