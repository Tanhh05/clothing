<template>
  <section>
    <h1>Login</h1>
    <form class="stack" @submit.prevent="onSubmit">
      <BaseInput v-model="form.usernameOrEmail" type="text" />
      <BaseInput v-model="form.password" type="password" />
      <BaseButton type="submit">Login</BaseButton>
    </form>
    <div class="or-divider">OR</div>
    <div ref="googleButtonRef"></div>
    <RouterLink to="/auth/register">Create account</RouterLink>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import BaseButton from "@/components/base/BaseButton.vue";
import BaseInput from "@/components/base/BaseInput.vue";
import { useAuthStore } from "@/store/authStore";

const router = useRouter();
const authStore = useAuthStore();
const googleButtonRef = ref(null);
const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

const form = reactive({
  usernameOrEmail: "",
  password: ""
});

async function onSubmit() {
  await authStore.login(form);
  router.push("/products");
}

async function handleGoogleCredential(response) {
  if (!response?.credential) {
    return;
  }
  await authStore.loginWithGoogle(response.credential);
  router.push("/products");
}

onMounted(() => {
  if (!googleClientId || !googleButtonRef.value) {
    return;
  }

  loadGoogleScript().then(() => {
    if (!window.google?.accounts?.id) {
      return;
    }
    window.google.accounts.id.initialize({
      client_id: googleClientId,
      callback: handleGoogleCredential,
      auto_select: false
    });

    window.google.accounts.id.renderButton(googleButtonRef.value, {
      theme: "outline",
      size: "large",
      shape: "rectangular",
      text: "continue_with",
      width: 280
    });
  });
});

function loadGoogleScript() {
  if (window.google?.accounts?.id) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[src="https://accounts.google.com/gsi/client"]');
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("Failed to load Google script")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Failed to load Google script"));
    document.head.appendChild(script);
  });
}
</script>
