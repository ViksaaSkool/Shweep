package com.skooldev.shweep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(): DataStore<Preferences>