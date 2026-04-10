<template>
  <section class="base-page">
    <div class="base-page__inner">
      <header v-if="title || subtitle || $slots.header" class="base-page__header">
        <div class="base-page__heading">
          <h1 v-if="title" class="base-page__title">{{ title }}</h1>
          <p v-if="subtitle" class="base-page__subtitle">{{ subtitle }}</p>
        </div>
        <div v-if="$slots.header" class="base-page__actions">
          <slot name="header" />
        </div>
      </header>
      <div class="base-page__body">
        <slot />
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ""
  },
  subtitle: {
    type: String,
    default: ""
  }
});
</script>

<style scoped lang="scss">
.base-page {
  padding: 24px 16px 40px;
}

.base-page__inner {
  max-width: 1240px;
  margin: 0 auto;
}

.base-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.base-page__title {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: -0.4px;
  color: #111827;
}

.base-page__subtitle {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.base-page__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.base-page__body {
  min-height: 220px;
}

@media (max-width: 768px) {
  .base-page {
    padding: 16px 12px 28px;
  }

  .base-page__header {
    flex-direction: column;
    gap: 10px;
  }

  .base-page__title {
    font-size: 24px;
  }
}
</style>
