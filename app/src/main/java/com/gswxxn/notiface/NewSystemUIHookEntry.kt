package com.gswxxn.notiface

import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Message
import com.gswxxn.notiface.wrapper.AnimatedVectorDrawableCompatWrapper
import com.gswxxn.notiface.wrapper.MiuiKeyguardFaceUnlockViewWrapper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class NewSystemUIHookEntry(lpparam: XC_LoadPackage.LoadPackageParam) {

    var shadeListBuilderInstance: Any? = null

    private val mExpandableNotificationRow by lazy {
        XposedHelpers.findClass(
            "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow",
            lpparam.classLoader
        )
    }

    private val mIHapticFeedBack by lazy {
        XposedHelpers.findClass("com.miui.interfaces.IHapticFeedBack", lpparam.classLoader)
    }

    private val mMiuiDependency by lazy {
        XposedHelpers.findClass("com.miui.systemui.MiuiDependency", lpparam.classLoader)
    }

    private val mIMiuiFaceUnlockManager by lazy {
        XposedHelpers.findClass("com.miui.interfaces.keyguard.IMiuiFaceUnlockManager", lpparam.classLoader)
    }

    var isFaceUnlocked = false

    init {

        // 获取 ShadeListBuilder 实例
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.statusbar.notification.collection.ShadeListBuilder",
            lpparam.classLoader,
            "buildList",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isFaceUnlocked)
                        shadeListBuilderInstance = param.thisObject
                }
            }
        )

        // 获取当前锁屏状态
        XposedHelpers.findAndHookMethod("com.android.keyguard.KeyguardUpdateMonitor$15",
            lpparam.classLoader,
            "handleMessage",
            Message::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val what = (param.args[0] as Message).what
                    val isStartedGoingToSleep = what == 321

                    // 息屏后设置 isFaceUnlocked 为 false, 并刷新通知 list
                    if (isStartedGoingToSleep){
                        isFaceUnlocked = false

                        // 刷新通知显示
                        val mOnBeforeRenderListListeners = XposedHelpers.getObjectField(shadeListBuilderInstance, "mOnBeforeRenderListListeners") as List<*>
                        val mReadOnlyNotifList = XposedHelpers.getObjectField(shadeListBuilderInstance, "mReadOnlyNotifList") as List<*>
                        mOnBeforeRenderListListeners.forEach {
                            XposedHelpers.callMethod(it, "onBeforeRenderList", mReadOnlyNotifList)
                        }
                    }
                }
            })

        // Hook 人脸解锁成功事件
        XposedHelpers.findAndHookMethod("com.android.keyguard.KeyguardUpdateMonitor",
            lpparam.classLoader,
            "onFaceAuthenticated",
            Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    isFaceUnlocked = true

                    // 刷新通知显示
                    val mOnBeforeRenderListListeners = XposedHelpers.getObjectField(shadeListBuilderInstance, "mOnBeforeRenderListListeners") as List<*>
                    val mReadOnlyNotifList = XposedHelpers.getObjectField(shadeListBuilderInstance, "mReadOnlyNotifList") as List<*>
                    mOnBeforeRenderListListeners.forEach {
                        XposedHelpers.callMethod(it, "onBeforeRenderList", mReadOnlyNotifList)
                    }

                    // 添加震动
                    XposedHelpers.callStaticMethod(mMiuiDependency, "get", mIHapticFeedBack)
                        .let { XposedHelpers.callMethod(it, "extExtHapticFeedback", 206, "mesh_light", -1, 0) }

                    // 播放解锁成功动画
                    XposedHelpers.callStaticMethod(mMiuiDependency, "get", mIMiuiFaceUnlockManager)
                        .let { XposedHelpers.getObjectField(it, "mFaceViewList") as ArrayList<*> }
                        .forEach {mFaceViewList ->
                            XposedHelpers.callMethod(mFaceViewList, "get")
                                ?.let { startFaceUnlockSuccessAnimation(
                                    MiuiKeyguardFaceUnlockViewWrapper(it), lpparam.classLoader
                                ) }
                        }

                    return null
                }
            })

        // 解锁后 设置所有通知为非敏感
        XposedHelpers.findAndHookMethod(mExpandableNotificationRow, "setSensitive",
            Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    if (isFaceUnlocked) param!!.args[0] = false
                }
            })

        // 如果已成功解锁通知, 则不再重新监听人脸事件
        XposedHelpers.findAndHookMethod(
            "com.android.keyguard.KeyguardUpdateMonitor",
            lpparam.classLoader,
            "shouldListenForFace",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isFaceUnlocked) param.result = false
                }
            })

        // 播放展开通知动画的标志
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.statusbar.notification.DynamicPrivacyController",
            lpparam.classLoader,
            "isDynamicallyUnlocked",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isFaceUnlocked) param.result = true
                }
            })
    }

    // 播放人脸解锁成功动画
    fun startFaceUnlockSuccessAnimation(miuiKeyguardFaceUnlockView: MiuiKeyguardFaceUnlockViewWrapper, classLoader: ClassLoader) {

        val animatorSet: AnimatorSet? = miuiKeyguardFaceUnlockView.mFaceUnlockAnimation
        if (animatorSet != null && animatorSet.isRunning()) {
            miuiKeyguardFaceUnlockView.mFaceUnlockAnimation.cancel()
        }
        miuiKeyguardFaceUnlockView.mRingIV.setAlpha(0.0f)
        miuiKeyguardFaceUnlockView.mRingIV.rotationY = 0.0f
        miuiKeyguardFaceUnlockView.mRingIV.translationX = 0.0f
        miuiKeyguardFaceUnlockView.mFaceIV.rotationY = 0.0f
        miuiKeyguardFaceUnlockView.mFaceIV.translationX = 0.0f

        val context2: Context = miuiKeyguardFaceUnlockView.getContext()
        val i = if (!miuiKeyguardFaceUnlockView.isPrimaryBouncerIsOrWillBeShowing() && miuiKeyguardFaceUnlockView.mLightLockWallpaper) {
            context2.getID("drawable", "face_unlock_black_success_ani")
        } else {
            context2.getID("drawable", "face_unlock_success_ani")
        }
        val animatedVectorDrawableCompat = AnimatedVectorDrawableCompatWrapper.newInstance(context2, classLoader)
        val resources = context2.resources
        val theme = context2.theme
        val drawable = resources.getDrawable(i, theme)
        drawable.callback = animatedVectorDrawableCompat.mCallback
        animatedVectorDrawableCompat.mDelegateDrawable = drawable
        miuiKeyguardFaceUnlockView.mSuccessfulAnimation = animatedVectorDrawableCompat.instance as Animatable
        animatedVectorDrawableCompat.registerAnimationCallback(miuiKeyguardFaceUnlockView.mSuccessAnimationListener)
        miuiKeyguardFaceUnlockView.mFaceIV.setImageDrawable(miuiKeyguardFaceUnlockView.mSuccessfulAnimation as Drawable)
        miuiKeyguardFaceUnlockView.mSuccessfulAnimation.start()
        miuiKeyguardFaceUnlockView.setContentDescription(
            miuiKeyguardFaceUnlockView.mContext.resources
                .getString(context2.getID("string", "face_unlock_success"))
        )
    }

    @SuppressLint("DiscouragedApi")
    private fun Context.getID(type: String, name: String): Int =
        resources.getIdentifier(name, type, "com.android.systemui")
}