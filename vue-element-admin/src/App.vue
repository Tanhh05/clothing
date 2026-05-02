<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    uiSize() {
      return this.$store.getters.size || 'medium'
    }
  },
  watch: {
    uiSize: {
      immediate: true,
      handler(size) {
        const sizeClassNames = ['ui-font-default', 'ui-font-medium', 'ui-font-small', 'ui-font-mini']
        document.body.classList.remove(...sizeClassNames)
        document.body.classList.add(`ui-font-${size}`)
      }
    }
  }
}
</script>
