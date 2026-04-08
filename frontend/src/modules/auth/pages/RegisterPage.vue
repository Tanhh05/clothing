<template>
  <section>
    <h1>Register</h1>
    <form class="stack" @submit.prevent="onSubmit">
      <BaseInput v-model="form.username" type="text" />
      <BaseInput v-model="form.email" type="text" />
      <BaseInput v-model="form.password" type="password" />
      <BaseInput v-model="form.fullName" type="text" />
      <BaseInput v-model="form.phone" type="text" />
      <BaseButton type="submit">Register</BaseButton>
    </form>
  </section>
</template>

<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";
import BaseButton from "@/components/base/BaseButton.vue";
import BaseInput from "@/components/base/BaseInput.vue";
import { useAuthStore } from "@/store/authStore";

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  username: "",
  email: "",
  password: "",
  fullName: "",
  phone: ""
});

async function onSubmit() {
  await authStore.register(form);
  router.push("/auth/login");
}
</script>
