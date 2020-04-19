package com.support.core.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.support.core.event.asObservable
import com.support.core.functional.LifecycleDestroyObserver
import kotlinx.android.extensions.LayoutContainer

abstract class RecyclerAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mItems: List<T>? = null

    val items get() = mItems

    open fun submit(it: List<T>?) {
        mItems = it
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mItems?.size ?: 0

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? IHolder<T>)?.bind(mItems!![position])
    }

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (!payloads.isNullOrEmpty()) (holder as? IHolder<T>)?.onChangedWith(payloads)
        else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        (holder as? IHolder<*>)?.onRecycled()
    }

}

interface IHolder<T> {
    fun bind(item: T)
    fun onRecycled() {}
    fun onChangedWith(payload: Any? = null) {
    }
}

open class RecyclerHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView), IHolder<T>,
        LayoutContainer, Observer<T> {
    private var parent: ViewGroup? = null

    constructor(parent: ViewGroup, @LayoutRes layoutId: Int) : this(
            LayoutInflater.from(parent.context)
                    .inflate(layoutId, parent, false)
    ) {
        this.parent = parent
    }

    override val containerView: View?
        get() = itemView
    var item: T? = null
        private set

    fun subscribe(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(LifecycleDestroyObserver {
            onRecycled()
        })
    }

    @CallSuper
    override fun bind(item: T) {
        item.asObservable()?.also {
            val old = this@RecyclerHolder.item
            if (old != item) old?.asObservable()?.unsubscribe(this@RecyclerHolder)
        }?.subscribe(this)
        this.item = item
    }

    override fun onRecycled() {
        item?.asObservable()?.unsubscribe(this)
    }

    override fun onChanged(t: T) {}

    val isAtLast get() = adapterPosition + 1 == (parent as? RecyclerView)?.adapter?.itemCount ?: 0
    val isAtFirst get() = adapterPosition == 0

}
