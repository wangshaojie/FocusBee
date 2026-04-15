package com.animalgame

import android.app.Application
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

class AnimalGameApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initUMeng()
    }

    private fun initUMeng() {
        // 配置友盟参数
        // appKey: 69cf821c9a7f376488b3e1a2
        // channel: Google Play
        // deviceType: PHONE
        UMConfigure.init(
            this,
            "69cf821c9a7f376488b3e1a2",
            "Google Play",
            UMConfigure.DEVICE_TYPE_PHONE,
            null
        )

        // 设置日志开关（生产环境建议关闭）
        UMConfigure.setLogEnabled(false)

        // 设置 session 超时间隔（默认 30 秒）
        MobclickAgent.setSessionContinueMillis(30000L)
    }
}