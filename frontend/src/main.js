import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import pinia from "./plugins/pinia";
import setupInterceptors from "./services/interceptor";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";

import "./styles/main.scss";

setupInterceptors(pinia);

const app = createApp(App);

// Register Element Plus Icons globally
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(pinia);
app.use(router);
app.use(ElementPlus);

app.mount("#app");
