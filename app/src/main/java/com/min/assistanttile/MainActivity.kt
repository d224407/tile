package com.min.assistanttile

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * Không có UI thật. Mở app chỉ để hướng dẫn người dùng thêm tile
 * vào thanh Quick Settings, rồi tự đóng lại.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(
            this,
            "Kéo thanh thông báo xuống 2 lần, bấm bút chì (Edit) rồi kéo tile \"Assistant\" vào Quick Settings.",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }
}
