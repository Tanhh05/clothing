<template>
  <input class="input" :type="type" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ""
  },
  type: {
    type: String,
    default: "text"
  }
});

defineEmits(["update:modelValue"]);
</script>
