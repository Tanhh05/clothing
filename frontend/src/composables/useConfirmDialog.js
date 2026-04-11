import { useConfirmDialogStore } from '@/store/confirmDialogStore';

export const useConfirmDialog = () => {
  const store = useConfirmDialogStore();

  const confirm = (options) => {
    return new Promise((resolve, reject) => {
      store.show({
        title: options.title || 'Xác nhận',
        message: options.message || '',
        confirmButtonText: options.confirmButtonText || 'OK',
        cancelButtonText: options.cancelButtonText || 'Cancel',
        type: 'confirm',
        onConfirm: async () => {
          if (options.onConfirm) {
            try {
              await options.onConfirm();
            } catch (error) {
              reject(error);
              return;
            }
          }
          resolve(true);
        },
        onCancel: () => {
          reject(new Error('cancel'));
          if (options.onCancel) {
            options.onCancel();
          }
        }
      });
    });
  };

  const showError = (title, message) => {
    return new Promise((resolve) => {
      store.show({
        title: title || 'Lỗi',
        message: message || '',
        confirmButtonText: 'OK',
        cancelButtonText: 'Cancel',
        type: 'alert',
        onConfirm: async () => {
          resolve(true);
        },
        onCancel: () => {
          resolve(false);
        }
      });
    });
  };

  const showAlert = (message, title = 'Thông báo') => {
    return showError(title, message);
  };

  return { confirm, showError, showAlert };
};

