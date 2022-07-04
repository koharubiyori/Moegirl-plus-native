package com.moegirlviewer.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

var needExitProcessOnExitApp = false

fun exitApp() = globalCoroutineScope.launch {
  Globals.activity.finishAndRemoveTask()
  if (needExitProcessOnExitApp) {
    delay(500)
    exitProcess(0)
  }
}