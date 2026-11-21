package com.climb.companion

import android.app.Application
import com.climb.companion.data.AppDatabase

class CruxApplication : Application() {
    val db by lazy { AppDatabase.getInstance(this) }
}