<script setup lang="ts">
import { useToast } from '@/stores/toast'

const toast = useToast()
</script>

<template>
  <div class="toast-stack" aria-live="polite">
    <TransitionGroup name="toast">
      <button
        v-for="message in toast.messages.value"
        :key="message.id"
        type="button"
        class="toast-message"
        :class="`toast-${message.type}`"
        @click="toast.dismiss(message.id)"
      >
        <span class="toast-mark">{{
          message.type === 'success' ? '✓' : message.type === 'error' ? '!' : 'i'
        }}</span>
        <span>{{ message.text }}</span>
      </button>
    </TransitionGroup>
  </div>
</template>
