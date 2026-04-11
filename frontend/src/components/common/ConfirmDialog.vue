<template>
  <Teleport to="body">
    <transition name="fade">
      <div v-if="dialogStore.isVisible" class="confirm-dialog-overlay">
        <transition name="slide-up">
          <div v-if="dialogStore.isVisible" class="confirm-dialog">
            <!-- Header -->
            <div class="confirm-dialog__header">
              <h3 class="confirm-dialog__title">{{ dialogStore.title }}</h3>
              <button
                type="button"
                class="confirm-dialog__close"
                aria-label="Close"
                @click="dialogStore.cancel"
              >
                ✕
              </button>
            </div>

            <!-- Content -->
            <div class="confirm-dialog__content">
              <!-- Warning icon for alerts -->
              <div v-if="dialogStore.dialogType === 'alert'" class="confirm-dialog__icon confirm-dialog__icon--alert">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3.05h16.94a2 2 0 0 0 1.71-3.05l-8.47-14.14a2 2 0 0 0-3.41 0z"></path>
                  <line x1="12" y1="9" x2="12" y2="13"></line>
                  <line x1="12" y1="17" x2="12.01" y2="17"></line>
                </svg>
              </div>
              <!-- Question mark icon for confirm -->
              <div v-else class="confirm-dialog__icon confirm-dialog__icon--confirm">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="16" x2="12" y2="12"></line>
                  <line x1="12" y1="8" x2="12.01" y2="8"></line>
                </svg>
              </div>
              <p class="confirm-dialog__message">{{ dialogStore.message }}</p>
            </div>

            <!-- Footer -->
            <div class="confirm-dialog__footer">
              <button
                type="button"
                class="confirm-dialog__btn confirm-dialog__btn--cancel"
                @click="dialogStore.cancel"
                :disabled="dialogStore.isLoading"
              >
                {{ dialogStore.cancelButtonText }}
              </button>
              <button
                type="button"
                class="confirm-dialog__btn confirm-dialog__btn--primary"
                @click="dialogStore.confirm"
                :disabled="dialogStore.isLoading"
              >
                <span v-if="dialogStore.isLoading" class="spinner"></span>
                {{ dialogStore.confirmButtonText }}
              </button>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { useConfirmDialogStore } from '@/store/confirmDialogStore';

const dialogStore = useConfirmDialogStore();
</script>

<style scoped lang="scss">
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.confirm-dialog {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  width: min(420px, calc(100vw - 40px));
  overflow: hidden;
  animation: slideUp 0.3s ease-out;
}

.confirm-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: none;
}

.confirm-dialog__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.confirm-dialog__close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;

  &:hover {
    color: #666;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.confirm-dialog__content {
  padding: 8px 16px 12px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.confirm-dialog__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  color: #f0ad4e;
  margin-top: 1px;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 100%;
    height: 100%;
  }

  &--alert {
    color: #e74c3c;
  }

  &--confirm {
    color: #f0ad4e;
  }
}

.confirm-dialog__message {
  margin: 0;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  flex: 1;
}

.confirm-dialog__footer {
  padding: 10px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.confirm-dialog__btn {
  min-width: 72px;
  height: 32px;
  padding: 0 14px;
  border-radius: 4px;
  border: 1px solid;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.confirm-dialog__btn--cancel {
  color: #666;
  border-color: #ddd;
  background: #fff;

  &:hover:not(:disabled) {
    color: #333;
    border-color: #b3b3b3;
    background: #fafafa;
  }
}

.confirm-dialog__btn--primary {
  color: #fff;
  border-color: #409eff;
  background: #409eff;

  &:hover:not(:disabled) {
    border-color: #66b1ff;
    background: #66b1ff;
  }
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>

