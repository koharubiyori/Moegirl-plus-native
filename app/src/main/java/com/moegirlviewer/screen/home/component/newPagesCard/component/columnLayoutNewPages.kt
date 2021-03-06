package com.moegirlviewer.screen.home.component.newPagesCard.component

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moegirlviewer.R
import com.moegirlviewer.api.editingRecord.bean.NewPagesBean
import com.moegirlviewer.api.page.bean.PageProfileBean
import com.moegirlviewer.compable.remember.rememberFromMemory
import com.moegirlviewer.compable.remember.rememberImageRequest
import com.moegirlviewer.component.RippleColorScope
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.theme.background2
import com.moegirlviewer.theme.text
import com.moegirlviewer.util.Globals
import com.moegirlviewer.util.gotoArticlePage

@Composable
fun ColumnLayoutNewPages(
  pageList: List<PageProfileBean.Query.MapValue>,
  onMoreButtonClick: () -> Unit
) {
  val themeColors = MaterialTheme.colors
  val scrollState = rememberFromMemory("scrollState") { ScrollState(0) }

  RippleColorScope(color = themeColors.primaryVariant) {
    Row(
      modifier = Modifier
        .height(235.dp)
        .padding(top = 15.dp, bottom = 10.dp, start = 10.dp, end = 10.dp)
        .horizontalScroll(scrollState),
    ) {
      for ((index, item) in pageList.withIndex()) {
        Item(
          title = item.title,
          imageUrl = item.thumbnail?.source,
          isFirstItem = index == 0,
          onClick = { gotoArticlePage(item.title) }
        )

        if (index == pageList.size - 1) {
          Column(
            modifier = Modifier
              .padding(start = 10.dp)
              .width(122.dp)
              .height(175.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable { onMoreButtonClick() },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              modifier = Modifier
                .size(26.dp),
              imageVector = Icons.Filled.AutoAwesomeMotion,
              contentDescription = null,
              tint = themeColors.primaryVariant
            )

            StyledText(
              modifier = Modifier
                .padding(top = 5.dp),
              text = stringResource(id = R.string.viewMore),
              color = themeColors.primaryVariant,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Item(
  title: String,
  imageUrl: String?,
  isFirstItem: Boolean,
  onClick: () -> Unit
) {
  val themeColors = MaterialTheme.colors

  Column(
    modifier = Modifier
      .height(210.dp)
      .padding(start = if (isFirstItem) 0.dp else 10.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() },
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (imageUrl != null) {
      AsyncImage(
        modifier = Modifier
          .width(122.dp)
          .height(175.dp)
          .clip(RoundedCornerShape(10.dp)),
        model = rememberImageRequest(imageUrl),
        placeholder = painterResource(id = R.drawable.placeholder),
        contentDescription = null,
        alignment = Alignment.TopCenter,
        contentScale = ContentScale.Crop
      )
    } else {
      Box(
        modifier = Modifier
          .width(122.dp)
          .height(175.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(themeColors.background2),
        contentAlignment = Alignment.Center
      ) {
        StyledText(
          text = stringResource(id = R.string.noImage),
          color = themeColors.text.secondary
        )
      }
    }

    Box(
      modifier = Modifier
        .padding(top = 5.dp),
      contentAlignment = Alignment.Center
    ) {
      StyledText(
        modifier = Modifier
          .width(120.dp),
        text = title,
        fontSize = 13.sp,
        color = themeColors.primaryVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        lineHeight = 13.sp
      )
    }
  }
}