package com.moegirlviewer.screen.category.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import com.moegirlviewer.R
import com.moegirlviewer.api.category.bean.CategorySearchResultBean
import com.moegirlviewer.compable.remember.rememberImageRequest
import com.moegirlviewer.component.RippleColorScope
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.theme.text
import kotlin.math.max

@Composable
fun CategoryScreenItem(
  pageName: String,
  thumbnail: CategorySearchResultBean.Query.MapValue.Thumbnail?,
  categories: List<String>,
  onClick: () -> Unit,
  onCategoryClick: (categoryName: String) -> Unit,
) {
  val themeColors = MaterialTheme.colors

  Surface(
    modifier = Modifier
      .padding(start = 10.dp, end = 10.dp, top = 10.dp)
      .clickable { onClick() },
    elevation = 1.dp,
    shape = RoundedCornerShape(1)
  ) {
    Row() {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        StyledText(
          text = pageName,
          fontSize = 17.sp
        )

        RippleColorScope(color = Color.White) {
          FlowRow(
            modifier = Modifier
              .padding(top = 10.dp)
          ) {
            for (item in categories) {
              StyledText(
                modifier = Modifier
                  .padding(end = 5.dp, bottom = 5.dp)
                  .clickable { onCategoryClick(item) }
                  .background(themeColors.secondary)
                  .padding(3.dp)
                ,
                text = item,
                color = themeColors.onSecondary,
                fontSize = 10.sp
              )
            }
          }
        }
      }

      if (thumbnail != null) {
        AsyncImage(
          modifier = Modifier
            .width(120.dp)
            .height(max(150f, (120f / thumbnail.width * thumbnail.height)).dp),
          model = rememberImageRequest(data = thumbnail.source),
          contentDescription = null,
        )
      } else {
        Box(
          modifier = Modifier
            .width(120.dp)
            .height(150.dp)
            .background(Color(0xffe2e2e2)),
          contentAlignment = Alignment.Center
        ) {
          StyledText(
            text = stringResource(id = R.string.noImage),
            color = themeColors.text.secondary,
            fontSize = 18.sp
          )
        }
      }
    }
  }
}