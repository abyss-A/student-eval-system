<template>
  <div class="search-capsule" :style="capsuleStyle">
    <el-input
      ref="inputRef"
      :model-value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      clearable
      class="search-capsule-input"
      @input="onInput"
      @keyup.enter="$emit('submit')"
      @clear="clearValue"
    >
      <template #suffix>
        <el-icon class="search-capsule-icon" @click="$emit('submit')">
          <Search />
        </el-icon>
      </template>
    </el-input>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '请输入关键字' },
  disabled: { type: Boolean, default: false },
  width: { type: String, default: '180px' }
})

const emit = defineEmits(['update:modelValue', 'submit', 'clear'])

const inputRef = ref(null)

const capsuleStyle = computed(() => ({
  width: props.width || '180px'
}))

const onInput = (value) => {
  emit('update:modelValue', value || '')
}

const clearValue = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}
</script>

<style scoped>
.search-capsule {
  display: inline-flex;
  align-items: center;
  min-width: 180px;
  max-width: 100%;
}

.search-capsule-input {
  width: 100%;
}

.search-capsule-icon {
  cursor: pointer;
  color: var(--app-text-faint);
}

.search-capsule-icon:hover {
  color: var(--app-primary);
}
</style>
