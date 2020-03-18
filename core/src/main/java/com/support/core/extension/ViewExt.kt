package com.support.core.extension

import android.content.Context
import android.content.res.TypedArray
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DimenRes
import java.util.*


fun EditText.focus() {
    requestFocus()
    setSelection(0, length())
}

fun View.setMarginTop(it: Float) {
    (layoutParams as ViewGroup.MarginLayoutParams).topMargin = it.toInt()
}

fun View.setMarginTop(@DimenRes dimen: Int) {
    (layoutParams as ViewGroup.MarginLayoutParams).topMargin = if (dimen == 0) 0 else
        resources.getDimensionPixelSize(dimen)
}

fun ViewGroup.setContentView(id: Int) {
    LayoutInflater.from(context).inflate(id, this, true)
}


fun TextView.format(vararg format: Any): String {
    return text.toString().format(Locale.getDefault(), *format)
}

fun TextView.addSpan(
    spanValue: String,
    spanned: CharacterStyle,
    textValue: String = text.toString()
) {
    val span = SpannableString(textValue)
    val start = span.indexOf(spanValue)
    if (start == -1) return
    val end = start + spanValue.length
    span.setSpan(spanned, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    if (spanned is ClickableSpan) {
        movementMethod = LinkMovementMethod.getInstance()
    }
    setText(span, TextView.BufferType.SPANNABLE)
}

fun ViewGroup.inflate(id: Int): View {
    return LayoutInflater.from(context).inflate(id, this, false)
}

fun Context.with(
    attrs: AttributeSet?,
    type: IntArray,
    defStyleAttr: Int,
    function: (TypedArray) -> Unit
) {
    if (attrs != null) {
        val typed = obtainStyledAttributes(attrs, type, defStyleAttr, 0)
        function(typed)
        typed.recycle()
    }
}

infix fun Boolean.enable(view: View) {
    view.isEnabled = this
}

infix fun Boolean.enable(views: List<View>) {
    views.forEach { this enable it }
}

infix fun Boolean.show(view: View) {
    view.visibility = if (this) View.VISIBLE else View.GONE
}

infix fun Boolean.show(views: List<View>) {
    views.forEach { this show it }
}

infix fun Boolean.invisible(view: View) {
    view.visibility = if (this) View.INVISIBLE else View.VISIBLE
}

operator fun View.plus(view: View): List<View> {
    return arrayListOf(this, view)
}

fun View.gone() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}
