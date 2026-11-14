package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoomEntity::class,
        TenantEntity::class,
        RentEntryEntity::class,
        SuppressedRentCycleEntity::class,
        ElectricityBillEntity::class,
        PaymentReceiptEntity::class,
        PaymentAllocationEntity::class,
        AdvanceTransactionEntity::class,
        WriteOffEntity::class,
        FollowUpEntity::class,
        RoomTransferEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun tenantDao(): TenantDao
    abstract fun rentDao(): RentDao
    abstract fun electricityDao(): ElectricityDao
    abstract fun paymentDao(): PaymentDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun writeOffDao(): WriteOffDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun roomTransferDao(): RoomTransferDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val TOTAL_ROOMS = 13

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rrr_rentals.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            db.execSQL("INSERT OR IGNORE INTO app_settings (id, defaultElectricityRatePaise, nextReceiptSeq, lastBackupEpochMs, backupReminderDays) VALUES (1, 800, 1, 0, 7)")
                            for (room in 1..TOTAL_ROOMS) {
                                db.execSQL("INSERT OR IGNORE INTO rooms (roomNumber, roomLabel, currentTenantId, lastMeterReading) VALUES ($room, 'Room $room', NULL, 0.0)")
                            }
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
