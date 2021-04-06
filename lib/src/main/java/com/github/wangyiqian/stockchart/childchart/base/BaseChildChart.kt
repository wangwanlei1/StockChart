package com.github.wangyiqian.stockchart.childchart.base

import android.graphics.*
import android.view.View
import android.view.ViewGroup
import com.github.wangyiqian.stockchart.IStockChart
import com.github.wangyiqian.stockchart.StockChart
import com.github.wangyiqian.stockchart.entities.EmptyKEntity
import com.github.wangyiqian.stockchart.listener.OnKEntitiesChangedListener

/**
 * 所有的StockChart的子View都需要继承此类
 *
 * @author wangyiqian E-mail: wangyiqian9891@gmail.com
 * @version 创建时间: 2021/1/28
 */
abstract class BaseChildChart<C : BaseChildChartConfig> @JvmOverloads constructor(
    val stockChart: IStockChart,
    val chartConfig: C
) : View(stockChart.getContext()), IChildChart,
    OnKEntitiesChangedListener {

    // 管理matrix
    private var childChartMatrixHelper: ChildChartMatrixHelper<C>? = null

    // 显示区域
    private val chartDisplayArea = RectF()

    // 主显示区域
    private val chartMainDisplayArea = RectF()

    // 临时载体
    protected val tmp2FloatArray = FloatArray(2)
    protected val tmp4FloatArray = FloatArray(4)
    protected val tmp12FloatArray = FloatArray(12)
    protected val tmp24FloatArray = FloatArray(24)
    protected val tmpRectF = RectF()
    protected val tmpRect = Rect()
    protected val tmpPath = Path()
    protected val tmpFontMetrics = Paint.FontMetrics()

    init {
        val layoutParams =
            StockChart.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                chartConfig.height
            )
        layoutParams.setMargins(
            0,
            chartConfig.marginTop,
            0,
            chartConfig.marginBottom
        )
        this.layoutParams = layoutParams

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        stockChart.addOnKEntitiesChangedListener(this)
        childChartMatrixHelper =
            ChildChartMatrixHelper(
                stockChart,
                this
            )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stockChart.removeOnKEntitiesChangedListener(this)
    }

    override fun getHighlight() = stockChart.getHighlight(this)

    override fun getKEntities() = stockChart.getConfig().kEntities

    override fun view() = this

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setDisplayArea()
        prepare()
    }

    private fun setDisplayArea() {

        // x轴显示区域固定死 子类不可各自实现
        chartDisplayArea.left = 0f
        chartDisplayArea.right = width.toFloat()

        chartDisplayArea.top = getDisplayAreaYRangeMin()
        chartDisplayArea.bottom = getDisplayAreaYRangeMax()

        chartMainDisplayArea.left = chartDisplayArea.left
        chartMainDisplayArea.right = chartDisplayArea.right
        chartMainDisplayArea.top =
            chartDisplayArea.top + chartConfig.chartMainDisplayAreaPaddingTop
        chartMainDisplayArea.bottom =
            chartDisplayArea.bottom - chartConfig.chartMainDisplayAreaPaddingBottom
    }

    /**
     * y轴显示区域最小值
     * 子类若有特殊需求可覆盖实现
     */
    open fun getDisplayAreaYRangeMin() = 0f

    /**
     * y轴显示区域最大值
     * 子类若有特殊需求可覆盖实现
     */
    open fun getDisplayAreaYRangeMax() = height.toFloat()

    override fun onSetKEntities() {
        onKEntitiesChanged()
        prepare()
    }

    override fun onAppendKEntities() {
        onKEntitiesChanged()
        prepare()
    }

    private fun prepare() {
        if (stockChart.getConfig().getKEntitiesSize() <= 0) return
        childChartMatrixHelper?.prepare()
    }

    /**
     * 模板方法：k线数据发送变化
     */
    abstract fun onKEntitiesChanged()

    /**
     * 获得指定下标范围内[startIndex ~ endIndex]，x轴逻辑坐标的范围值
     * 注意，应包含最后一个数据的长度
     * 统一规则，子类不可定制，
     */
    fun getXValueRange(startIndex: Int, endIndex: Int, result: FloatArray) {
        result[0] = startIndex.toFloat()
        result[1] = endIndex.toFloat() + getXValueUnitLen()
    }

    /**
     * x轴逻辑坐标单位长度，统一固定规则
     */
    private fun getXValueUnitLen() = 1f

    /**
     * 模板方法：获得指定下标范围内[startIndex ~ endIndex]，y轴逻辑坐标的范围值
     */
    abstract fun getYValueRange(startIndex: Int, endIndex: Int, result: FloatArray)

    override fun onDraw(canvas: Canvas) {
        if (stockChart.getConfig().getKEntitiesSize() <= 0) return
        childChartMatrixHelper?.setOnDraw()
        preDrawBackground(canvas)
        drawBackground(canvas)
        preDrawData(canvas)
        drawData(canvas)
        preDrawHighlight(canvas)
        drawHighlight(canvas)
        drawAddition(canvas)
    }

    /**
     * 模板方法：在绘制背景之前绘制
     */
    abstract fun preDrawBackground(canvas: Canvas)

    /**
     * 模板方法：绘制背景
     */
    abstract fun drawBackground(canvas: Canvas)

    /**
     * 模板方法：在绘制主数据之前绘制
     */
    abstract fun preDrawData(canvas: Canvas)

    /**
     * 模板方法：绘制主数据
     */
    abstract fun drawData(canvas: Canvas)

    /**
     * 模板方法：在绘制长按高亮之前绘制
     */
    abstract fun preDrawHighlight(canvas: Canvas)

    /**
     * 模板方法：绘制长按高亮
     */
    abstract fun drawHighlight(canvas: Canvas)

    /**
     * 模板方法：绘制其他内容
     */
    abstract fun drawAddition(canvas: Canvas)

    override fun getCoordinateMatrix() = childChartMatrixHelper!!.coordinateMatrix

    override fun mapPointsValue2Real(pts: FloatArray) {
        childChartMatrixHelper?.mapPointsValue2Real(pts)
    }

    override fun mapRectValue2Real(rect: RectF) {
        childChartMatrixHelper?.mapRectValue2Real(rect)
    }

    override fun mapPathValue2Real(path: Path) {
        childChartMatrixHelper?.mapPathValue2Real(path)
    }

    override fun mapPointsReal2Value(pts: FloatArray) {
        childChartMatrixHelper?.mapPointsReal2Value(pts)
    }

    override fun mapRectReal2Value(rect: RectF) {
        childChartMatrixHelper?.mapRectReal2Value(rect)
    }

    override fun mapPathReal2Value(path: Path) {
        childChartMatrixHelper?.mapPathReal2Value(path)
    }

    override fun getChartDisplayArea() = chartDisplayArea

    override fun getChartMainDisplayArea() = chartMainDisplayArea

    override fun getConfig() = chartConfig

}