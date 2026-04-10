<template>
  <el-table
    ref="innerTableRef"
    class="base-table"
    :data="data"
    :border="border"
    :stripe="stripe"
    :size="size"
    :table-layout="tableLayout"
    :empty-text="emptyText"
    v-bind="$attrs"
  >
    <slot />
  </el-table>
</template>

<script setup>
import { ref } from "vue";

defineProps({
  data: {
    type: Array,
    default: () => []
  },
  border: {
    type: Boolean,
    default: true
  },
  stripe: {
    type: Boolean,
    default: true
  },
  size: {
    type: String,
    default: "small"
  },
  tableLayout: {
    type: String,
    default: "fixed"
  },
  emptyText: {
    type: String,
    default: "Không có dữ liệu"
  }
});

const innerTableRef = ref(null);

defineExpose({
  clearSelection: () => innerTableRef.value?.clearSelection?.(),
  toggleRowSelection: (...args) => innerTableRef.value?.toggleRowSelection?.(...args),
  toggleAllSelection: () => innerTableRef.value?.toggleAllSelection?.(),
  setCurrentRow: (...args) => innerTableRef.value?.setCurrentRow?.(...args),
  clearFilter: (...args) => innerTableRef.value?.clearFilter?.(...args),
  clearSort: () => innerTableRef.value?.clearSort?.(),
  doLayout: () => innerTableRef.value?.doLayout?.(),
  sort: (...args) => innerTableRef.value?.sort?.(...args)
});
</script>
