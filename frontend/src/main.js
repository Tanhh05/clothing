import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import pinia from "./plugins/pinia";
import setupInterceptors from "./services/interceptor";
import BaseTable from "@/components/base/BaseTable.vue";

import "./styles/main.scss";

setupInterceptors(pinia);

const app = createApp(App);

app.component("BaseTable", BaseTable);
app.use(pinia);
app.use(router);

app.mount("#app");
