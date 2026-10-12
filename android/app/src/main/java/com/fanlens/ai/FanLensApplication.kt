package com.fanlens.ai

import android.app.Application
import com.fanlens.ai.di.ServiceLocator

class FanLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(applicationContext)
    }
}
