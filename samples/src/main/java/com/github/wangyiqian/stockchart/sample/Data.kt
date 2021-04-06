package com.github.wangyiqian.stockchart.sample

import android.content.Context
import android.util.Log
import com.github.wangyiqian.stockchart.entities.EmptyKEntity
import com.github.wangyiqian.stockchart.entities.IKEntity
import com.github.wangyiqian.stockchart.entities.KEntity
import com.github.wangyiqian.stockchart.entities.KEntityOfLineStarter
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 模拟数据
 *
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/1/29
 */
object Data {

    private const val MOCK_DELAY = 0L // 模拟耗时

    fun loadDayData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {

        if (page > 2 || page < 0) {
            callback.invoke(listOf())
        }

        loadData(context, "mock_data_day_page_$page.txt", callback)
    }

    fun loadFiveDayData(context: Context, callback: (List<IKEntity>) -> Unit) {
        MainScope().launch {
            val deferred = async {
                delay(MOCK_DELAY)
                loadDataFromTimeDataAsserts(context, "mock_time_data_five_day.txt")
            }
            val result = deferred.await()
            val dateFormat = SimpleDateFormat("MM/dd")
            val date = Date()
            var dateStr = ""
            result.forEachIndexed { idx, kEntity ->
                date.time = kEntity.getTime()
                val formatDate = dateFormat.format(date)

                if (formatDate != dateStr) {
                    dateStr = formatDate
                    result[idx] = KEntityOfLineStarter(kEntity)
                }
            }
            callback.invoke(result)
        }
    }

    fun loadWeekData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 1 || page < 0) {
            callback.invoke(listOf())
        }

        loadData(context, "mock_data_week_page_$page.txt", callback)
    }

    fun loadMonthData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 0 || page < 0) {
            callback.invoke(listOf())
        }

        loadData(context, "mock_data_month_page_$page.txt", callback)
    }

    fun loadQuarterData(context: Context, callback: (List<IKEntity>) -> Unit) {
        loadData(context, "mock_data_quarter.txt", callback)
    }

    fun loadYearData(context: Context, callback: (List<IKEntity>) -> Unit) {
        loadData(context, "mock_data_year.txt", callback)
    }

    fun loadFiveYearData(context: Context, callback: (List<IKEntity>) -> Unit) {
        loadData(context, "mock_data_five_year.txt", callback)
    }

    fun loadYTDData(context: Context, callback: (List<IKEntity>) -> Unit) {
        MainScope().launch {
            val deferred = async {
                delay(MOCK_DELAY)
                loadDataFromAsserts(context, "mock_data_ytd.txt")
            }
            val result = deferred.await()
            if (result.isNotEmpty()) {
                val time = result[0].getTime()
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = time
                val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                val maxDaysOfYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                val weekCount = (maxDaysOfYear - dayOfYear) / 7 + 1
                for (i in 0 until (weekCount - result.size)) {
                    result.add(EmptyKEntity()) // 一年内还未产生的数据用EmptyKEntity()填充
                }
            }
            callback.invoke(result)
        }
    }

    fun loadOneMinuteData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 4 || page < 0) {
            callback.invoke(listOf())
        }

        loadData(context, "mock_data_one_minute_page_$page.txt", callback)
    }

    fun loadFiveMinutesData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 3 || page < 0) {
            callback.invoke(listOf())
        }

        loadData(context, "mock_data_five_minutes_page_$page.txt", callback)
    }

    fun loadSixtyMinutesData(context: Context, page: Int, callback: (List<IKEntity>) -> Unit) {
        if (page > 3 || page < 0) {
            callback.invoke(listOf())
        }

        loadData(context, "mock_data_sixty_minutes_page_$page.txt", callback)
    }

    private fun loadData(
        context: Context,
        assertsFileName: String,
        callback: (List<IKEntity>) -> Unit
    ) {
        MainScope().launch {
            val deferred = async {
                delay(MOCK_DELAY)
                loadDataFromAsserts(context, assertsFileName)
            }
            callback.invoke(deferred.await())
        }
    }

    private fun loadDataFromAsserts(context: Context, fileName: String): MutableList<IKEntity> {
        var inputStream: InputStream? = null
        val result = mutableListOf<IKEntity>()
        try {
            inputStream = context.resources.assets.open(fileName)
            var buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            val jsonStr = String(buffer)
            var data = JSONArray(jsonStr)
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val kEntity = KEntity(
                    item.getString("high").toFloat(),
                    item.getString("low").toFloat(),
                    item.getString("open").toFloat(),
                    item.getString("close").toFloat(),
                    item.getLong("volume"),
                    item.getLong("time")
                )
                result.add(kEntity)
            }
        } catch (tr: Throwable) {
            Log.e("", "", tr)
        } finally {
            inputStream?.close()
        }
        return result
    }

    private fun loadDataFromTimeDataAsserts(
        context: Context,
        fileName: String
    ): MutableList<IKEntity> {
        var inputStream: InputStream? = null
        val result = mutableListOf<IKEntity>()
        try {
            inputStream = context.resources.assets.open(fileName)
            var buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            val jsonStr = String(buffer)
            var data = JSONArray(jsonStr)
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val kEntity = KEntity(
                    item.getString("price").toFloat(),
                    item.getString("price").toFloat(),
                    item.getString("price").toFloat(),
                    item.getString("price").toFloat(),
                    item.getLong("volume"),
                    item.getLong("time")
                )
                result.add(kEntity)
            }
        } catch (tr: Throwable) {
            Log.e("", "", tr)
        } finally {
            inputStream?.close()
        }
        return result
    }


}