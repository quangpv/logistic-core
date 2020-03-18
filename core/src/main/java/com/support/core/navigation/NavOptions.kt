package com.support.core.navigation

import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

class NavOptions(
        val popupTo: KClass<out Fragment>? = null,
        val inclusive: Boolean = false,
        val singleTop: Boolean = false,
        val singleInstance: Boolean = false,

        val animEnter: Int = 0,
        val animExit: Int = 0,

        val animPopEnter: Int = 0,
        val animPopExit: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
            Class.forName(parcel.readString()
                    ?: error("No class name")).asSubclass(Fragment::class.java).kotlin,
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (inclusive) 1 else 0)
        parcel.writeByte(if (singleTop) 1 else 0)
        parcel.writeByte(if (singleInstance) 1 else 0)
        parcel.writeInt(animEnter)
        parcel.writeInt(animExit)
        parcel.writeInt(animPopEnter)
        parcel.writeInt(animPopExit)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NavOptions> {
        override fun createFromParcel(parcel: Parcel): NavOptions {
            return NavOptions(parcel)
        }

        override fun newArray(size: Int): Array<NavOptions?> {
            return arrayOfNulls(size)
        }
    }


}