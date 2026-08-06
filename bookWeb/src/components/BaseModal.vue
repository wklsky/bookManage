<script setup lang="ts">
withDefaults(
  defineProps<{
    open: boolean
    title: string
    width?: string
  }>(),
  { width: '620px' },
)

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-backdrop" @mousedown.self="emit('close')">
        <section class="modal-panel" :style="{ maxWidth: width }" role="dialog" aria-modal="true">
          <header class="modal-header">
            <h2>{{ title }}</h2>
            <button class="icon-button" type="button" aria-label="关闭" @click="emit('close')">
              ×
            </button>
          </header>
          <div class="modal-body"><slot /></div>
          <footer v-if="$slots.footer" class="modal-footer"><slot name="footer" /></footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
