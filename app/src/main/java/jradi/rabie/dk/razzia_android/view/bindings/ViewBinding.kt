package jradi.rabie.dk.razzia_android.view.bindings

import android.databinding.BindingAdapter
import android.view.View
import android.view.View.*

/**
 * @author rabie
 *
 *
 */

@BindingAdapter("visibleOrGone")
fun View.setVisibleOrGone(show: Boolean) {
    visibility = if (show) VISIBLE else GONE
}

@BindingAdapter("visible")
fun View.setVisible(show: Boolean) {
    visibility = if (show) VISIBLE else INVISIBLE
}