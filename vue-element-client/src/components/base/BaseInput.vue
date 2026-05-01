<template>
  <el-input
    :model-value="modelValue"
    :type="type"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :readonly="readonly"
    :maxlength="maxlength"
    :show-word-limit="showWordLimit"
    :prefix-icon="prefixIcon"
    :suffix-icon="suffixIcon"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event)"
    @keyup.enter="$emit('enter')"
  />
</template>

<script setup>
defineProps({
  modelValue: {
    type: [String, Number],
    default: ""
  },
  type: {
    type: String,
    default: "text"
  },
  placeholder: {
    type: String,
    default: ""
  },
  clearable: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  maxlength: {
    type: [Number, String],
    default: undefined
  },
  showWordLimit: {
    type: Boolean,
    default: false
  },
  prefixIcon: {
    type: [Object, Function, String],
    default: null
  },
  suffixIcon: {
    type: [Object, Function, String],
    default: null
  }
});

defineEmits(["update:modelValue", "enter"]);
</script>
