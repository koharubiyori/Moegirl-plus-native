package com.moegirlviewer.screen.drawer.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moegirlviewer.R
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.screen.drawer.CommonDrawerState
import com.moegirlviewer.theme.text
import com.moegirlviewer.util.Globals
import com.moegirlviewer.util.exitApp
import com.moegirlviewer.util.noRippleClickable
import kotlinx.coroutines.launch

@Composable
fun CommonDrawerFooter(
  commonDrawerState: CommonDrawerState
) {
  val themeColors = MaterialTheme.colors
  val density = LocalDensity.current.density
  val scope = rememberCoroutineScope()

  Row(
    modifier = Modifier
      .height(45.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .noRippleClickable {
          scope.launch { commonDrawerState.close() }
          Globals.navController.navigate("settings")
        },
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        modifier = Modifier
          .padding(end = 10.dp)
          .width(22.dp)
          .height(22.dp),
        imageVector = Icons.Filled.Settings,
        contentDescription = null,
        tint = themeColors.text.secondary
      )

      StyledText(
        text = stringResource(R.string.settings),
        color = themeColors.text.secondary
      )
    }

    Spacer(modifier = Modifier
      .width((1 / density).dp)
      .height(30.dp)
      .background(themeColors.text.secondary)
    )

    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .noRippleClickable {
          exitApp()
        },
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        modifier = Modifier
          .padding(end = 10.dp)
          .width(22.dp)
          .height(22.dp),
        imageVector = Icons.Filled.SubdirectoryArrowLeft,
        contentDescription = null,
        tint = themeColors.text.secondary
      )

      StyledText(
        text = stringResource(R.string.exit),
        color = themeColors.text.secondary
      )
    }
  }
}