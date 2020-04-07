package com.support.core.navigation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.util.*
import kotlin.reflect.KClass

class FragmentNavigator(
    private val fragmentManager: FragmentManager,
    container: Int
) : Navigator(fragmentManager, container) {

    private val mStack = DestinationStack()

    override val lastDestination: Destination? get() = mStack.last

    private val Destination.requireFragment: Fragment
        get() = fragment ?: error("Not found requireFragment $tag")

    private val Destination.fragment: Fragment?
        get() = fragmentManager.findFragmentByTag(tag)

    private val String.tagId: Long?
        get() = split(":").lastOrNull()?.toLong()

    private val String.isSingle: Boolean
        get() = split(":").let { it[it.size - 2] == "single" }

    private val KClass<out Fragment>.tagId: Long?
        get() = fragmentManager.fragments.findLast { (it.javaClass == this.java) && it.tag?.isSingle == true }?.tag?.tagId

    override fun navigate(kClass: KClass<out Fragment>, args: Bundle?, navOptions: NavOptions?) {
        transaction {
            val isStart = mStack.isEmpty
            val lastVisible = mStack.last

            val fragmentRemoves = if (navOptions?.popupTo == null) emptyList<Fragment>()
            else popBackStack(kClass, navOptions)

            val (destination, fragment) = findOrCreate(kClass, navOptions)

            fragment.notifyArgumentChangeIfNeeded(args)

            setNavigateAnim(destination, lastVisible, isStart)

            lastVisible
                ?.requireFragment
                ?.takeIf { !fragmentRemoves.contains(it) }
                ?.also { hide(it) }

            fragmentRemoves.forEach { remove(it) }
            if (fragment.isAdded) show(fragment)
            else add(container, fragment, destination.tag)
            onNavigateChangedListener(kClass)
            Log.i("Stack", mStack.toString())
        }
    }

    private fun FragmentTransaction.setNavigateAnim(
        destination: Destination,
        visible: Destination?,
        isStart: Boolean
    ) {
        setCustomAnimations(if (isStart) 0 else destination.animEnter, visible?.animExit ?: 0)
    }

    private fun FragmentTransaction.setPopupAnim(
        current: Destination,
        previous: Destination?
    ) {
        setCustomAnimations(previous?.animPopEnter ?: 0, current.animPopExit)
    }

    private fun findOrCreate(
        kClass: KClass<out Fragment>,
        navOptions: NavOptions?
    ): Pair<Destination, Fragment> {
        if (navOptions?.singleTop == true) {
            val des = mStack.find(kClass)
            if (des != null) {
                mStack.remove(des)
                mStack.push(des)
                return des to des.requireFragment
            }
        }
        val keepInstance = navOptions?.singleInstance ?: false

        val tagId = if (keepInstance) kClass.tagId ?: System.currentTimeMillis()
        else System.currentTimeMillis()

        val des = mStack.create(kClass, tagId, navOptions)
        if (des.keepInstance) {
            val fragment = des.fragment
            if (fragment != null) return des to fragment
        }
        return des to des.createFragment()
    }

    private fun popBackStack(
        kClass: KClass<out Fragment>,
        navOptions: NavOptions
    ): ArrayList<Fragment> {
        val removes = arrayListOf<Fragment>()
        fun popup(it: Destination) {
            if (it.keepInstance) return
            if (kClass != it.kClass || !navOptions.singleTop) removes.add(it.requireFragment)
        }

        if (navOptions.inclusive) mStack.removeUntil(navOptions, kClass, ::popup)
        else mStack.removeAllIgnore(navOptions, kClass, ::popup)
        return removes
    }

    override fun navigateUp(): Boolean {
        if (mStack.isEmpty) return false
        val currentFragment = mStack.last!!.requireFragment
        if (currentFragment is Backable && currentFragment.onInterceptBackPress()) return true

        if (mStack.size == 1) return false
        val current = mStack.pop() ?: return false
        val previous = mStack.last
        transaction {
            setPopupAnim(current, previous)

            if (current.keepInstance) hide(currentFragment)
            else remove(currentFragment)

            if (previous != null) {
                val fragment=previous.requireFragment
                show(fragment)
                onNavigateChangedListener(fragment.javaClass.kotlin)
            }
        }
        Log.i("Stack", mStack.toString())
        return previous != null
    }

    override fun onSaveInstance(state: Bundle) {
        mStack.onSaveInstance(state)
        Log.i("Stack", "Saved")
    }

    override fun onRestoreInstance(saved: Bundle) {
        mStack.onRestoreInstance(saved)
        Log.i("Stack", mStack.toString())
    }

}

fun Fragment.notifyArgumentChangeIfNeeded(args: Bundle?) {
    arguments = args
    args ?: return
    if (this is ArgumentChangeable) {
        if (isAdded) onNewArguments(args)
        else lifecycle.onSingleStart {
            onNewArguments(args)
        }
    }
}

class DestinationStack {
    val size get() = mStack.size
    val isEmpty get() = mStack.isEmpty()
    private val mStack = Stack<Destination>()
    val last: Destination?
        get() {
            if (mStack.empty()) return null
            return mStack.lastElement()
        }

    fun find(
        kClass: KClass<out Fragment>,
        accept: ((Destination) -> Boolean)? = null
    ): Destination? {
        if (accept != null) return mStack.findLast { (it.kClass == kClass) && accept(it) }
        return mStack.findLast { it.kClass == kClass }
    }

    fun create(kClass: KClass<out Fragment>, tagId: Long, navOptions: NavOptions?): Destination {
        return Destination(
            kClass,
            tagId,
            navOptions?.singleInstance ?: false
        ).also {
            if (navOptions != null) {
                it.animEnter = navOptions.animEnter
                it.animExit = navOptions.animExit
                it.animPopEnter = navOptions.animPopEnter
                it.animPopExit = navOptions.animPopExit
            }
            mStack.push(it)
        }
    }

    fun pop(): Destination? {
        if (mStack.empty()) return null
        return mStack.pop()
    }

    fun removeUntil(
        navOptions: NavOptions,
        kClassNavigate: KClass<out Fragment>,
        function: (Destination) -> Unit
    ) {
        removeAll(navOptions, kClassNavigate, function) {
            mStack.pop().also(function)
        }
    }

    fun removeAllIgnore(
        navOptions: NavOptions,
        kClassNavigate: KClass<out Fragment>,
        function: (Destination) -> Unit
    ) {
        removeAll(navOptions, kClassNavigate, function)
    }

    private fun removeAll(
        navOptions: NavOptions,
        kClassNavigate: KClass<out Fragment>,
        onNext: (Destination) -> Unit,
        doFinish: ((Destination) -> Destination?)? = null
    ) {
        var isOverDestination = false
        var singleTop: Destination? = null
        while (true) {
            if (mStack.empty()) break
            val current = mStack.lastElement()
            var isAtSingleTop = false

            if (current.kClass == kClassNavigate) {
                isOverDestination = true
                if (navOptions.singleTop) isAtSingleTop = true
            }

            if (current.kClass == navOptions.popupTo && isOverDestination) {
                val next = doFinish?.invoke(current)
                if (isAtSingleTop) singleTop = next
                break
            }

            val next = mStack.pop().also(onNext)
            if (isAtSingleTop) singleTop = next
        }
        singleTop?.also { mStack.push(it) }
    }

    override fun toString(): String {
        return mStack.joinToString()
    }

    fun onSaveInstance(state: Bundle) {
        val bundle = Bundle()
        mStack.forEachIndexed { index, destination ->
            bundle.putBundle(index.toString(), destination.toBundle())
        }
        state.putBundle(STACK, bundle)
    }

    fun onRestoreInstance(saved: Bundle) {
        val bundle = saved.getBundle(STACK) ?: error("Error restore state")
        bundle.keySet().forEach {
            mStack.push(
                Destination.from(
                    bundle.getBundle(it) ?: error("Error restore destination")
                )
            )
        }
    }

    fun remove(des: Destination) {
        mStack.remove(des)
    }

    fun push(des: Destination) {
        mStack.push(des)
    }

    companion object {
        private const val STACK = "android:destination:stack"
    }
}

class Destination(
    val kClass: KClass<out Fragment>,
    private val tagId: Long,
    val keepInstance: Boolean = false
) {

    var animPopExit: Int = 0
    var animPopEnter: Int = 0
    var animEnter: Int = 0
    var animExit: Int = 0
    val tag: String =
        "navigator:destination:${kClass.java.simpleName}:tag:${if (keepInstance) "single" else "new"}:$tagId"

    private val mLog = "${kClass.java.simpleName}:$tagId"

    fun createFragment(): Fragment {
        return kClass.java.getConstructor().newInstance()
    }

    override fun toString(): String {
        return mLog
    }

    fun toBundle(): Bundle {
        return Bundle().also {
            it.putString(CLASS, kClass.java.name)
            it.putLong(TAG_ID, tagId)
            it.putBoolean(KEEP_INSTANCE, keepInstance)
        }
    }

    companion object {
        private const val CLASS = "android:destination:class:name"
        private const val TAG_ID = "android:destination:tagId"
        private const val KEEP_INSTANCE = "android:destination:keep:instance"

        fun from(bundle: Bundle): Destination {
            return Destination(
                Class.forName(
                    bundle.getString(CLASS) ?: error("Not found class name")
                ).asSubclass(Fragment::class.java).kotlin,
                bundle.getLong(TAG_ID),
                bundle.getBoolean(KEEP_INSTANCE)
            )
        }
    }
}