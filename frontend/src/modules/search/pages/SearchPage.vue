<template>
  <section>
    <h1>Search</h1>
    <BaseInput v-model="keyword" type="text" />
    <BaseButton @click="onSearch">Search</BaseButton>
    <ul>
      <li v-for="item in store.results" :key="item.id">
        {{ item.name }} - {{ item.categoryName }}
      </li>
    </ul>
  </section>
</template>

<script setup>
import { ref } from "vue";
import BaseButton from "@/components/base/BaseButton.vue";
import BaseInput from "@/components/base/BaseInput.vue";
import { useSearchStore } from "@/modules/search/store/searchStore";

const keyword = ref("");
const store = useSearchStore();

function onSearch() {
  store.search(keyword.value);
}
</script>
