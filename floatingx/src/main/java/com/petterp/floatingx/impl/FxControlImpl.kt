package com.petterp.floatingx.impl

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.petterp.floatingx.assist.FxHelper
import com.petterp.floatingx.config.SystemConfig
import com.petterp.floatingx.ext.FxDebug
import com.petterp.floatingx.ext.fxParentView
import com.petterp.floatingx.ext.hide
import com.petterp.floatingx.ext.navigationBarHeight
import com.petterp.floatingx.ext.show
import com.petterp.floatingx.ext.statusBarHeight
import com.petterp.floatingx.ext.topActivity
import com.petterp.floatingx.listener.IFxControl
import com.petterp.floatingx.view.FxMagnetView
import com.petterp.floatingx.view.FxViewHolder
import java.lang.ref.WeakReference

/**
 * @Author petterp
 * @Date 2021/5/21-2:24 下午
 * @Email ShiyihuiCloud@163.com
 * @Function fx基础控制器实现类
 */
open class FxControlImpl(private val helper: FxHelper) : IFxControl {

    private var managerView: FxMagnetView? = null
    private var viewHolder: FxViewHolder? = null
    private var mContainer: WeakReference<FrameLayout>? = null
    private val managerViewOrContainerIsNull: Boolean
        get() = mContainer == null && managerView == null
    private val runnable by lazy {
        Runnable {
            cancelFx()
        }
    }

    override fun show() {
        if (!helper.enableFx) helper.enableFx = true
        if (!isShowRunning())
            attach(topActivity!!)
        managerView?.show()
    }

    override fun show(activity: Activity) {
        if (!helper.enableFx) helper.enableFx = true
        attach(activity)
        managerView?.show()
    }

    override fun hide() {
        managerView ?: return
        managerView?.hide()
    }

    override fun getView(): View? = managerView

    override fun isShowRunning(): Boolean =
        managerView != null && ViewCompat.isAttachedToWindow(managerView!!) && managerView?.isVisible == true

    override

    fun updateView(obj: (FxViewHolder) -> Unit) {
        viewHolder?.let(obj)
    }

    override fun updateView(@DrawableRes resource: Int) {
        initManagerView(resource)
    }

    override fun updateParams(params: ViewGroup.LayoutParams) {
        managerView?.layoutParams = params
    }

    override fun attach(activity: Activity) {
        activity.fxParentView?.let {
            attach(it)
        } ?: FxDebug.e("system -> fxParentView==null")
    }

    // 安装到fragmentLayout上
    override fun attach(container: FrameLayout) {
        if (managerView?.parent === container) {
            return
        }
        topActivity?.let {
            SystemConfig.navigationBarHeight = it.navigationBarHeight
            SystemConfig.statsBarHeight = it.statusBarHeight
            FxDebug.v("system-> navigationBar-${SystemConfig.navigationBarHeight}--statBarHeight-${SystemConfig.statsBarHeight}")
        }
        var isAnimation = false
        if (managerView == null) {
            initManagerView()
            isAnimation = true
        } else {
            removeManagerView(getContainer())
        }
        mContainer = WeakReference(container)
        addManagerView()
        if (isAnimation && helper.enableAnimation) {
            helper.iFxAnimation?.apply {
                cancelAnimation()
                startAnimation(managerView)
                FxDebug.d("view->Animation -----start")
            }
        }
    }

    /** 删除view */
    override fun detach(activity: Activity) {
        if (managerViewOrContainerIsNull) return
        activity.fxParentView?.let {
            detach(it)
        }
    }

    override fun detach(container: FrameLayout) {
        if (managerView != null && ViewCompat.isAttachedToWindow(managerView!!)) {
            removeManagerView(container)
        }
        if (container === getContainer()) {
            mContainer = null
        }
    }

    override fun setClickListener(time: Long, obj: (View) -> Unit) {
        helper.clickListener = obj
        helper.clickTime = time
    }

    override fun dismiss() {
        FxDebug.d("view->dismiss-----------")
        helper.enableFx = false
        if (helper.enableAnimation && helper.iFxAnimation != null) {
            managerView?.removeCallbacks(runnable)
            managerView?.postDelayed(
                runnable,
                helper.iFxAnimation.animatorTime
            )
            helper.iFxAnimation.endAnimation(managerView)
            FxDebug.d("view->Animation -----end")
        } else {
            cancelFx()
        }
    }

    private fun cancelFx() {
        mContainer?.get()?.let {
            detach(it)
        }
        managerView = null
        viewHolder = null
        FxDebug.d("view-lifecycle-> code->cancelFx")
    }

    private fun addManagerView() {
        FxDebug.d("view-lifecycle-> code->addView")
        helper.iFxViewLifecycle?.postAttach()
        getContainer()?.addView(managerView)
        managerView?.show()
    }

    private fun removeManagerView(container: FrameLayout?) {
        if (container == null) return
        FxDebug.d("view-lifecycle-> code->removeView")
        helper.iFxViewLifecycle?.postDetached()
        container.removeView(managerView)
    }

    open fun initManagerView(@DrawableRes layout: Int = 0) {
        if (layout != 0) helper.layoutId = layout
        if (helper.layoutId == 0) throw RuntimeException("The layout id cannot be 0")
        managerView = FxMagnetView(helper)
        ViewCompat.setOnApplyWindowInsetsListener(managerView!!, windowsInsetsListener)
        managerView?.requestApplyInsets()
        viewHolder = FxViewHolder(managerView!!)
    }

    @SuppressLint("WrongConstant")
    val windowsInsetsListener: OnApplyWindowInsetsListener =
        OnApplyWindowInsetsListener { _, insets ->
            FxDebug.v("System--StatusBar---old-(${SystemConfig.statsBarHeight}),new-(${insets.systemWindowInsetTop})")
            SystemConfig.statsBarHeight = insets.systemWindowInsetTop
            insets
        }

    private fun getContainer(): FrameLayout? {
        return mContainer?.get()
    }
}
