package com.github.wangyiqian.stockchart.childchart.base

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.github.wangyiqian.stockchart.IStockChart
import kotlin.math.abs
import kotlin.math.round

/**
 * 管理子图的matrix
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/2/3
 */
class ChildChartMatrixHelper<O : BaseChildChartConfig>(
    private val stockChart: IStockChart,
    private val chart: BaseChildChart<O>
) {

    // 用于逻辑坐标转实际坐标，这个matrix转换的结果是所有数据填满显示区域，而不关注需要显示哪些指定范围的数据
    val coordinateMatrix = Matrix()

    // 如果需要"一格一格"滑动，则x轴可能由于缩放引起"半个"数据显示在边缘，需要调整
    private val fixXMatrix = Matrix()

    // 调整Y，使得最终的内容填满显示区域
    private val fixYMatrix = Matrix()

    // 多个matrix组合后
    private val concatMatrix = Matrix()

    // 临时载体
    private val tmp2FloatArray = FloatArray(2)
    private val tmp4FloatArray = FloatArray(4)
    private val tmpMatrix = Matrix()

    /**
     * 初始准备
     */
    fun prepare() {
        prepareCoordinateMatrix()
    }

    /**
     * 每次绘制的时候，需要设置相关matrix
     */
    fun setOnDraw() {
        setFixXMatrix()
        setFixYMatrix()
        setConcatMatrix()
    }

    private fun prepareCoordinateMatrix() {
        coordinateMatrix.reset()

        val chartDisplayArea = chart.getChartMainDisplayArea()

        chart.getXValueRange(
            stockChart.getConfig().showStartIndex,
            stockChart.getConfig().showEndIndex,
            tmp2FloatArray
        )
        val xValueRangeFrom = tmp2FloatArray[0]
        val xValueRangeEnd = tmp2FloatArray[1]
        val xValueRangeLen = xValueRangeEnd - xValueRangeFrom

        chart.getYValueRange(
            stockChart.getConfig().showStartIndex,
            stockChart.getConfig().showEndIndex,
            tmp2FloatArray
        )
        val yValueRangeFrom = tmp2FloatArray[0]
        val yValueRangeEnd = tmp2FloatArray[1]
        var yValueRangeLen = yValueRangeEnd - yValueRangeFrom

        if (yValueRangeLen == 0f) {
            // 非正常情况，y轴逻辑区间无法算出（所有值相等），之前处于原始逻辑坐标，将需要显示的逻辑区域移动到显示区域左边垂直居中位置
            coordinateMatrix.postTranslate(
                chartDisplayArea.left - xValueRangeFrom,
                (chartDisplayArea.bottom - chartDisplayArea.top) / 2 - yValueRangeFrom
            )
        } else {
            // 正常情况，之前处于原始逻辑坐标，将需要显示的逻辑区域移动到显示区域左上角
            coordinateMatrix.postTranslate(
                chartDisplayArea.left - xValueRangeFrom,
                chartDisplayArea.top - yValueRangeFrom
            )
        }

        val sx = (chartDisplayArea.right - chartDisplayArea.left) / xValueRangeLen
        val sy = if (yValueRangeLen == 0f) {
            // 非正常情况，y轴逻辑区间无法算出（所有值相等），直接保持原状不缩放
            1f
        } else {
            // 正常情况，y按照区间比缩放即可
            (chartDisplayArea.bottom - chartDisplayArea.top) / yValueRangeLen
        }

        // 缩放使得需要显示的内容刚好撑满显示区域，再向上翻转，使得y内容翻转在显示区域上方
        coordinateMatrix.postScale(
            sx,
            -sy,
            chartDisplayArea.left,
            chartDisplayArea.top
        )

        // 正常情况，下移一个显示区域
        coordinateMatrix.postTranslate(0f, chartDisplayArea.bottom - chartDisplayArea.top)
    }

    /**
     * 计算[fixXMatrix]
     */
    private fun setFixXMatrix() {
        fixXMatrix.reset()

        if (!stockChart.getConfig().scrollSmoothly) {

            // "一格一格"地滑

            val chartDisplayArea = chart.getChartMainDisplayArea()

            // 反算出会被移动到显示区域的第一个逻辑坐标值（数据下标）
            tmpMatrix.reset()
            tmpMatrix.postConcat(coordinateMatrix)
            tmpMatrix.postConcat(stockChart.getXScaleMatrix())
            tmpMatrix.postConcat(stockChart.getFixXScaleMatrix())
            tmpMatrix.postConcat(stockChart.getScrollMatrix())
            tmpMatrix.invert(tmpMatrix)
            tmp2FloatArray[0] = chartDisplayArea.left
            tmp2FloatArray[1] = 0f
            tmpMatrix.mapPoints(tmp2FloatArray)
            var indexFrom = (round(tmp2FloatArray[0])).toInt()
            if (indexFrom !in 0 until stockChart.getConfig().getKEntitiesSize()) {
                indexFrom = 0
            }

            // 计算出"一格"长度
            tmp4FloatArray[0] = indexFrom.toFloat()
            tmp4FloatArray[1] = 0f
            tmp4FloatArray[2] = (indexFrom + 1).toFloat()
            tmp4FloatArray[3] = 0f
            tmpMatrix.reset()
            tmpMatrix.postConcat(coordinateMatrix)
            tmpMatrix.postConcat(stockChart.getXScaleMatrix())
            tmpMatrix.postConcat(stockChart.getFixXScaleMatrix())
            tmpMatrix.postConcat(stockChart.getScrollMatrix())
            tmpMatrix.mapPoints(tmp4FloatArray)
            val first = tmp4FloatArray[0]
            val second = tmp4FloatArray[2]
            val lengthOfOneIndex = second - first

            if (lengthOfOneIndex != 0f) {
                val unalignedDis = (first - chartDisplayArea.left) % lengthOfOneIndex

                val dx = when {
                    // 右移
                    unalignedDis < 0 && abs(unalignedDis) < lengthOfOneIndex / 2 -> {
                        abs(unalignedDis)
                    }
                    // 左移
                    unalignedDis < 0 && abs(unalignedDis) > lengthOfOneIndex / 2 -> {
                        -abs(lengthOfOneIndex - unalignedDis)
                    }
                    // 左移
                    unalignedDis > 0 && abs(unalignedDis) < lengthOfOneIndex / 2 -> {
                        -abs(unalignedDis)
                    }

                    // 右移
                    unalignedDis > 0 && abs(unalignedDis) > lengthOfOneIndex / 2 -> abs(
                        lengthOfOneIndex - unalignedDis
                    )
                    // 不移动
                    else -> 0f
                }

                fixXMatrix.postTranslate(dx, 0f)
            }
        }
    }

    /**
     * 计算[fixYMatrix]，使y轴内容刚好填满显示区域
     */
    private fun setFixYMatrix() {
        fixYMatrix.reset()

        val chartDisplayArea = chart.getChartMainDisplayArea()

        // 反算出哪个下标（逻辑坐标）范围会被移动到显示区域
        tmpMatrix.reset()
        tmpMatrix.postConcat(coordinateMatrix)
        tmpMatrix.postConcat(stockChart.getXScaleMatrix())
        tmpMatrix.postConcat(stockChart.getFixXScaleMatrix())
        tmpMatrix.postConcat(stockChart.getScrollMatrix())
        tmpMatrix.postConcat(fixXMatrix)
        tmpMatrix.invert(tmpMatrix)
        tmp4FloatArray[0] = chartDisplayArea.left
        tmp4FloatArray[1] = 0f
        tmp4FloatArray[2] = chartDisplayArea.right
        tmp4FloatArray[3] = 0f
        tmpMatrix.mapPoints(tmp4FloatArray)
        var indexFrom = (round(tmp4FloatArray[0])).toInt()
        if (indexFrom !in 0 until stockChart.getConfig().getKEntitiesSize()) {
            indexFrom = 0
        }
        var indexEnd = (round(tmp4FloatArray[2])).toInt()
        if (indexEnd !in 0 until stockChart.getConfig().getKEntitiesSize()) {
            indexEnd = stockChart.getConfig().getKEntitiesSize() - 1
        }

        // 算出移到实际显示区域后保持原缩放比例时的y实际坐标范围
        chart.getYValueRange(indexFrom, indexEnd, tmp2FloatArray)
        val yValueRangeFrom = tmp2FloatArray[0]
        val yValueRangeEnd = tmp2FloatArray[1]
        tmpMatrix.reset()
        tmpMatrix.postConcat(coordinateMatrix)
        tmpMatrix.postConcat(stockChart.getXScaleMatrix())
        concatMatrix.postConcat(stockChart.getFixXScaleMatrix())
        tmpMatrix.postConcat(stockChart.getScrollMatrix())
        tmpMatrix.postConcat(fixXMatrix)
        tmp4FloatArray[0] = 0f
        tmp4FloatArray[1] = yValueRangeFrom
        tmp4FloatArray[2] = 0f
        tmp4FloatArray[3] = yValueRangeEnd
        tmpMatrix.mapPoints(tmp4FloatArray)
        val yMin: Float
        val yMax: Float
        if (tmp4FloatArray[3] > tmp4FloatArray[1]) {
            yMin = tmp4FloatArray[1]
            yMax = tmp4FloatArray[3]
        } else {
            yMin = tmp4FloatArray[3]
            yMax = tmp4FloatArray[1]
        }

        if (yMin != yMax) {
            // 先贴顶
            fixYMatrix.postTranslate(0f, chartDisplayArea.top - yMin)
            val sy = (chartDisplayArea.bottom - chartDisplayArea.top) / (yMax - yMin)
            // 再缩放
            fixYMatrix.postScale(1f, sy, 0f, chartDisplayArea.top)
        }
    }

    private fun setConcatMatrix() {
        concatMatrix.reset()
        concatMatrix.postConcat(coordinateMatrix)
        concatMatrix.postConcat(stockChart.getXScaleMatrix())
        concatMatrix.postConcat(stockChart.getFixXScaleMatrix())
        concatMatrix.postConcat(stockChart.getScrollMatrix())
        concatMatrix.postConcat(fixXMatrix)
        concatMatrix.postConcat(fixYMatrix)
    }

    fun mapPointsValue2Real(pts: FloatArray) {
        setConcatMatrix()
        concatMatrix.mapPoints(pts)
    }

    fun mapRectValue2Real(rect: RectF) {
        setConcatMatrix()
        concatMatrix.mapRect(rect)
    }

    fun mapPathValue2Real(path: Path) {
        setConcatMatrix()
        path.transform(concatMatrix)
    }

    fun mapPointsReal2Value(pts: FloatArray) {
        setConcatMatrix()
        concatMatrix.invert(tmpMatrix)
        tmpMatrix.mapPoints(pts)
    }

    fun mapRectReal2Value(rect: RectF) {
        setConcatMatrix()
        concatMatrix.invert(tmpMatrix)
        tmpMatrix.mapRect(rect)
    }

    fun mapPathReal2Value(path: Path) {
        setConcatMatrix()
        concatMatrix.invert(tmpMatrix)
        path.transform(tmpMatrix)
    }

}