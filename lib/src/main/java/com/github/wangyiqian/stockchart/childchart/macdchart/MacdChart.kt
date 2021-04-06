package com.github.wangyiqian.stockchart.childchart.macdchart

import android.graphics.Canvas
import android.graphics.Paint
import com.github.wangyiqian.stockchart.IStockChart
import com.github.wangyiqian.stockchart.childchart.base.BaseChildChart
import kotlin.math.round

/**
 * MACD指标图
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/2/18
 */
class MacdChart(
    stockChart: IStockChart,
    chartConfig: MacdChartConfig
) : BaseChildChart<MacdChartConfig>(stockChart, chartConfig) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightHorizontalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightVerticalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightLabelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indexTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var indexList: List<List<Float?>>? = null

    private var drawnIndexTextHeight = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        indexList = chartConfig.index?.calculate(getKEntities())
    }

    override fun onKEntitiesChanged() {
        indexList = chartConfig.index?.calculate(getKEntities())
    }

    override fun getYValueRange(startIndex: Int, endIndex: Int, result: FloatArray) {
        var yMax = 0f
        var yMin = 0f
        indexList?.forEach { valueList ->
            valueList.filterIndexed { idx, _ -> idx in startIndex..endIndex }.filterNotNull()
                .apply {
                    if (size > 0) {
                        yMax = kotlin.math.max(yMax, max()!!)
                        yMin = kotlin.math.min(yMin, min()!!)
                    }
                }
        }
        result[0] = yMin
        result[1] = yMax
    }

    override fun preDrawBackground(canvas: Canvas) {
    }

    override fun drawBackground(canvas: Canvas) {
    }

    override fun preDrawData(canvas: Canvas) {
    }

    override fun drawData(canvas: Canvas) {
        val difIdx = 0
        val deaIdx = 1
        val macdIdx = 2

        // draw macd bar
        val barWidth = 1 * (1 - chartConfig.barSpaceRatio)
        val spaceWidth = 1 * chartConfig.barSpaceRatio
        var barLeft = spaceWidth / 2
        indexList?.get(macdIdx).let { valueList ->
            valueList?.forEach { value ->
                value?.let {
                    barPaint.color =
                        if (it >= 0f) stockChart.getConfig().riseColor else stockChart.getConfig().downColor

                    tmpRectF.left = barLeft
                    tmpRectF.top = it
                    tmpRectF.right = barLeft + barWidth
                    tmpRectF.bottom = 0f

                    mapRectValue2Real(tmpRectF)

                    canvas.drawRect(tmpRectF, barPaint)
                }

                barLeft += barWidth + spaceWidth
            }
        }

        fun drawLine(valueList: List<Float?>?) {
            valueList?.forEachIndexed { valueIdx, value ->
                if (valueIdx == 0) {
                    return@forEachIndexed
                }
                value?.let { value ->
                    valueList[valueIdx - 1]?.let { preValue ->
                        tmp4FloatArray[0] = valueIdx - 1 + 0.5f
                        tmp4FloatArray[1] = preValue
                        tmp4FloatArray[2] = valueIdx + 0.5f
                        tmp4FloatArray[3] = value

                        mapPointsValue2Real(tmp4FloatArray)

                        canvas.drawLines(tmp4FloatArray, linePaint)
                    }
                }
            }
        }

        // draw dif line
        linePaint.strokeWidth = chartConfig.difLineStrokeWidth
        linePaint.color = chartConfig.difLineColor
        drawLine(indexList?.get(difIdx))

        // draw dea line
        linePaint.strokeWidth = chartConfig.deaLineStrokeWidth
        linePaint.color = chartConfig.deaLineColor
        drawLine(indexList?.get(deaIdx))

        // draw index text
        drawnIndexTextHeight = 0f
        chartConfig.index?.let { index ->
            indexList?.let { indexList ->
                val highlight = getHighlight()
                var indexIdx = highlight?.getIdx() ?: stockChart.findLastNotEmptyKEntityIdxInDisplayArea()
                indexTextPaint.textSize = index.textSize
                var left = index.textMarginLeft
                val top = index.textMarginTop
                indexTextPaint.getFontMetrics(tmpFontMetrics)
                fun drawIndexText(text: String) {
                    canvas.drawText(
                        text,
                        left,
                        -tmpFontMetrics.top + top,
                        indexTextPaint
                    )
                    left += indexTextPaint.measureText(text) + index.textSpace
                    drawnIndexTextHeight =
                        tmpFontMetrics.bottom - tmpFontMetrics.top
                }
                if (!index.startText.isNullOrEmpty()) {
                    indexTextPaint.color = index.startTextColor
                    drawIndexText(index.startText)
                }
                indexList.forEachIndexed { lineIdx, pointList ->
                    indexTextPaint.color = when (lineIdx) {
                        difIdx -> chartConfig.difLineColor
                        deaIdx -> chartConfig.deaLineColor
                        else -> chartConfig.macdTextColor
                    }
                    val value =
                        if (indexIdx != null && indexIdx in pointList.indices && pointList[indexIdx] != null) pointList[indexIdx] else null
                    val text = index.textFormatter.invoke(lineIdx, value)
                    drawIndexText(text)
                }
            }
        }
    }

    override fun preDrawHighlight(canvas: Canvas) {
    }

    override fun drawHighlight(canvas: Canvas) {
        getHighlight()?.let { highlight ->
            val highlightAreaTop = getChartDisplayArea().top + drawnIndexTextHeight
            if (stockChart.getConfig().showHighlightHorizontalLine) {
                if (highlight.y >= highlightAreaTop && highlight.y <= getChartDisplayArea().bottom) {

                    highlightHorizontalLinePaint.color =
                        stockChart.getConfig().highlightHorizontalLineColor
                    highlightHorizontalLinePaint.strokeWidth =
                        stockChart.getConfig().highlightHorizontalLineWidth

                    var highlightHorizontalLineLeft = getChartDisplayArea().left
                    var highlightHorizontalLineRight = getChartDisplayArea().right

                    // left highlight label
                    chartConfig.highlightLabelLeft?.let { highlightLabel ->
                        highlightLabelPaint.textSize = highlightLabel.textSize
                        highlightLabelPaint.color = highlightLabel.textColor
                        highlightLabelBgPaint.color = highlightLabel.bgColor
                        val text = highlightLabel.textFormat(highlight.valueY)
                        highlightLabelPaint.getTextBounds(text, 0, text.length, tmpRect)
                        val textWidth = tmpRect.width()
                        val textHeight = tmpRect.height()
                        val bgWidth = textWidth + highlightLabel.padding * 2
                        val bgHeight = textHeight + highlightLabel.padding * 2
                        tmpRectF.left = getChartDisplayArea().left
                        tmpRectF.top = highlight.y - bgHeight / 2
                        tmpRectF.right = bgWidth
                        tmpRectF.bottom = highlight.y + bgHeight / 2
                        if (tmpRectF.top < highlightAreaTop) {
                            tmpRectF.offset(0f, highlightAreaTop - tmpRectF.top)
                        } else if (tmpRectF.bottom > getChartDisplayArea().bottom) {
                            tmpRectF.offset(0f, getChartDisplayArea().bottom - tmpRectF.bottom)
                        }
                        highlightLabelPaint.getFontMetrics(tmpFontMetrics)
                        val textBaseLine =
                            tmpRectF.top + bgHeight / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom

                        canvas.drawRoundRect(
                            tmpRectF,
                            highlightLabel.bgCorner,
                            highlightLabel.bgCorner,
                            highlightLabelBgPaint
                        )

                        canvas.drawText(
                            text,
                            tmpRectF.left + highlightLabel.padding,
                            textBaseLine,
                            highlightLabelPaint
                        )

                        highlightHorizontalLineLeft += bgWidth
                    }

                    // right highlight label
                    chartConfig.highlightLabelRight?.let { highlightLabel ->
                        highlightLabelPaint.textSize = highlightLabel.textSize
                        highlightLabelPaint.color = highlightLabel.textColor
                        highlightLabelBgPaint.color = highlightLabel.bgColor
                        val text = highlightLabel.textFormat(highlight.valueY)
                        highlightLabelPaint.getTextBounds(text, 0, text.length, tmpRect)
                        val textWidth = tmpRect.width()
                        val textHeight = tmpRect.height()
                        val bgWidth = textWidth + highlightLabel.padding * 2
                        val bgHeight = textHeight + highlightLabel.padding * 2
                        tmpRectF.left = getChartDisplayArea().right - bgWidth
                        tmpRectF.top = highlight.y - bgHeight / 2
                        tmpRectF.right = getChartDisplayArea().right
                        tmpRectF.bottom = highlight.y + bgHeight / 2
                        if (tmpRectF.top < highlightAreaTop) {
                            tmpRectF.offset(0f, highlightAreaTop - tmpRectF.top)
                        } else if (tmpRectF.bottom > getChartDisplayArea().bottom) {
                            tmpRectF.offset(0f, getChartDisplayArea().bottom - tmpRectF.bottom)
                        }
                        highlightLabelPaint.getFontMetrics(tmpFontMetrics)
                        val textBaseLine =
                            tmpRectF.top + bgHeight / 2 + (tmpFontMetrics.bottom - tmpFontMetrics.top) / 2 - tmpFontMetrics.bottom

                        canvas.drawRoundRect(
                            tmpRectF,
                            highlightLabel.bgCorner,
                            highlightLabel.bgCorner,
                            highlightLabelBgPaint
                        )

                        canvas.drawText(
                            text,
                            tmpRectF.left + highlightLabel.padding,
                            textBaseLine,
                            highlightLabelPaint
                        )

                        highlightHorizontalLineRight -= bgWidth
                    }

                    // highlight horizontal line
                    canvas.drawLine(
                        highlightHorizontalLineLeft,
                        highlight.y,
                        highlightHorizontalLineRight,
                        highlight.y,
                        highlightHorizontalLinePaint
                    )
                }
            }

            if (stockChart.getConfig().showHighlightVerticalLine) {
                if (highlight.x >= getChartDisplayArea().left && highlight.x <= getChartDisplayArea().right) {

                    highlightVerticalLinePaint.color =
                        stockChart.getConfig().highlightVerticalLineColor
                    highlightVerticalLinePaint.strokeWidth =
                        stockChart.getConfig().highlightVerticalLineWidth

                    tmp2FloatArray[0] = highlight.getIdx() + 0.5f
                    tmp2FloatArray[1] = 0f
                    mapPointsValue2Real(tmp2FloatArray)
                    val x = tmp2FloatArray[0]

                    // highlight vertical line
                    canvas.drawLine(
                        x,
                        highlightAreaTop,
                        x,
                        getChartDisplayArea().bottom,
                        highlightVerticalLinePaint
                    )
                }
            }
        }
    }

    override fun drawAddition(canvas: Canvas) {
    }
}