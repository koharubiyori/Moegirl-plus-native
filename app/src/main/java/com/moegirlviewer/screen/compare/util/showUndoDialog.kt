package com.moegirlviewer.screen.compare.util

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.gson.Gson
import com.moegirlviewer.R
import com.moegirlviewer.component.PlainTextField
import com.moegirlviewer.component.commonDialog.ButtonConfig
import com.moegirlviewer.component.commonDialog.CommonAlertDialogProps
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.screen.compare.CompareScreenModel
import com.moegirlviewer.theme.background2
import com.moegirlviewer.util.Globals
import kotlinx.coroutines.launch

@ExperimentalPagerApi
fun showUndoDialog(model: CompareScreenModel) {
  var summary by mutableStateOf("")
  val quickInsertList = Gson().fromJson(Globals.context.getString(R.string.jsonArray_quickSummaryListOfUndo), Array<String>::class.java)

  fun submit() {
    model.coroutineScope.launch {
      val isCloseDialog = model.submitUndo(summary)
      if (isCloseDialog) {
        Globals.commonAlertDialog.hide()
      }
    }
  }

  Globals.commonAlertDialog.show(CommonAlertDialogProps(
    title = Globals.context.getString(R.string.execUndo),
    closeOnDismiss = false,
    closeOnAction = false,
    content = {
      val themeColors = MaterialTheme.colors

      Column {
        PlainTextField(
          modifier = Modifier
            .padding(vertical = 5.dp),
          value = summary,
          placeholder = stringResource(id = R.string.inputUndoReasonPlease),
          singleLine = true,
          underline = true,
          maxLength = 200,
          lengthIndicator = true,
          textStyle = TextStyle(
            fontSize = 16.sp
          ),
          keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onAny = { submit() }
          ),
          onValueChange = {
            summary = it
          }
        )

        StyledText(
          text = stringResource(id = R.string.quickInsert),
          fontSize = 16.sp
        )

        Row(
          modifier = Modifier
            .horizontalScroll(rememberScrollState())
        ) {
          for (item in quickInsertList) {
            StyledText(
              modifier = Modifier
                .padding(top = 10.dp, end = 5.dp)
                .clip(CircleShape)
                .clickable { summary += item }
                .background(themeColors.background2)
                .padding(vertical = 5.dp, horizontal = 10.dp),
              text = item,
            )
          }
        }
      }
    },
    secondaryButton = ButtonConfig.cancelButton(),
    primaryButtonText = Globals.context.getString(R.string.submit),
    onPrimaryButtonClick = { submit() }
  ))
}