package com.support.core.functional.spanable

import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.widget.TextView

class MultipleBoldSpan(
    override val textView: TextView,
    vararg text: String
) : MultipleSpanText {
    private val mText = text

    override val spans = mText.mapNotNull { if (it.isEmpty()) null else ItemSpan(it) }

    inner class ItemSpan(val it: String) : SpanText {
        override val textView: TextView
            get() = this@MultipleBoldSpan.textView
        override val spanAtText: String
            get() = it
        override val style: CharacterStyle
            get() = StyleSpan(Typeface.BOLD)
    }
}