import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useConfirmDialogStore = defineStore('confirmDialog', () => {
  const isVisible = ref(false);
  const title = ref('');
  const message = ref('');
  const confirmButtonText = ref('OK');
  const cancelButtonText = ref('Cancel');
  const onConfirm = ref(null);
  const onCancel = ref(null);
  const isLoading = ref(false);
  const dialogType = ref('confirm'); // confirm | alert

  const show = (options = {}) => {
    title.value = options.title || 'Xác nhận';
    message.value = options.message || '';
    confirmButtonText.value = options.confirmButtonText || 'OK';
    cancelButtonText.value = options.cancelButtonText || 'Cancel';
    dialogType.value = options.type || 'confirm';
    onConfirm.value = options.onConfirm || null;
    onCancel.value = options.onCancel || null;
    isLoading.value = false;
    isVisible.value = true;
  };

  const hide = () => {
    isVisible.value = false;
    setTimeout(() => {
      title.value = '';
      message.value = '';
      confirmButtonText.value = 'OK';
      cancelButtonText.value = 'Cancel';
      onConfirm.value = null;
      onCancel.value = null;
      isLoading.value = false;
    }, 300);
  };

  const confirm = async () => {
    if (onConfirm.value) {
      try {
        isLoading.value = true;
        await onConfirm.value();
      } finally {
        isLoading.value = false;
        hide();
      }
    } else {
      hide();
    }
  };

  const cancel = () => {
    if (onCancel.value) {
      onCancel.value();
    }
    hide();
  };

  return {
    isVisible,
    title,
    message,
    confirmButtonText,
    cancelButtonText,
    isLoading,
    dialogType,
    show,
    hide,
    confirm,
    cancel
  };
});

