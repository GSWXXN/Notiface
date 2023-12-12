package com.gswxxn.notiface.wrapper

import android.content.Context
import android.graphics.drawable.Drawable
import de.robv.android.xposed.XposedHelpers

class AnimatedVectorDrawableCompatWrapper(val instance: Any) {

    companion object {
        fun newInstance(context: Context, classLoader: ClassLoader) = XposedHelpers.newInstance(
            XposedHelpers.findClass("androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat", classLoader),
            context
        ).let { AnimatedVectorDrawableCompatWrapper(it) }
    }

    val mCallback = XposedHelpers.getObjectField(instance, "mCallback") as Drawable.Callback

    var mDelegateDrawable: Any
        get() = XposedHelpers.getObjectField(instance, "mDelegateDrawable")
        set(value) = XposedHelpers.setObjectField(instance, "mDelegateDrawable", value)

    fun registerAnimationCallback(callback: Any) {
        XposedHelpers.callMethod(instance, "registerAnimationCallback", callback)
    }
}