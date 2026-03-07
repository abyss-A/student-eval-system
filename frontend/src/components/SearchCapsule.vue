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

.search-capsule-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  min-height: 36px;
  box-shadow: 0 0 0 1px #c7d7f2 inset;
}

.search-capsule-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #abc3e6 inset;
}

.search-capsule-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #709bd2 inset, 0 0 0 3px rgba(47, 109, 184, 0.14);
}

.search-capsule-icon {
  cursor: pointer;
  color: #355f96;
}

.search-capsule-icon:hover {
  color: #1f5fae;
}
</style>
