package br.com.paymentsimulator.data.local

import android.content.Context
import androidx.room.Room

fun createAppDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "payment-simulator.db",
    )
        // The simulator keeps no real history, so an older local file can be recreated.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
