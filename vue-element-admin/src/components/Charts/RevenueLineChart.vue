<template>
  <div :class="className" :style="{height, width}" />
</template>

<script>
import echarts from 'echarts'
import resize from './mixins/resize'

export default {
  name: 'RevenueLineChart',
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '360px'
    },
    labels: {
      type: Array,
      default: () => []
    },
    values: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    labels() {
      this.setOptions()
    },
    values() {
      this.setOptions()
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
    })
  },
  beforeDestroy() {
    if (!this.chart) return
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$el)
      this.setOptions()
    },
    setOptions() {
      if (!this.chart) return
      const vm = this
      this.chart.setOption({
        grid: { top: 36, left: 16, right: 16, bottom: 24, containLabel: true },
        tooltip: {
          trigger: 'axis',
          formatter(params) {
            const point = Array.isArray(params) ? params[0] : params
            const value = Number(point && point.value || 0)
            return `${point.axisValue}<br/>${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)}`
          }
        },
        xAxis: {
          type: 'category',
          data: vm.labels
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter(value) {
              return `${Math.round(Number(value) / 1000)}k`
            }
          }
        },
        series: [
          {
            name: 'Doanh thu',
            type: 'line',
            smooth: true,
            data: vm.values,
            symbolSize: 8,
            lineStyle: { width: 3, color: '#409EFF' },
            itemStyle: { color: '#409EFF' },
            areaStyle: { color: 'rgba(64, 158, 255, 0.15)' }
          }
        ]
      }, true)
    }
  }
}
</script>
