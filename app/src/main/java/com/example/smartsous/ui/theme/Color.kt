package com.example.smartsous.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand colors ──────────────────────────────────────────
// Màu chủ đạo: tím ấm — gợi cảm giác ngon miệng, hiện đại
val Purple50  = Color(0xFFEEEDFE)
val Purple100 = Color(0xFFCECBF6)
val Purple400 = Color(0xFF7F77DD)  // primary
val Purple600 = Color(0xFF534AB7)  // primary dark
val Purple800 = Color(0xFF3C3489)  // primary darker

// Màu phụ: xanh lá — tươi, thực phẩm, dinh dưỡng
val Teal50  = Color(0xFFE1F5EE)
val Teal400 = Color(0xFF1D9E75)   // secondary
val Teal600 = Color(0xFF0F6E56)
val Teal800 = Color(0xFF085041)

// Accent: cam san hô — nổi bật cho CTA, badge hết hạn
val Coral50  = Color(0xFFFAECE7)
val Coral400 = Color(0xFFD85A30)  // error / warning
val Coral600 = Color(0xFF993C1D)

// Amber — dùng cho badge sắp hết hạn (cảnh báo nhẹ)
val Amber50  = Color(0xFFFAEEDA)
val Amber400 = Color(0xFFBA7517)
val Amber600 = Color(0xFF854F0B)

// ── Neutral ───────────────────────────────────────────────
val Gray50  = Color(0xFFF5F4F0)   // background page
val Gray100 = Color(0xFFD3D1C7)   // divider, border
val Gray400 = Color(0xFF888780)   // placeholder text
val Gray800 = Color(0xFF444441)   // body text
val Gray900 = Color(0xFF1A1745)   // heading text (dark purple-toned)

val White   = Color(0xFFFFFFFF)
val Black   = Color(0xFF111111)

// ── Semantic colors (dùng theo nghĩa, không theo màu) ─────
val ColorSuccess = Teal400
val ColorError   = Coral400
val ColorWarning = Amber400
val ColorInfo    = Purple400

// ── Surface colors ────────────────────────────────────────
val SurfaceLight = White
val SurfaceDark  = Color(0xFF1C1B2E)   // dark mode background
val CardLight    = White
val CardDark     = Color(0xFF27263D)