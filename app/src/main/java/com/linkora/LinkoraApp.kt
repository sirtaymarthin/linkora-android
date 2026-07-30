package com.linkora

import android.app.Application
import androidx.room.Room
import com.linkora.data.LinkoraDb

class LinkoraApp : Application() {
    lateinit var db: LinkoraDb
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, LinkoraDb::class.java, "linkora.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
