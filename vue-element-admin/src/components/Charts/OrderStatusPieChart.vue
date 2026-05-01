<template>
  <div :class="className" :style="{height, width}" />
</template>

<script>
import echarts from 'echarts'
import resize from './mixins/resize'

export default {
  name: 'OrderStatusPieChart',
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
    statusCounts: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      chart: null
    }
  },
  watch: {
    statusCounts: {
      deep: true,
      handler() {
        this.setOptions()
      }
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
      const data = Object.keys(this.statusCounts || {}).map((key) => ({
        name: key,
        value: Number(this.statusCounts[key] || 0)
      }))

      this.chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, type: 'scroll' },
        series: [
          {
            name: 'Trạng thái đơn',
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '42%'],
            data,
            label: { formatter: '{b}: {c}' }
          }
        ]
      }, true)
    }
  }
}
</script>
