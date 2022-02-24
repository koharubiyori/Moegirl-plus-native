package com.moegirlviewer.screen.compare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.moegirlviewer.R
import com.moegirlviewer.api.account.AccountApi
import com.moegirlviewer.api.edit.EditApi
import com.moegirlviewer.api.editingRecord.EditingRecordApi
import com.moegirlviewer.api.editingRecord.bean.ComparePageResult
import com.moegirlviewer.component.commonDialog.ButtonConfig
import com.moegirlviewer.component.commonDialog.CommonAlertDialogProps
import com.moegirlviewer.component.styled.StyledText
import com.moegirlviewer.request.MoeRequestException
import com.moegirlviewer.screen.compare.util.DiffLine
import com.moegirlviewer.screen.compare.util.collectDiffBlocksFormHtml
import com.moegirlviewer.util.Globals
import com.moegirlviewer.util.LoadStatus
import com.moegirlviewer.util.printRequestErr
import com.moegirlviewer.util.toast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
@ExperimentalPagerApi
class CompareScreenModel @Inject constructor() : ViewModel() {
  lateinit var routeArguments: CompareRouteArguments
  val coroutineScope = CoroutineScope(Dispatchers.Main)
  val pagerState = PagerState(0)
  var compareData by mutableStateOf<ComparePageResult.Compare?>(null)
  var leftLines by mutableStateOf(emptyList<DiffLine>())
  var rightLines by mutableStateOf(emptyList<DiffLine>())
  var status by mutableStateOf(LoadStatus.INITIAL)
  var selectedTabIndex by mutableStateOf(0)

  val isCompareTextMode get() = routeArguments is CompareTextRouteArguments
  val routeArgumentsOfPageCompare get() = routeArguments as ComparePageRouteArguments
  val routeArgumentsOfTextCompare get() = routeArguments as CompareTextRouteArguments

  suspend fun loadCompareData() {
    status = LoadStatus.LOADING
    try {
      val res = if (isCompareTextMode) {
        EditingRecordApi.comparePage(
          fromText = routeArgumentsOfTextCompare.formText,
          toText = routeArgumentsOfTextCompare.toText
        )
      } else {
        EditingRecordApi.comparePage(
          fromTitle = routeArgumentsOfPageCompare.pageName,
          fromRev = routeArgumentsOfPageCompare.fromRevId,
          toTitle = routeArgumentsOfPageCompare.pageName,
          toRev = routeArgumentsOfPageCompare.toRevId
        )
      }

      val diffBlocks = withContext(Dispatchers.Default) {
        collectDiffBlocksFormHtml("<table>${res.compare._asterisk}</table>")
      }

      compareData = res.compare
      leftLines = diffBlocks.map { it.left }
      rightLines = diffBlocks.map { it.right }
      status = LoadStatus.SUCCESS
    } catch (e: MoeRequestException) {
      printRequestErr(e, "加载页面差异数据失败")
      status = LoadStatus.FAIL
    }
  }

  suspend fun submitUndo(summary: String): Boolean {
    val userName = compareData!!.touser
    val toRevId = routeArgumentsOfPageCompare.toRevId
    val summaryPrefix = Globals.context.getString(R.string.comparesummaryPrefix, userName, toRevId.toString())
    val fullSummary = summaryPrefix + " " +
      Globals.context.getString(R.string.undoReason) +
      "：${if (summary != "") summary else Globals.context.getString(R.string.reasonNoFilled)}"

    Globals.commonLoadingDialog.showText(Globals.context.getString(R.string.submitting))
    return try {
      EditApi.edit(
        pageName = routeArgumentsOfPageCompare.pageName,
        summary = fullSummary,
        undoRevId = toRevId,
        baseDateISO = EditApi.getTimestampOfLastEdit(routeArgumentsOfPageCompare.pageName)
      )
      toast(Globals.context.getString(R.string.undid))
      true
    } catch (e: MoeRequestException) {
      printRequestErr(e, "执行撤销失败")
      toast(e.message)
      false
    } finally {
      Globals.commonLoadingDialog.hide()
    }
  }

  fun sendThank() {
    Globals.commonAlertDialog.show(CommonAlertDialogProps(
      content = { StyledText(stringResource(id = R.string.sendThankHint)) },
      secondaryButton = ButtonConfig.cancelButton(),
      onPrimaryButtonClick = {
        coroutineScope.launch {
          Globals.commonLoadingDialog.showText(Globals.context.getString(R.string.submitting))
          try {
            // 没有toRevId代表是通过“当前”按钮点进来的，这个感谢功能应该总是给用点进去的那条修订的编辑者发送
            // “当前”是点击版本(from)与当前版本(to)对比，“之前”是点击版本的上个版本(from)与点击版本(to)对比
            AccountApi.thank(routeArgumentsOfPageCompare.toRevId ?: routeArgumentsOfPageCompare.fromRevId)
            toast(Globals.context.getString(R.string.sent))
          } catch (e: MoeRequestException) {
            printRequestErr(e, "发送编辑感谢失败")
            toast(e.message)
          } finally {
            Globals.commonLoadingDialog.hide()
          }
        }
      }
    ))
  }

  override fun onCleared() {
    super.onCleared()
    coroutineScope.cancel()
    routeArguments.removeReferencesFromArgumentPool()
  }
}