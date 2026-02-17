package com.skooldev.shweep

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

class ShweepApplication : Application() {
    
    private val dataStoreInstance: DataStore<Preferences> by preferencesDataStore(name = "shweep_sessions")
    
    fun getDataStore(): DataStore<Preferences> = dataStoreInstance
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: ShweepApplication
            private set
    }
}