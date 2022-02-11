package com.moegirlviewer.screen.edit.tabs.wikitextEditor.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.lang.Integer.max

internal val linearParsingMarkupList = listOf(
  PairWikitextMarkup(
    startText = "[[",
    endText = "]]",
    style = SpanStyle(
      color = Color(0xff21A3F1),
    ),
  ),
  PairWikitextMarkup(
    startText = "{{",
    endText = "}}",
    style = SpanStyle(
      color = Color(0xffE38A2E),
    ),
  ),
  PairWikitextMarkup(
    startText = "[http",
    endText = "]",
    style = SpanStyle(
      color = Color(0xff38AD6C)
    ),
  ),
  PairWikitextMarkup(
    startText = "<!--",
    endText = "-->",
    style = SpanStyle(
      color = Color(0xffBEF781)
    )
  ),
  EqualWikitextMarkup(
    text = "'''",
    style = SpanStyle(
      fontWeight = FontWeight.Black,
    ),
  ),
  EqualWikitextMarkup(
    text = "''",
    style = SpanStyle(
      fontStyle = FontStyle.Italic
    )
  )
)

internal val matchParsingMarkupList = listOf(
  *(6 downTo 1).map {
    InlineEqualWikitextMarkup(
      text = "=".repeat(it),
      style = SpanStyle(
        color = Color.Red,
        fontWeight = FontWeight.Black
      )
    )
  }.toTypedArray(),
  InlineSingleWikitextMarkup(
    text = "*",
    style = SpanStyle(
      color = Color(0xff6868F2),
      fontWeight = FontWeight.Black
    ),
    contentStyle = SpanStyle(
      color = Color(0xff6868F2)
    )
  ),
  InlineSingleWikitextMarkup(
    text = "#",
    style = SpanStyle(
      color = Color(0xff6868F2),
      fontWeight = FontWeight.Black
    ),
    contentStyle = SpanStyle(
      color = Color(0xff6868F2)
    )
  ),
  InlineSingleWikitextMarkup(
    text = ";",
    style = SpanStyle(
      color = Color(0xff6868F2),
      fontWeight = FontWeight.Black
    ),
    contentStyle = SpanStyle(
      color = Color(0xff6868F2)
    )
  ),
)

fun tintWikitext(wikitext: String): AnnotatedString {
  val linearParseResult = linearParseWikitext(wikitext)
  val matchParseResult = matchParseWikitext(wikitext)

  val mergedResult = linearParseResult.mergeInlineParseResult(matchParseResult)
  val annotatedString = buildAnnotatedString {
    for (item in mergedResult) {
      tintTextByMarkup(item)
    }
  }

  return annotatedString
}

abstract class WikitextMarkup(
  val style: SpanStyle,
  val contentStyle: SpanStyle = style
)

//class TagWikitextMarkup(
//  val tagName: String,
//  style: SpanStyle,
//  contentStyle: SpanStyle = style
//) : WikitextMarkup(style, contentStyle)

interface TintableWikitextMarkup {
  val startText: String
  val endText: String
  val style: SpanStyle
  val contentStyle: SpanStyle
}

data class ParseResult<T : TintableWikitextMarkup>(
  val content: String = "",
  val contentRange: ClosedRange<Int>,
  val markup: T? = null,
  val containStartMarkup: Boolean = false,
  val containEndMarkup: Boolean = false,
  val prefix: String? = null,
  val suffix: String? = null
) {
  val startMarkupRange: ClosedRange<Int>? get() {
    if (markup == null || !containStartMarkup) return null
    return (contentRange.start - markup.startText.length)..contentRange.start
  }
  val endMarkupRange: ClosedRange<Int>? get() {
    if (markup == null || !containEndMarkup) return null
    return contentRange.endInclusive..(contentRange.endInclusive + markup.endText.length)
  }
  val fullContentRange: ClosedRange<Int> get() {
    if (markup == null) return contentRange
    val startOffset = if (containStartMarkup) -markup.startText.length else 0
    val endOffset = if (containEndMarkup) markup.endText.length else 0
    return (contentRange.start + startOffset - (prefix?.length ?: 0))..(contentRange.endInclusive + endOffset + (suffix?.length ?: 0))
  }
}

//fun List<ParseResult>.update(
//  startIndex: Int,
//  content: String? = null,
//  deleteCount: Int = 0,
//): List<ParseResult> {
//  if ()
//}

private fun AnnotatedString.Builder.tintTextByMarkup(parseResult: ParseResult<out TintableWikitextMarkup>) {
  if (parseResult.markup == null) {
    append(parseResult.content)
  } else {
    if (parseResult.prefix != null) append(parseResult.prefix)
    if (parseResult.containStartMarkup) {
      withStyle(parseResult.markup.style) { append(parseResult.markup.startText) }
    }
    withStyle(parseResult.markup.contentStyle) { append(parseResult.content) }
    if (parseResult.containEndMarkup) {
      withStyle(parseResult.markup.style) { append(parseResult.markup.endText) }
    }
    if (parseResult.suffix != null) append(parseResult.suffix)
  }
}