package com.moegirlviewer.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moegirlviewer.util.Globals

@Composable
fun AppHeaderIcon(
  modifier: Modifier = Modifier,
  image: ImageVector,
  iconSize: Dp = 30.dp,
  iconColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
  onClick: () -> Unit,
) {
  IconButton(
    onClick = onClick
  ) {
    Icon(
      modifier = Modifier
        .width(iconSize)
        .height(iconSize)
        .then(modifier),
      imageVector = image,
      contentDescription = null,
      tint = iconColor,
    )
  }
}

@Composable
fun BackButton(
  modifier: Modifier = Modifier,
  iconColor: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
  AppHeaderIcon(
    modifier = Modifier
      .then(modifier),
    image = Icons.Filled.ArrowBack,
    iconColor = iconColor,
    onClick = { Globals.navController.popBackStack() }
  )
}