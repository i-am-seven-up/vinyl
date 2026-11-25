package com.vinyl.shared.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Song::class], version = 1, exportSchema = true)
internal abstract class VinylDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
