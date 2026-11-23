package com.climb.companion.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.FileOutputStream

enum class KilterSort {
    MOST,
    LEAST,
    TOP,
}

object KilterCatalog {
    private const val ASSET_NAME = "kilter.db"
    private const val FILE_NAME = "kilter.db"
    private const val PREFS = "kilter_catalog"
    private const val KEY_VERSION = "version"
    private const val VERSION = 2
    private const val CLIMB_COLUMNS =
        "uuid, name, grade, angle, setter, layout_name, logged_count, quality, " +
            "IFNULL((SELECT frames FROM climb_frames WHERE uuid = climbs.uuid), '')"
    private const val PAGE_SIZE = 30
    private const val SAVED_ID_LIMIT = 900

    const val TOP_LIMIT = 50

    data class Page(
        val climbs: List<KilterClimb>,
        val hasMore: Boolean,
    )

    @Volatile
    private var database: SQLiteDatabase? = null

    @Volatile
    private var boardLayout: BoardLayout? = null

    private val gate = Any()

    fun page(
        context: Context,
        minGrade: Int,
        maxGrade: Int,
        angle: Int?,
        sort: KilterSort,
        hideSaved: Boolean,
        savedIds: Set<String>,
        offset: Int,
    ): Page {
        val remaining = if (sort == KilterSort.TOP) TOP_LIMIT - offset else PAGE_SIZE
        if (remaining <= 0) return Page(emptyList(), hasMore = false)
        val limit = minOf(PAGE_SIZE, remaining)
        val (where, args) = selection(minGrade, maxGrade, angle, hideSaved, savedIds)
        val order = if (sort == KilterSort.LEAST) {
            "logged_count ASC, name COLLATE NOCASE ASC"
        } else {
            "logged_count DESC, name COLLATE NOCASE ASC"
        }
        val queryArgs = args + limit.toString() + offset.toString()
        val climbs = query(
            context,
            """
            SELECT $CLIMB_COLUMNS
            FROM climbs
            WHERE $where
            ORDER BY $order
            LIMIT ? OFFSET ?
            """.trimIndent(),
            queryArgs,
        )
        val hasMore = climbs.size == limit &&
            (sort != KilterSort.TOP || offset + climbs.size < TOP_LIMIT)
        return Page(climbs, hasMore)
    }

    fun count(
        context: Context,
        minGrade: Int,
        maxGrade: Int,
        angle: Int?,
        sort: KilterSort,
        hideSaved: Boolean,
        savedIds: Set<String>,
    ): Int {
        val (where, args) = selection(minGrade, maxGrade, angle, hideSaved, savedIds)
        val database = open(context)
        database.rawQuery(
            "SELECT COUNT(*) FROM climbs WHERE $where",
            args,
        ).use { cursor ->
            val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
            return if (sort == KilterSort.TOP) minOf(total, TOP_LIMIT) else total
        }
    }

    fun find(context: Context, uuid: String, angle: Int): KilterClimb? {
        val matches = query(
            context,
            """
            SELECT $CLIMB_COLUMNS
            FROM climbs
            WHERE uuid = ? AND angle = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(uuid, angle.toString()),
        )
        if (matches.isNotEmpty()) return matches.first()
        return query(
            context,
            """
            SELECT $CLIMB_COLUMNS
            FROM climbs
            WHERE uuid = ?
            ORDER BY logged_count DESC
            LIMIT 1
            """.trimIndent(),
            arrayOf(uuid),
        ).firstOrNull()
    }

    fun board(context: Context): BoardLayout {
        boardLayout?.let { return it }
        synchronized(gate) {
            boardLayout?.let { return it }
            val database = open(context)
            val placements = HashMap<Int, BoardPlacement>()
            database.rawQuery(
                """
                SELECT p.id, h.x, h.y, p.set_id
                FROM placements p
                JOIN holes h ON h.id = p.hole_id
                """.trimIndent(),
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    placements[cursor.getInt(0)] = BoardPlacement(
                        point = BoardPoint(cursor.getInt(1), cursor.getInt(2)),
                        setId = cursor.getInt(3),
                    )
                }
            }
            val roles = HashMap<Int, String>()
            database.rawQuery("SELECT id, name FROM roles", null).use { cursor ->
                while (cursor.moveToNext()) {
                    roles[cursor.getInt(0)] = cursor.getString(1)
                }
            }
            val layout = BoardLayout(
                placements = placements,
                roles = roles,
            )
            boardLayout = layout
            return layout
        }
    }

    private fun selection(
        minGrade: Int,
        maxGrade: Int,
        angle: Int?,
        hideSaved: Boolean,
        savedIds: Set<String>,
    ): Pair<String, Array<String>> {
        val low = minOf(minGrade, maxGrade)
        val high = maxOf(minGrade, maxGrade)
        val clauses = mutableListOf("v_grade BETWEEN ? AND ?")
        val args = mutableListOf(low.toString(), high.toString())
        if (angle != null) {
            clauses += "angle = ?"
            args += angle.toString()
        }
        if (hideSaved && savedIds.isNotEmpty() && savedIds.size <= SAVED_ID_LIMIT) {
            clauses += "uuid NOT IN (${savedIds.joinToString(",") { "?" }})"
            args += savedIds
        }
        return clauses.joinToString(" AND ") to args.toTypedArray()
    }

    private fun query(context: Context, sql: String, args: Array<String>): List<KilterClimb> {
        val database = open(context)
        database.rawQuery(sql, args).use { cursor ->
            val climbs = ArrayList<KilterClimb>(cursor.count)
            while (cursor.moveToNext()) {
                climbs += KilterClimb(
                    uuid = cursor.getString(0),
                    name = cursor.getString(1),
                    grade = cursor.getString(2),
                    angle = cursor.getInt(3),
                    setter = cursor.getString(4),
                    layoutName = cursor.getString(5),
                    loggedCount = cursor.getInt(6),
                    quality = cursor.getDouble(7),
                    frames = cursor.getString(8) ?: "",
                )
            }
            return climbs
        }
    }

    private fun open(context: Context): SQLiteDatabase {
        database?.let { if (it.isOpen) return it }
        synchronized(gate) {
            database?.let { if (it.isOpen) return it }
            val appContext = context.applicationContext
            val file = appContext.getDatabasePath(FILE_NAME)
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getInt(KEY_VERSION, 0) != VERSION || !file.exists()) {
                file.parentFile?.mkdirs()
                appContext.assets.open(ASSET_NAME).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                prefs.edit().putInt(KEY_VERSION, VERSION).apply()
            }
            val opened = SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            database = opened
            return opened
        }
    }
}
