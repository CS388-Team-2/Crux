package com.climb.companion.data

import android.content.Context
import com.climb.companion.data.entity.CatalogClimb
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Database(entities = [CatalogClimb::class], version = 1)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        private const val DB_NAME = "catalog-db"
        private const val ASSET_NAME = "feed_climbs.json"

        @Volatile
        private var INSTANCE: CatalogDatabase? = null

        private val loadMutex = Mutex()

        fun getInstance(context: Context): CatalogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatalogDatabase::class.java,
                    DB_NAME,
                ).build().also { INSTANCE = it }
            }

        suspend fun ensureLoaded(context: Context) {
            val appContext = context.applicationContext
            val database = getInstance(appContext)
            loadMutex.withLock {
                withContext(Dispatchers.IO) {
                    val dao = database.catalogDao()
                    if (dao.count() == 0) {
                        dao.insertAll(readClimbs(appContext))
                    }
                }
            }
        }

        private fun readClimbs(context: Context): List<CatalogClimb> {
            val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            return List(array.length()) { index ->
                val item = array.getJSONObject(index)
                CatalogClimb(
                    uuid = item.getString("uuid"),
                    name = item.getString("name"),
                    grade = item.getString("grade"),
                    angle = item.getInt("angle"),
                    position = index,
                )
            }
        }
    }
}
