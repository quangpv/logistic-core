package com.support.core.widget

import android.app.Dialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.support.core.R
import com.support.core.phone.IPhoneCode
import kotlinx.android.synthetic.main.dialog_phone_code_picker.*
import kotlinx.android.synthetic.main.item_view_phone_code.view.*

class PhoneCodePickerDialog(context: Context, val style: Int = R.style.PhoneCodeTheme) : Dialog(context, style) {
    private var mAdapter: Adapter
    private var mCallback: (IPhoneCode) -> Unit = {}
    private var mItems: List<IPhoneCode> = emptyList()
    private var mOriginal = arrayListOf<IPhoneCode>()

    init {
        setContentView(R.layout.dialog_phone_code_picker)
        mAdapter = Adapter()
        rvLanguage.adapter = mAdapter
        edtSearch.addTextChangedListener(PhoneSearching())
        setCancelable(true)
    }

    fun show(phoneCodes: List<IPhoneCode>, function: (IPhoneCode) -> Unit) {
        mCallback = function
        mOriginal.clear()
        mOriginal.addAll(phoneCodes)

        mItems = phoneCodes
        mAdapter.notifyDataSetChanged()
        super.show()
    }

    private fun filter(key: String) {
        mItems = mOriginal.filter {
            "${it.code} ${it.dialCode} ${it.name}".contains(key, true)
        }
        mAdapter.notifyDataSetChanged()
    }

    private inner class Adapter : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: onCreateView(parent)
            onBindView(view, position)
            return view
        }

        override fun getItem(position: Int): IPhoneCode {
            return mItems[position]
        }

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = mItems.size

        private fun onBindView(view: View, position: Int): Unit = with(view) {
            val item = getItem(position)
            txtCountryName.text = item.name
            txtDialCode.text = item.dialCode
            setOnClickListener {
                mCallback(item)
                dismiss()
            }
        }

        private fun onCreateView(parent: ViewGroup): View {
            return inflater.inflate(R.layout.item_view_phone_code, parent, false)
        }
    }

    private inner class PhoneSearching : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            filter(s.toString())
        }
    }
}
