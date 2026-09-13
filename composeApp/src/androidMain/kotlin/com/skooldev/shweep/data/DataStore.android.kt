package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.skooldev.shweep.ShweepApplication

actual fun createDataStore(): DataStore<Preferences> {
    return ShweepApplication.instance.getDataStore()
}