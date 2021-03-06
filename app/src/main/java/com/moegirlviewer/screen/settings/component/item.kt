package com.moegirlviewer.screen.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.theme.background2
import com.moegirlviewer.theme.text
import com.moegirlviewer.util.BorderSide
import com.moegirlviewer.util.sideBorder

@Composable
fun SettingsScreenItem(
  title: String,
  titleStyle: TextStyle = TextStyle(),
  subtext: String? = null,
  innerVerticalPadding: Boolean = true,
  visibleBorder: Boolean = true,
  onClick: (() -> Unit)? = null,
  rightContent: (@Composable () -> Unit)? = null,
) {
  val themeColors = MaterialTheme.colors

  Surface(
    modifier = Modifier
      .then(if (visibleBorder) Modifier
        .sideBorder(BorderSide.BOTTOM, 1.dp, themeColors.background2)
      else Modifier)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick?.invoke() }
        .padding(
          vertical = if (innerVerticalPadding) 10.dp else 0.dp,
          horizontal = 15.dp
        ),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier
          .weight(1f),
      ) {
        StyledText(
          text = title,
          color = themeColors.text.primary,
          style = titleStyle,
          fontSize = 15.sp
        )
        if (subtext != null) {
          StyledText(
            modifier = Modifier
              .padding(top = 3.dp),
            text = subtext,
            fontSize = 12.sp,
            color = themeColors.text.secondary
          )
        }
      }

      rightContent?.invoke()
    }
  }
}