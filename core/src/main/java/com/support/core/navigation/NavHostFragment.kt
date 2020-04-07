package com.support.core.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.support.core.base.BaseFragment
import kotlin.reflect.KClass

interface Backable {
    fun onInterceptBackPress() = false
}

class NavHostFragment : BaseFragment(0), NavigationOwner, Backable {
    companion object {
        const val START_CLASS_NAME = "fragment:navigation:start:class"
        const val NAV_OPTION = "fragment:navigation:options"
        const val ARGUMENT = "fragment:navigation:argument"
        const val ID = "fragment:navigation:id"
    }

    private var mNavigator: Navigator? = null
    override val navigator: Navigator
        get() = mNavigator ?: error("Navigator not init yet!")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNavigator = FragmentNavigator(childFragmentManager, args(ID) ?: id)
        if (savedInstanceState != null) mNavigator?.onRestoreInstance(savedInstanceState)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return FrameLayout(requireContext()).also { it.id = args(ID) ?: id }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        doNavigateIfNeeded()
    }

    private fun doNavigateIfNeeded() {
        val startClassName: String? = arguments?.getString(START_CLASS_NAME)
        val bundleArgs: Bundle? = arguments?.getBundle(ARGUMENT)
        val navOptions: NavOptions? = arguments?.getParcelable(NAV_OPTION)
        if (startClassName.isNullOrEmpty()) return
        mNavigator?.navigate(
                Class.forName(startClassName).asSubclass(Fragment::class.java).kotlin,
                bundleArgs,
                navOptions
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mNavigator?.onSaveInstance(outState)
    }

    override fun onInterceptBackPress(): Boolean {
        return mNavigator?.navigateUp() ?: false
    }
}

fun Navigator.navigateHost(@IdRes id: Int, kClass: KClass<out Fragment>, args: Bundle? = null, navOptions: NavOptions? = null) {
    val bundle = Bundle()
    bundle.putString(NavHostFragment.START_CLASS_NAME, kClass.java.name)
    bundle.putInt(NavHostFragment.ID, id)
    bundle.putParcelable(NavHostFragment.NAV_OPTION, navOptions)
    bundle.putBundle(NavHostFragment.ARGUMENT, args)
    navigate(NavHostFragment::class, bundle)
}