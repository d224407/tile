package com.min.assistanttile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Tile trong Quick Settings: chạm vào là kích hoạt thẳng trợ lý ảo mặc định
 * (giống nhấn giữ nút Home / vuốt góc màn hình), có chụp ngữ cảnh màn hình hiện tại.
 */
class AssistantTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        // ACTION_ASSIST là action chuẩn để gọi assist app đang đặt mặc định.
        // ACTION_VOICE_COMMAND không có handler cố định trên nhiều máy nên hay
        // bị hệ thống lái sang màn hình Settings "chọn trợ lý mặc định".
        val intent = Intent(Intent.ACTION_ASSIST).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: bắt buộc phải dùng PendingIntent để mở Activity từ tile
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
