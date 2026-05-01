<template>
  <el-button
    :type="type"
    :size="size"
    :plain="plain"
    :text="text"
    :link="link"
    :bg="bg"
    :loading="loading"
    :disabled="disabled"
    :round="round"
    :circle="circle"
    :native-type="nativeType"
    :icon="icon"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  >
    <slot />
  </el-button>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: "primary"
  },
  size: {
    type: String,
    default: "default"
  },
  plain: {
    type: Boolean,
    default: false
  },
  text: {
    type: Boolean,
    default: false
  },
  link: {
    type: Boolean,
    default: false
  },
  bg: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  round: {
    type: Boolean,
    default: false
  },
  circle: {
    type: Boolean,
    default: false
  },
  nativeType: {
    type: String,
    default: "button"
  },
  icon: {
    type: [Object, Function, String],
    default: null
  }
});

defineEmits(["click"]);
</script>
