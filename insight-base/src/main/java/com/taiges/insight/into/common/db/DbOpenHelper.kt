package com.taiges.insight.into.common.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import com.taiges.insight.into.bean.UploadEventData
import com.taiges.insight.into.common.CommonUtil
import com.taiges.insight.into.common.GsonManager
import com.taiges.insight.into.common.encrypt.AesUtil
import com.taiges.insight.into.common.getLongData
import com.taiges.insight.into.common.getStringData
import com.taiges.insight.into.common.log.ILog

/**
 * 数据库存储工具类
 */
internal class DbOpenHelper(context: Context) :
    SQLiteOpenHelper(
        context,
        "insight_into.db",
        null,
        1
    ) {

    companion object {

        private const val TABLE_INSIGHT_INTO_EVENT = "insight_into_event"

        /**
         * 事件采集表 SQL
         */
        private const val CREATE_INSIGHT_INTO_EVENT_DATA_SQL =
            "CREATE TABLE IF NOT EXISTS $TABLE_INSIGHT_INTO_EVENT(uploadId text PRIMARY KEY,data text,type integer,isEncrypt integer, createTime timestamp,remark text)"

        private var dbOpenHelper: DbOpenHelper? = null
        private val lockAny = Any()

        fun getInstance(context: Context): DbOpenHelper {
            if (dbOpenHelper == null) {
                synchronized(lockAny) {
                    if (dbOpenHelper == null) {
                        dbOpenHelper = DbOpenHelper(context)
                    }
                }
            }
            return dbOpenHelper!!
        }

        /**
         * 保存事件
         */
        @Synchronized
        fun saveEventRequest(context: Context, uploadEventData: UploadEventData) {
            val data = GsonManager.gson.toJson(uploadEventData)
            val encryptData = AesUtil.encryptStr(data, CommonUtil.akx)

            val contentValues = ContentValues()
            contentValues.put("uploadId", uploadEventData.uploadId)
            contentValues.put("data", encryptData)
            contentValues.put("createTime", System.currentTimeMillis())
            contentValues.put("type", 1)
            contentValues.put("isEncrypt", 1)
            contentValues.put("remark", "")

            getInstance(context).insertData(TABLE_INSIGHT_INTO_EVENT, contentValues)
        }

        /**
         * 删除事件
         */
        @Synchronized
        fun deleteEventRequest(context: Context, uploadId: String) {
            val selection = "uploadId=?"
            val selectionArgs = arrayOf(uploadId)
            getInstance(context).deleteData(TABLE_INSIGHT_INTO_EVENT, selection, selectionArgs)
        }

        /**
         * 查询所有事件
         */
        fun queryAllEvent(
            context: Context,
            dataCacheValidTime: Long
        ): List<UploadEventData> {
            val list = mutableListOf<UploadEventData>()
            val projection = arrayOf("uploadId", "data", "createTime")
            val sortOrder = "createTime DESC"

            val cursor = getInstance(context).queryData(
                TABLE_INSIGHT_INTO_EVENT,
                projection,
                "type = ?",
                arrayOf("1"),
                sortOrder
            )

            cursor?.apply {
                val currentTime = System.currentTimeMillis()
                while (moveToNext()) {
                    val uploadId = getStringData("uploadId")
                    val data = getStringData("data")
                    val createTime = getLongData("createTime") ?: 0L
                    try {
                        if (createTime == 0L || currentTime - createTime > dataCacheValidTime) {
                            //过期缓存数据直接删除掉
                            deleteEventRequest(context, uploadId)
                            continue
                        }

                        if (!TextUtils.isEmpty(uploadId)) {
                            val jsonData = AesUtil.decryptStr(data, CommonUtil.akx)
                            val uploadEventData = GsonManager.gson.fromJson(
                                jsonData,
                                UploadEventData::class.java
                            )
                            list.add(uploadEventData)
                        }
                    } catch (t: Throwable) {
                        ILog.e("query event exception!", t)
                        if (!TextUtils.isEmpty(uploadId)) {
                            deleteEventRequest(context, uploadId)
                        }
                    }
                }
            }

            return list
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_INSIGHT_INTO_EVENT_DATA_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        ILog.d("oldVersion:$oldVersion,newVersion:$newVersion")
    }

    /**
     * 插入数据
     */
    fun insertData(tableName: String, contentValues: ContentValues) {
        writableDatabase.insert(tableName, null, contentValues)
    }

    /**
     * 查询数据
     */
    fun queryData(
        tableName: String, projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = readableDatabase.query(
        tableName,
        projection,
        selection,
        selectionArgs,
        null,
        null,
        sortOrder
    )

    /**
     * 更新数据
     */
    fun updateData(
        tableName: String,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ) = writableDatabase.update(tableName, values, selection, selectionArgs)

    /**
     * 删除数据
     */
    fun deleteData(
        tableName: String,
        selection: String?,
        selectionArgs: Array<String>?,
    ) =
        writableDatabase.delete(tableName, selection, selectionArgs)

}