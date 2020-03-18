package com.support.core.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import java.io.Serializable
import java.util.*
import kotlin.reflect.KClass

abstract class Navigator(private val fragmentManager: FragmentManager, @IdRes val container: Int) {
    abstract val lastDestination: Destination?
    var onNavigateChangedListener: (KClass<*>) -> Unit = {}
    private val mTransactionManager = TransactionManager()

    abstract fun navigate(
        kClass: KClass<out Fragment>,
        args: Bundle? = null,
        navOptions: NavOptions? = null
    )

    abstract fun navigateUp(): Boolean

    open fun onSaveInstance(state: Bundle) {}

    open fun onRestoreInstance(saved: Bundle) {}

    protected fun transaction(function: FragmentTransaction. () -> Unit) {
        mTransactionManager.push(Transaction(function))
    }

    private inner class TransactionManager {
        private val mTransactions = ArrayDeque<Transaction>()
        private val isEmpty get() = mTransactions.isEmpty()
        private val next get() = mTransactions.takeIf { it.isNotEmpty() }?.peekFirst()

        fun push(transaction: Transaction) = synchronized(this) {
            transaction.onFinishListener = {
                mTransactions.pop()
                next?.execute()
            }
            if (isEmpty) {
                mTransactions.add(transaction)
                transaction.execute()
            } else {
                mTransactions.add(transaction)
            }
        }
    }

    private inner class Transaction(private val function: FragmentTransaction.() -> Unit) {
        var onFinishListener: (() -> Unit)? = null

        fun execute() {
            fragmentManager.beginTransaction().also {
                it.function()
                it.runOnCommit { onFinishListener?.invoke() }
                it.commit()
            }
        }
    }

}

fun Navigator.navigate(
    kClass: KClass<out Fragment>,
    args: FragmentParams? = null,
    navOptions: NavOptions? = null
) {
    navigate(kClass, args?.toBundle(), navOptions)
}

interface FragmentParams : Serializable {
    fun toBundle(): Bundle = Bundle().also {
        it.putSerializable(KEY, this)
    }

    companion object {
        const val KEY = "fragment:arguments"

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : Serializable> from(bundle: Bundle?): T {
            return (bundle?.getSerializable(KEY) as? T)
                ?: error("Can not to cast to ${T::class.java.simpleName}")
        }
    }
}

interface ArgumentChangeable {
    fun onNewArguments(arguments: Bundle)
}

fun FragmentActivity.findNavigator(@IdRes containerId: Int = 0): Navigator {
    if (this is NavigationOwner) {
        if (containerId == 0) return this.navigator
        else if (containerId == this.navigator.container) return this.navigator
    }

    if (containerId == 0) return supportFragmentManager.fragments.find { it is NavHostFragment }?.findNavigator()
        ?: error("Not found navigator")

    return (supportFragmentManager.findFragmentById(containerId) as? NavHostFragment)?.navigator
        ?: error("Not found navigator")
}

fun Fragment.findNavigator(@IdRes containerId: Int = 0): Navigator {
    if (this is NavigationOwner) {
        if (containerId == 0) return this.navigator
        else if (containerId == this.navigator.container) return this.navigator
    }

    if (containerId != 0) {
        val navigator =
            (childFragmentManager.findFragmentById(containerId) as? NavHostFragment)?.navigator
        if (navigator != null) return navigator
    }

    if (parentFragment == null) {
        val activity = this.activity
        if (activity is NavigationOwner) {
            if (containerId == 0) return activity.navigator
            else if (containerId == activity.navigator.container) return activity.navigator
        }
        error("Not found navigator")
    }
    return parentFragment!!.findNavigator(containerId)
}

class StartFragment : Fragment()
