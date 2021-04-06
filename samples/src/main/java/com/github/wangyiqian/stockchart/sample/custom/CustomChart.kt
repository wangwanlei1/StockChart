package com.github.wangyiqian.stockchart.sample.custom

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.wangyiqian.stockchart.IStockChart
import com.github.wangyiqian.stockchart.childchart.base.BaseChildChart
import com.github.wangyiqian.stockchart.entities.EmptyKEntity
import com.github.wangyiqian.stockchart.entities.IKEntity
import com.github.wangyiqian.stockchart.util.DimensionUtil

/**
 * 自定义图示例：每天的平均价连线
 *
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/2/9
 */
class CustomChart(
    stockChart: IStockChart,
    chartConfig: CustomChartConfig
) : BaseChildChart<CustomChartConfig>(stockChart, chartConfig) {

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
    }

    private val pointHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
    }

    private val bigLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = DimensionUtil.sp2px(context, 30f).toFloat()
    }

    override fun onKEntitiesChanged() {}

    override fun getYValueRange(startIndex: Int, endIndex: Int, result: FloatArray) {
        var yMax = 0f
        var yMin = 0f

        getKEntities().filterIndexed { index, kEntity -> index in startIndex..endIndex && kEntity !is EmptyKEntity }
            .map { getAvgPrice(it) }.apply {
                if (size > 0) {
                    yMax = max()!!
                    yMin = min()!!
                }
            }

        result[0] = yMin
        result[1] = yMax
    }

    private fun getAvgPrice(kEntity: IKEntity) =
        (kEntity.getHighPrice() + kEntity.getLowPrice()) / 2

    override fun preDrawBackground(canvas: Canvas) {
    }

    override fun drawBackground(canvas: Canvas) {
        if (!chartConfig.bigLabel.isNullOrEmpty()) {
            bigLabelPaint.getFontMetrics(tmpFontMetrics)
            val x =
                (getChartDisplayArea().left + getChartDisplayArea().right) / 2 - bigLabelPaint.measureText(
                    chartConfig.bigLabel
                ) / 2
            val y =
                getChartDisplayArea().top + getChartDisplayArea().height() / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom
            canvas.drawText(chartConfig.bigLabel!!, x, y, bigLabelPaint)
        }
    }

    override fun preDrawData(canvas: Canvas) {
    }

    override fun drawData(canvas: Canvas) {
        getKEntities().forEachIndexed { index, kEntity ->
            if (kEntity !is EmptyKEntity) {
                tmp2FloatArray[0] = index + 0.5f
                tmp2FloatArray[1] = getAvgPrice(kEntity)
                mapPointsValue2Real(tmp2FloatArray)
                canvas.drawCircle(tmp2FloatArray[0], tmp2FloatArray[1], 10f, pointPaint)
            }
        }
    }

    override fun preDrawHighlight(canvas: Canvas) {
    }

    override fun drawHighlight(canvas: Canvas) {
        getHighlight()?.let { highlight ->
            if (highlight.x >= getChartDisplayArea().left && highlight.x <= getChartDisplayArea().right) {

                val idx = highlight.getIdx()

                if (idx >= 0 && idx <= getKEntities().size - 1 && getKEntities()[idx] !is EmptyKEntity) {
                    val yValue = getAvgPrice(getKEntities()[idx])

                    // 计算出要绘制的坐标
                    tmp2FloatArray[0] = idx + 0.5f
                    tmp2FloatArray[1] = yValue
                    mapPointsValue2Real(tmp2FloatArray)

                    // 绘制高亮点
                    canvas.drawCircle(
                        tmp2FloatArray[0],
                        tmp2FloatArray[1],
                        20f,
                        pointHighlightPaint
                    )
                }
            }
        }
    }

    override fun drawAddition(canvas: Canvas) {
    }
}