package com.oc.catemoji.catoc.core.helper

import android.content.Context
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.content.res.ResourcesCompat

object StringHelper {
    fun changeColor(
        context: Context,
        text: String,
        fontfamily: Int,
    ): SpannableString {
        val spannableString = SpannableString(text)

        val font = ResourcesCompat.getFont(context, fontfamily)
        val typefaceSpan = CustomTypefaceSpan("", font)
        spannableString.setSpan(
            typefaceSpan, 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }
}