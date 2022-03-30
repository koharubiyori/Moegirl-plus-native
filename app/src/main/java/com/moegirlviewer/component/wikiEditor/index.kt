package com.moegirlviewer.component.wikiEditor

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref
import com.google.gson.Gson
import com.moegirlviewer.component.htmlWebView.HtmlWebView
import com.moegirlviewer.component.htmlWebView.HtmlWebViewContent
import com.moegirlviewer.component.htmlWebView.HtmlWebViewRef
import com.moegirlviewer.util.ProguardIgnore
import com.moegirlviewer.util.toCssRgbaString
import com.moegirlviewer.util.toUnicodeForInjectScriptInWebView
import kotlinx.coroutines.launch

@Composable
fun WikiEditor(
  modifier: Modifier = Modifier,
  state: WikiEditorState,
  onTextChange: ((String) -> Unit)? = null
) {
  val themeColors = MaterialTheme.colors
  val scope = rememberCoroutineScope()

  LaunchedEffect(true) {
    state.init(themeColors)
  }

  HtmlWebView(
    modifier = modifier,
    ref = state.htmlWebViewRef,
    messageHandlers = mapOf(
      "onLoaded" to {
        scope.launch {
          state.setTextContent(state.lastSettingContent)
        }
      },

      "onTextChange" to {
        val textContent = it!!.get("text").asString
        onTextChange?.invoke(textContent)
      }
    )
  )
}

class WikiEditorState {
  internal val htmlWebViewRef = Ref<HtmlWebViewRef>()
  internal var lastSettingContent = ""

  internal fun init(themeColors: Colors) {
    val style = """
      .CodeMirror-line::selection, 
      .CodeMirror-line>span::selection, 
      .CodeMirror-line>span>span::selection {
        background-color: ${themeColors.secondary.copy(alpha = 0.2f).toCssRgbaString()} !important;
      }

      /* 这段代码不生效，经过测试CodeMirror会检测是否为移动端，如果是的话就不会使用自定义的光标。可能是使用自定义光标会有问题，但这里还是姑且保留一下 */
      .CodeMirror-cursor {
        border-left: 2px solid ${themeColors.secondary.toCssRgbaString()}     
      }
      
      body {
        caret-color: ${themeColors.secondary.toCssRgbaString()};
        font-size: 16px;
      }
    """.trimIndent()

    val script = """
      window.onEditorTextChange = text => _postMessage('onTextChange', { text })
      _postMessage('onLoaded')
    """.trimIndent()

    htmlWebViewRef.value!!.updateContent {
      HtmlWebViewContent(
        title = "wikiEditor",
        injectedStyles = listOf(style),
        injectedScripts = listOf(script),
        injectedFiles = listOf(
          "editor-main.js",
          "editor-main.css"
        )
      )
    }
  }

  internal suspend fun getTextContent(): String {
    val rawResult = htmlWebViewRef.value!!.injectScript("editor.getValue()")
    return Gson().fromJson(rawResult, String::class.java)
  }

  internal suspend fun setTextContent(text: String) {
    lastSettingContent = text
    val escapedText = text.toUnicodeForInjectScriptInWebView()
    htmlWebViewRef.value!!.injectScript("editor.setValue('$escapedText')")
  }

  internal suspend fun insertTextAtCursor(text: String) {
    val escapedText = text.toUnicodeForInjectScriptInWebView()
    htmlWebViewRef.value!!.injectScript("editor.replaceSelection('$escapedText')")
  }

  suspend fun getPosition(): WikiEditorCursorPosition {
    val positionJson = htmlWebViewRef.value!!.injectScript("editor.getCursor()")
    return Gson().fromJson(positionJson, WikiEditorCursorPosition::class.java)
  }

  suspend fun setSelection(
    startPosition: WikiEditorCursorPosition,
    endPosition: WikiEditorCursorPosition
  ) {
    htmlWebViewRef.value!!.injectScript("""
      editor.setSelection(
        { line: ${startPosition.line}, ch: ${startPosition.ch} },
        { line: ${endPosition.line}, ch: ${endPosition.ch} },
      )
    """.trimIndent())
  }

  suspend fun setCursorPosition(position: WikiEditorCursorPosition) {
    setSelection(position, position)
  }
}

@ProguardIgnore
data class WikiEditorCursorPosition(
  val line: Int,
  val ch: Int
)