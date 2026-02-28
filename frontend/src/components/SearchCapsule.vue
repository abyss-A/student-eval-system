<template>
  <div class="search-capsule" :style="capsuleStyle">
    <input
      ref="inputRef"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      class="search-capsule-input"
      @input="onInput"
      @keyup.enter="$emit('submit')"
    />
    <button
      v-if="showClear"
      type="button"
      class="search-capsule-clear"
      :disabled="disabled"
      aria-label="清空"
      @click="clearValue"
    >
      ×
    </button>
    <button
      type="button"
      class="search-capsule-action"
      :disabled="disabled"
      aria-label="搜索"
      @click="$emit('submit')"
    >
      <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
        <path
          d="M10.5 3a7.5 7.5 0 1 0 4.8 13.3l4.2 4.2a1 1 0 0 0 1.4-1.4l-4.2-4.2A7.5 7.5 0 0 0 10.5 3Zm0 2a5.5 5.5 0 1 1 0 11a5.5 5.5 0 0 1 0-11Z"
          fill="currentColor"
        />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入关键字' },
  disabled: { type: Boolean, default: false },
  width: { type: String, default: '320px' }
})

const emit = defineEmits(['update:modelValue', 'submit', 'clear'])

const inputRef = ref(null)

const capsuleStyle = computed(() => ({
  width: props.width || '320px'
}))

const showClear = computed(() => String(props.modelValue || '').length > 0)

const onInput = (e) => {
  emit('update:modelValue', e?.target?.value || '')
}

const clearValue = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}
</script>
