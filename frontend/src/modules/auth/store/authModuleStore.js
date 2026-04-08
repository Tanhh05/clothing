import { defineStore } from "pinia";

export const useAuthModuleStore = defineStore("authModuleStore", {
  state: () => ({
    submitting: false
  }),
  actions: {
    setSubmitting(value) {
      this.submitting = value;
    }
  }
});
