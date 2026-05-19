plugins {
    // Các plugin hiện tại của bạn
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false

    // 🌟 THÊM ĐÚNG DÒNG NÀY VÀO ĐÂY ĐỂ ĐỊNH NGHĨA PHIÊN BẢN:
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}