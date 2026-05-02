package com.example.smartsous.core.common

import androidx.compose.ui.unit.dp

object Spacing {
    val xs  = 4.dp   // khoảng cách siêu nhỏ: giữa icon và text
    val sm  = 8.dp   // nhỏ: padding bên trong chip, badge
    val md  = 16.dp  // vừa: padding card, giữa các section
    val lg  = 24.dp  // lớn: padding màn hình (horizontal)
    val xl  = 32.dp  // rất lớn: khoảng giữa các khối lớn
    val xxl = 48.dp  // đặc biệt: hero section, onboarding
}

object Radius {
    val sm  = 8.dp   // chip, badge, button nhỏ
    val md  = 12.dp  // card thường
    val lg  = 16.dp  // card lớn, bottom sheet
    val xl  = 24.dp  // modal, hero card
    val full = 100.dp // pill shape (tròn hoàn toàn)
}