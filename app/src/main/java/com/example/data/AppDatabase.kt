package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _jobsFlow = MutableStateFlow<List<PrintJobEntity>>(emptyList())
    private val _shopsFlow = MutableStateFlow<List<PrintShopEntity>>(emptyList())

    init {
        scope.launch {
            loadInitialData()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS print_jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                fileUriString TEXT NOT NULL,
                printType TEXT NOT NULL,
                actionType TEXT NOT NULL,
                copies INTEGER NOT NULL,
                colorMode TEXT NOT NULL,
                paperSize TEXT NOT NULL,
                duplexMode TEXT NOT NULL,
                bindingOption TEXT NOT NULL,
                recipient TEXT,
                instructions TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS print_shops (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                notes TEXT NOT NULL,
                isFavorite INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        // Seed initial print shop contacts
        seedInitialShops(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration strategy
    }

    private fun seedInitialShops(db: SQLiteDatabase) {
        insertShopInternal(
            db,
            "Centre d'Impression Moderne",
            "impression.centre@example.com",
            "+33 6 12 34 56 78",
            "Ouvert 8h-19h - A4/A3, reliure spirale et couleur",
            1
        )
        insertShopInternal(
            db,
            "Cyber Print & Copie Express",
            "cyber.print@example.com",
            "+33 7 98 76 54 32",
            "Impression rapide documents & photos sur papier brillant",
            0
        )
        insertShopInternal(
            db,
            "مركز النور لخدمات الطباعة والنسخ",
            "alnoor.print@example.com",
            "+212 6 00 11 22 33",
            "طباعة مستندات، أطروحات، صور فوتوغرافية وتجليد فاخر",
            1
        )
    }

    private fun insertShopInternal(
        db: SQLiteDatabase,
        name: String,
        email: String,
        phone: String,
        notes: String,
        isFavorite: Int
    ): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("notes", notes)
            put("isFavorite", isFavorite)
        }
        return db.insert("print_shops", null, values)
    }

    private suspend fun loadInitialData() = withContext(Dispatchers.IO) {
        refreshJobs()
        refreshShops()
    }

    private fun refreshJobs() {
        val list = mutableListOf<PrintJobEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM print_jobs ORDER BY timestamp DESC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToJob(c))
            }
        }
        _jobsFlow.value = list
    }

    private fun refreshShops() {
        val list = mutableListOf<PrintShopEntity>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM print_shops ORDER BY isFavorite DESC, name ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToShop(c))
            }
        }
        _shopsFlow.value = list
    }

    private fun cursorToJob(c: Cursor): PrintJobEntity {
        return PrintJobEntity(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            title = c.getString(c.getColumnIndexOrThrow("title")),
            fileUriString = c.getString(c.getColumnIndexOrThrow("fileUriString")),
            printType = c.getString(c.getColumnIndexOrThrow("printType")),
            actionType = c.getString(c.getColumnIndexOrThrow("actionType")),
            copies = c.getInt(c.getColumnIndexOrThrow("copies")),
            colorMode = c.getString(c.getColumnIndexOrThrow("colorMode")),
            paperSize = c.getString(c.getColumnIndexOrThrow("paperSize")),
            duplexMode = c.getString(c.getColumnIndexOrThrow("duplexMode")),
            bindingOption = c.getString(c.getColumnIndexOrThrow("bindingOption")),
            recipient = c.getString(c.getColumnIndexOrThrow("recipient")),
            instructions = c.getString(c.getColumnIndexOrThrow("instructions")),
            timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
        )
    }

    private fun cursorToShop(c: Cursor): PrintShopEntity {
        return PrintShopEntity(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            name = c.getString(c.getColumnIndexOrThrow("name")),
            email = c.getString(c.getColumnIndexOrThrow("email")),
            phone = c.getString(c.getColumnIndexOrThrow("phone")),
            notes = c.getString(c.getColumnIndexOrThrow("notes")),
            isFavorite = c.getInt(c.getColumnIndexOrThrow("isFavorite")) == 1
        )
    }

    val printJobDaoImpl = object : PrintJobDao {
        override fun getAllJobs(): Flow<List<PrintJobEntity>> = _jobsFlow.asStateFlow()

        override suspend fun insertJob(job: PrintJobEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("title", job.title)
                put("fileUriString", job.fileUriString)
                put("printType", job.printType)
                put("actionType", job.actionType)
                put("copies", job.copies)
                put("colorMode", job.colorMode)
                put("paperSize", job.paperSize)
                put("duplexMode", job.duplexMode)
                put("bindingOption", job.bindingOption)
                put("recipient", job.recipient)
                put("instructions", job.instructions)
                put("timestamp", job.timestamp)
            }
            val id = db.insertWithOnConflict("print_jobs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshJobs()
            id
        }

        override suspend fun deleteJob(job: PrintJobEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("print_jobs", "id = ?", arrayOf(job.id.toString()))
            refreshJobs()
        }

        override suspend fun clearAll() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("print_jobs", null, null)
            refreshJobs()
        }
    }

    val printShopDaoImpl = object : PrintShopDao {
        override fun getAllShops(): Flow<List<PrintShopEntity>> = _shopsFlow.asStateFlow()

        override suspend fun insertShop(shop: PrintShopEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("name", shop.name)
                put("email", shop.email)
                put("phone", shop.phone)
                put("notes", shop.notes)
                put("isFavorite", if (shop.isFavorite) 1 else 0)
            }
            val id = db.insertWithOnConflict("print_shops", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshShops()
            id
        }

        override suspend fun updateShop(shop: PrintShopEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("name", shop.name)
                put("email", shop.email)
                put("phone", shop.phone)
                put("notes", shop.notes)
                put("isFavorite", if (shop.isFavorite) 1 else 0)
            }
            db.update("print_shops", values, "id = ?", arrayOf(shop.id.toString()))
            refreshShops()
        }

        override suspend fun deleteShop(shop: PrintShopEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("print_shops", "id = ?", arrayOf(shop.id.toString()))
            refreshShops()
        }

        override suspend fun getCount(): Int = withContext(Dispatchers.IO) {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM print_shops", null)
            var count = 0
            cursor.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
            count
        }
    }

    fun printJobDao(): PrintJobDao = printJobDaoImpl
    fun printShopDao(): PrintShopDao = printShopDaoImpl

    companion object {
        private const val DATABASE_NAME = "imprimedoc_database.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
