<script setup lang="ts">
import AppInput from './AppInput.vue'

defineOptions({ inheritAttrs: false })

const model = defineModel<string>({ default: '' })
</script>

<template>
  <AppInput v-model="model" v-bind="$attrs" type="date" />
</template>
