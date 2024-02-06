package com.example.clickdevice.helper

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.clickdevice.bean.ActionScript

class FragmentHelper<T : Fragment>(
    private var fragmentManager: FragmentManager,
    private var id: Int = 0,
    private var tag: String,
    private var fragmentCreate: () -> T
) {

    private var fragment: T? = null

    private fun createFragment(): T {
        return fragmentCreate()
    }

    private fun initFragment() {
        if (fragment == null) {
            var findFragment = findFragment()
            fragment = findFragment ?: createFragment()
        }
    }

    fun getFragment(): T {
        initFragment()
        return fragment!!
    }

    fun showFragment(now: Boolean = false): T {
        initFragment()
        if (findFragment() == null) {
            add(now)
        } else {
            show(now)
        }
        return fragment!!
    }

    fun hideFragment(now: Boolean = false) {
        initFragment()
        if (findFragment() != null) {
            hide(now)
        }
    }

    fun removeFragment(now: Boolean = false) {
        initFragment()
        if (findFragment() != null) {
            remove(now)
        }
    }


    private fun add(now: Boolean) {
        if (now) {
            fragmentManager.beginTransaction().add(id, fragment!!, tag).commitNow()
        } else {
            fragmentManager.beginTransaction().add(id, fragment!!, tag).commit()
        }
    }

    private fun show(now: Boolean) {
        if (now) {
            fragmentManager.beginTransaction().show(fragment!!).commitNow()
        } else {
            fragmentManager.beginTransaction().show(fragment!!).commit()
        }
    }

    private fun hide(now: Boolean) {
        if (now) {
            fragmentManager.beginTransaction().hide(fragment!!).commitNow()
        } else {
            fragmentManager.beginTransaction().hide(fragment!!).commit()
        }
    }

    private fun remove(now: Boolean) {
        if (now) {
            fragmentManager.beginTransaction().remove(fragment!!).commitNow()
        } else {
            fragmentManager.beginTransaction().remove(fragment!!).commit()
        }
    }

    private fun findFragment(): T? {
        return fragmentManager.findFragmentByTag(tag) as T?
    }

}