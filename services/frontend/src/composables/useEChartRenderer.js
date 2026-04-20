import { onBeforeUnmount, onMounted, ref } from "vue";
import * as echarts from "echarts";

function buildMovingAverage(closes, period) {
  const ma = [];
  let rollingSum = 0;

  for (let i = 0; i < closes.length; i += 1) {
    const close = Number(closes[i]);
    rollingSum += Number.isFinite(close) ? close : 0;

    if (i >= period) {
      const dropped = Number(closes[i - period]);
      rollingSum -= Number.isFinite(dropped) ? dropped : 0;
    }

    if (i + 1 < period || !Number.isFinite(close)) {
      ma.push(null);
    } else {
      ma.push(Number((rollingSum / period).toFixed(4)));
    }
  }

  return ma;
}

function buildTradeMarkers(highlight) {
  const action = String(highlight.actionType || "HOLD").toUpperCase();
  const entryPrice = Number(highlight.priceAtProblem);
  const exitPrice = Number(highlight.priceAtEvaluation);

  if (!Number.isFinite(entryPrice) || !Number.isFinite(exitPrice)) {
    return [];
  }

  if (action === "LONG") {
    return [
      {
        name: "LONG 진입",
        coord: [highlight.problemDate, entryPrice],
        itemStyle: { color: "#1f8a4c" },
        symbolRotate: 0,
      },
      {
        name: "포지션 청산",
        coord: [highlight.evaluationDate, exitPrice],
        itemStyle: { color: "#c53d2f" },
        symbolRotate: 180,
      },
    ];
  }

  if (action === "SHORT") {
    return [
      {
        name: "SHORT 진입",
        coord: [highlight.problemDate, entryPrice],
        itemStyle: { color: "#c53d2f" },
        symbolRotate: 180,
      },
      {
        name: "포지션 청산",
        coord: [highlight.evaluationDate, exitPrice],
        itemStyle: { color: "#1f8a4c" },
        symbolRotate: 0,
      },
    ];
  }

  return [
    {
      name: "관망 시작",
      coord: [highlight.problemDate, entryPrice],
      itemStyle: { color: "#5b5f66" },
      symbolRotate: 0,
    },
    {
      name: "관망 종료",
      coord: [highlight.evaluationDate, exitPrice],
      itemStyle: { color: "#5b5f66" },
      symbolRotate: 180,
    },
  ];
}

export function useEChartRenderer() {
  const chartRef = ref(null);
  let chartInstance = null;

  function handleWindowResize() {
    if (chartInstance) {
      chartInstance.resize();
    }
  }

  function renderChart(payload, highlight = {}) {
    if (!chartRef.value) {
      return;
    }

    if (!chartInstance) {
      chartInstance = echarts.init(chartRef.value);
    }

    const candles = payload.candles || [];
    const labels = candles.map((c) => c.date);
    const ohlc = candles.map((c) => [Number(c.open), Number(c.close), Number(c.low), Number(c.high)]);
    const closes = candles.map((c) => Number(c.close));
    const ma5 = buildMovingAverage(closes, 5);
    const ma20 = buildMovingAverage(closes, 20);
    const ma60 = buildMovingAverage(closes, 60);
    const ma120 = buildMovingAverage(closes, 120);

    const rsiMap = new Map((payload.indicators?.RSI || []).map((item) => [item.date, Number(item.value)]));
    const macdMap = new Map(
      (payload.indicators?.MACD || []).map((item) => [item.date, { macd: Number(item.macd), signal: Number(item.signal), hist: Number(item.hist) }])
    );
    const stochMap = new Map((payload.indicators?.SLOW_STOCHASTIC || []).map((item) => [item.date, { k: Number(item.k), d: Number(item.d) }]));

    const rsiSeries = labels.map((d) => (rsiMap.has(d) ? rsiMap.get(d) : null));
    const macdLine = labels.map((d) => (macdMap.has(d) ? macdMap.get(d).macd : null));
    const macdSignal = labels.map((d) => (macdMap.has(d) ? macdMap.get(d).signal : null));
    const macdHist = labels.map((d) => (macdMap.has(d) ? macdMap.get(d).hist : null));
    const stochK = labels.map((d) => (stochMap.has(d) ? stochMap.get(d).k : null));
    const stochD = labels.map((d) => (stochMap.has(d) ? stochMap.get(d).d : null));
    const markLineData = [];
    const markPointData = [];

    if (highlight.problemDate) {
      markLineData.push({ xAxis: highlight.problemDate, name: "문제시점" });
    }
    if (highlight.evaluationDate) {
      markLineData.push({ xAxis: highlight.evaluationDate, name: "정답시점" });
    }

    if (highlight.problemDate && highlight.evaluationDate) {
      markPointData.push(...buildTradeMarkers(highlight));
    }

    chartInstance.setOption({
      animation: false,
      legend: {
        data: ["Candles", "MA5", "MA20", "MA60", "MA120", "RSI", "MACD", "Signal", "Hist", "%K", "%D"],
        top: 0,
      },
      tooltip: { trigger: "axis" },
      axisPointer: { link: [{ xAxisIndex: [0, 1, 2] }] },
      grid: [
        { left: 50, right: 20, top: 30, height: "45%" },
        { left: 50, right: 20, top: "58%", height: "16%" },
        { left: 50, right: 20, top: "78%", height: "16%" },
      ],
      xAxis: [
        { type: "category", data: labels, scale: true, boundaryGap: false, axisLine: { onZero: false } },
        { type: "category", data: labels, gridIndex: 1, axisLabel: { show: false } },
        { type: "category", data: labels, gridIndex: 2 },
      ],
      yAxis: [
        { scale: true, splitArea: { show: true } },
        { gridIndex: 1, min: 0, max: 100 },
        { gridIndex: 2, scale: true },
      ],
      dataZoom: [
        { type: "inside", xAxisIndex: [0, 1, 2], start: 15, end: 100 },
        { type: "slider", xAxisIndex: [0, 1, 2], top: "95%", start: 15, end: 100 },
      ],
      series: [
        {
          name: "Candles",
          type: "candlestick",
          data: ohlc,
          markLine: {
            symbol: ["none", "none"],
            label: { formatter: (param) => param.name },
            lineStyle: { color: "#d9572b", type: "dashed" },
            data: markLineData,
          },
          markPoint: {
            symbol: "triangle",
            symbolSize: 18,
            label: {
              formatter: (param) => param.name,
              color: "#dae2fd",
              fontWeight: 700,
            },
            data: markPointData,
          },
        },
        {
          name: "MA5",
          type: "line",
          data: ma5,
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 1, color: "#3b82f6" },
        },
        {
          name: "MA20",
          type: "line",
          data: ma20,
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 2.5, color: "#f3d33f" },
        },
        {
          name: "MA60",
          type: "line",
          data: ma60,
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 1, color: "#22c55e" },
        },
        {
          name: "MA120",
          type: "line",
          data: ma120,
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 2.5, color: "#ef4444" },
        },
        { name: "RSI", type: "line", xAxisIndex: 1, yAxisIndex: 1, data: rsiSeries, smooth: true },
        { name: "MACD", type: "line", xAxisIndex: 2, yAxisIndex: 2, data: macdLine, smooth: true },
        { name: "Signal", type: "line", xAxisIndex: 2, yAxisIndex: 2, data: macdSignal, smooth: true },
        {
          name: "Hist",
          type: "bar",
          xAxisIndex: 2,
          yAxisIndex: 2,
          data: macdHist,
          itemStyle: {
            color: (p) => (p.value >= 0 ? "#23a55a" : "#d95140"),
          },
        },
        { name: "%K", type: "line", xAxisIndex: 1, yAxisIndex: 1, data: stochK, smooth: true },
        { name: "%D", type: "line", xAxisIndex: 1, yAxisIndex: 1, data: stochD, smooth: true },
      ],
    });
  }

  onMounted(() => {
    window.addEventListener("resize", handleWindowResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener("resize", handleWindowResize);
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
  });

  return {
    chartRef,
    renderChart,
  };
}
