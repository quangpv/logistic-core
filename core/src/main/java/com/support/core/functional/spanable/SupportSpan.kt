package com.support.core.functional.spanable

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.support.core.R
import com.support.core.extension.format

//class SupportSpan(override val textView: TextView, private val support: String) :
//    SpanText {
//    override val spanAtText: String = MyApplication.SUPPORT_EMAIL
//    override val spanText: String get() = textView.format(spanAtText)
//
//    override val style = object : ClickableSpan() {
//        override fun onClick(widget: View) {
//            context.openEmail(spanAtText, support)
//        }
//
//        override fun updateDrawState(ds: TextPaint) {
//            ds.color = ContextCompat.getColor(context, R.color.colorPrimary)
//            ds.isUnderlineText = false
//        }
//    }
//}