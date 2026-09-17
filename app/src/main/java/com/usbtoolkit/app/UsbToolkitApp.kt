package com.usbtoolkit.app

import android.app.Application
import com.usbtoolkit.app.data.SettingsRepository
import com.usbtoolkit.app.usb.UsbMonitor

class UsbToolkitApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var usbMonitor: UsbMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        usbMonitor = UsbMonitor(this)
        usbMonitor.start()
    }

    override fun onTerminate() {
        usbMonitor.stop()
        super.onTerminate()
    }
}
