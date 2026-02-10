package com.optik.cengkareng.ui.detail

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.optik.cengkareng.data.local.entity.CustomerEntity

class CustomerPagerAdapter(
    fragment: Fragment,
    private val customer: CustomerEntity?
) : FragmentStateAdapter(fragment) {

    // Kita simpan instance fragment agar nanti bisa diakses
    val infoFragment = CustomerInfoFragment.newInstance(customer ?: CustomerEntity(nama = "", noHp = ""))
    val historyFragment = CustomerHistoryFragment()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> infoFragment
            1 -> historyFragment
            else -> throw IllegalArgumentException("Posisi tab tidak valid: $position")
        }
    }
}