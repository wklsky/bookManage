<script setup lang="ts">
/**
 * @Author: wj 3363891051@qq.com
 * @Date: 2026-09-28 15:30
 * @LastEditors: wj 3363891051@qq.com
 * @LastEditTime: 2026-09-28 15:30
 * @FilePath: bookAdmin/src/components/BaseModal.vue
 * @Description: 通用弹窗容器
 */
interface Props {
  /** 弹窗标题 */
  title: string
  /** 是否展示。由父组件持有该状态，弹窗不自己维护开关，避免关闭后状态不同步 */
  open?: boolean
}

withDefaults(defineProps<Props>(), { open: false })

/** 点击遮罩或关闭按钮时触发，父组件需据此把 open 置为 false */
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <div v-if="open" class="modal-mask" @click.self="emit('close')">
    <section class="modal-card">
      <header class="modal-head">
        <h3>{{ title }}</h3>
        <button class="modal-close" type="button" @click="emit('close')">×</button>
      </header>
      <slot />
      <footer class="modal-foot">
        <slot name="footer" />
      </footer>
    </section>
  </div>
</template>
