package com.aile.takip

import android.app.Application
import com.aile.takip.data.db.AppDatabase

class AileTakipApp : Application() {
    val db by lazy { AppDatabase.get(this) }
}
