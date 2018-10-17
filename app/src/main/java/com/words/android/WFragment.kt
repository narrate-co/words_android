package com.words.android

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment


open class WFragment: Fragment() {

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        var animation = super.onCreateAnimation(transit, enter, nextAnim)
        if (animation == null && nextAnim != 0) animation = AnimationUtils.loadAnimation(activity, nextAnim)
        if (animation != null) {
            view?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            animation.setAnimationListener(object: Animation.AnimationListener {
                override fun onAnimationRepeat(p0: Animation?) {
                }

                override fun onAnimationEnd(p0: Animation?) {
                    view?.setLayerType(View.LAYER_TYPE_NONE, null)
                    onEnterTransactionEnded()
                }

                override fun onAnimationStart(p0: Animation?) {
                }
            })
        } else {
            onEnterTransactionEnded()
        }

        return animation
    }

    //TODO add methods to override behavior when transition starts/ends

    open fun onEnterTransactionEnded() { }

}