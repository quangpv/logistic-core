package com.support.core.functional.spanable

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.widget.TextView
import com.support.core.extension.block

interface ISpannable {
    val textView: TextView
    val spanText: String get() = textView.text.toString()
    val context: Context get() = textView.context
    fun apply()
}

interface SpanText : ISpannable {
    val spanAtText: String
    val style: CharacterStyle

    override fun apply() = block(textView) {
        val span = SpannableString(spanText)
        val start = span.indexOf(spanAtText)
        if (start == -1) return@block
        val end = start + spanAtText.length
        span.setSpan(style, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (style is ClickableSpan) {
            movementMethod = LinkMovementMethod.getInstance()
        }
        setText(span, TextView.BufferType.SPANNABLE)
    }
}

interface MultipleSpanText : ISpannable {

    val spans: List<SpanText>

    override fun apply() = block(textView) {
        if (spanText.isBlank()) {
            text = ""
            return@block
        }
        if (spans.isEmpty()) {
            text = spanText
            return@block
        }
        val span = SpannableString(spanText)
        spans.forEach {
            val start = span.indexOf(it.spanAtText)
            if (start == -1) return@forEach
            val end = start + it.spanAtText.length
            span.setSpan(it.style, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (it.style is ClickableSpan) {
                movementMethod = LinkMovementMethod.getInstance()
            }
        }
        setText(span, TextView.BufferType.SPANNABLE)
    }
}


