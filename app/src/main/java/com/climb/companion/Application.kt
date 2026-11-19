package com.climb.companion

import android.app.Application

class CruxApplication : Application() {
    val db by lazy { AppDatabase.getInstance(this) }
}