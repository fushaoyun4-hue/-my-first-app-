package com.watermark.inference

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 电池 & 温度监控
 *
 * Android 手机的 CPU/GPU 资源有限，处理 1080P AI 推理时发热严重。
 * 本模块在处理开始前检查：
 * - 电量 < 20% → 提示用户连接充电器
 * - 温度过高 → 暂停处理，建议冷却
 *
 * 注意：真正的电池温度需要 BatteryManager.EXTRA_TEMPERATURE，
 * 部分设备不提供，此时仅做电量检查。
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** 电池状态（快照）*/
    data class BatteryInfo(
        val level: Int,        // 电量百分比 0~100
        val isCharging: Boolean,
        val temperature: Int,  // 温度（摄氏度），-1 表示不可用
        val isOverheated: Boolean,  // 温度 > 45°C
        val canProceed: Boolean,     // 电量足够且未过热
    )

    /** 获取当前电池状态（一次性快照）*/
    fun getBatteryInfo(): BatteryInfo {
        val intent = context.registerReceiver(
            null,  // 不设置 BroadcastReceiver，直接获取当前 sticky broadcast
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return BatteryInfo(
            level = 100, isCharging = false,
            temperature = -1, isOverheated = false, canProceed = true
        )

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // 温度：Android 返回的是0.1°C单位的整数
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temperature = if (tempRaw > 0) tempRaw / 10 else -1
        val isOverheated = temperature > OVERHEAT_THRESHOLD_C

        val canProceed = percent > LOW_BATTERY_THRESHOLD || isCharging

        return BatteryInfo(
            level = percent,
            isCharging = isCharging,
            temperature = temperature,
            isOverheated = isOverheated,
            canProceed = canProceed
        )
    }

    /**
     * 实时电池/温度变化 Flow
     * 用于持续监控处理期间的温度（可选功能）
     */
    fun batteryUpdates(): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val info = parseIntent(intent)
                trySend(info)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun parseIntent(intent: Intent?): BatteryInfo {
        if (intent == null) return BatteryInfo(100, false, -1, false, true)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temperature = if (tempRaw > 0) tempRaw / 10 else -1
        val isOverheated = temperature > OVERHEAT_THRESHOLD_C

        val canProceed = percent > LOW_BATTERY_THRESHOLD || isCharging

        return BatteryInfo(percent, isCharging, temperature, isOverheated, canProceed)
    }

    companion object {
        /** 电量低于此值时提示用户充电（但充电中可跳过）*/
        const val LOW_BATTERY_THRESHOLD = 20

        /** 温度超过此值认为过热（单位：摄氏度）*/
        const val OVERHEAT_THRESHOLD_C = 45
    }
}
