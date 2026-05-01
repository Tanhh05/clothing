import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [
        ElementPlusResolver({
          importStyle: "css"
        })
      ]
    }),
    Components({
      resolvers: [
        ElementPlusResolver({
          importStyle: "css"
        })
      ]
    })
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes("node_modules")) return;
          if (id.includes("/axios/")) return "network";
          if (id.includes("/vue-router/")) return "vue-router";
          if (id.includes("/pinia/")) return "pinia";
          if (id.includes("/@vue/") || id.includes("/vue/")) return "vue-core";
        }
      }
    }
  },
  server: {
    host: "localhost",
    port: 5173,
    strictPort: true
  },
  resolve: {
    alias: {
      "@": "/src"
    }
  }
});
