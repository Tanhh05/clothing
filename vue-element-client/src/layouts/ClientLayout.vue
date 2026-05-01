<template>
  <div class="layout">
    <AppHeader />
    <main class="container">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import AppHeader from "@/components/common/AppHeader.vue";
import AppFooter from "@/components/common/AppFooter.vue";
import { useStoreSettingsStore } from "@/store/storeSettingsStore";

const storeSettingsStore = useStoreSettingsStore();

onMounted(() => {
  storeSettingsStore.fetchPublicSettings();
});
</script>
