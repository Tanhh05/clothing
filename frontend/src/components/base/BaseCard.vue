<template>
  <el-card :shadow="shadow" class="base-card" v-bind="$attrs">
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <slot />
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-card>
</template>

<script setup>
defineProps({
  shadow: {
    type: String,
    default: "never"
  }
});
</script>

<style scoped lang="scss">
.base-card {
  border-radius: 10px;
}
</style>
