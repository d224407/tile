package com.min.assistanttile

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Tile trong Quick Settings: cham vao la kich hoat thang tro ly ao dang duoc
 * dat lam mac dinh trong Settings > Ung dung tro ly mac dinh.
 *
 * De biet chinh xac app nao dang giu vai tro do (thay vi de he thong hien
 * hop thoai chon giua nhieu app), tile chay lenh
 * `cmd role get-role-holders android.app.role.ASSISTANT` -- dung lenh ma
 * man hinh Settings dung -- qua MOT TRONG HAI duong:
 *   1) Shizuku, neu app Shizuku dang chay va da cap quyen (uu tien, nhe hon).
 *   2) `su` (root truyen thong), neu Shizuku khong dung duoc.
 * Neu ca hai deu khong co, tile roi ve hanh vi cu: gui ACTION_ASSIST khong
 * kem package, de he thong tu xu ly (co the hien lai hop thoai chon app).
 */
class AssistantTileService : TileService() {

    companion object {
        private const val SHIZUKU_REQUEST_CODE = 9001
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val permissionLatch = AtomicReference<CountDownLatch?>()
    @Volatile private var shizukuPermissionGranted = false

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQUEST_CODE) {
                shizukuPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
                permissionLatch.get()?.countDown()
            }
        }

    override fun onCreate() {
        super.onCreate()
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            // Thu vien Shizuku khong khoi tao duoc (khong co app Shizuku...) -> bo qua,
            // se tu roi ve nhanh su/fallback khi bam tile.
        }
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            // no-op
        }
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()

        // Ca hai duong (Shizuku, su) deu can chay tren thread rieng de khong
        // chan TileService / gay ANR.
        Thread {
            val assistantPackage = queryDefaultAssistantPackage()

            val intent = Intent(Intent.ACTION_ASSIST).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (!assistantPackage.isNullOrBlank()) {
                    setPackage(assistantPackage)
                }
            }

            mainHandler.post { launchAssist(intent) }
        }.start()
    }

    private fun queryDefaultAssistantPackage(): String? {
        queryViaShizuku()?.let { return it }
        return queryViaRoot()
    }

    private val ASSISTANT_ROLE_CMD =
        arrayOf("cmd", "role", "get-role-holders", "android.app.role.ASSISTANT")

    // ---------- Duong 1: Shizuku ----------

    private fun queryViaShizuku(): String? {
        return try {
            if (!Shizuku.pingBinder()) return null

            val granted = if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                requestShizukuPermissionAndWait()
            }
            if (!granted) return null

            execViaShizuku(ASSISTANT_ROLE_CMD)
        } catch (e: Throwable) {
            null
        }
    }

    /** Xin quyen Shizuku va cho toi da 15 giay de nguoi dung bam "Cho phep" trong app Shizuku. */
    private fun requestShizukuPermissionAndWait(): Boolean {
        val latch = CountDownLatch(1)
        permissionLatch.set(latch)
        mainHandler.post {
            try {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            } catch (e: Throwable) {
                latch.countDown()
            }
        }
        latch.await(15, TimeUnit.SECONDS)
        return shizukuPermissionGranted
    }

    /**
     * Shizuku khong co API cong khai de chay lenh shell (Shizuku.newProcess la
     * method an, khong nam trong API cong khai cua thu vien) -> lay qua reflection,
     * la cach cac plugin Shizuku trong cong dong van dung.
     */
    private fun execViaShizuku(cmd: Array<String>): String? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val process = method.invoke(null, cmd, null, null) as Process
            val output = BufferedReader(InputStreamReader(process.inputStream))
                .readText()
                .trim()
            process.waitFor()
            output.lineSequence().firstOrNull { it.isNotBlank() }
        } catch (e: Throwable) {
            null
        }
    }

    // ---------- Duong 2: su (root) ----------

    private fun queryViaRoot(): String? {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("su", "-c", ASSISTANT_ROLE_CMD.joinToString(" "))
            )
            val output = BufferedReader(InputStreamReader(process.inputStream))
                .readText()
                .trim()
            process.waitFor()
            output.lineSequence().firstOrNull { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun launchAssist(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
