package com.moegirlviewer.screen.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

class SplashScreenState(
  // compose的动画需要compose的协程上下文
  private val composeCoroutineScope: CoroutineScope
) {
  internal val contentAlpha = Animatable(1f)
  internal val imageScale = Animatable(1.2f)

  suspend fun showAppearAnimation() = withContext(composeCoroutineScope.coroutineContext) {
    imageScale.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = 3000,
      )
    )
  }

  suspend fun showTransparentAnimation() = withContext(composeCoroutineScope.coroutineContext) {
    contentAlpha.animateTo(
      targetValue = 0f,
      animationSpec = tween(
        durationMillis = 350
      )
    )
  }
}