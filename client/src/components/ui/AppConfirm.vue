<script setup lang="ts">
import { computed } from 'vue'
import { useConfirm } from '../../composables/useConfirm'
import AppButton from './AppButton.vue'
import AppModal from './AppModal.vue'

const { state, accept, cancel } = useConfirm()
const open = computed(() => state.value.open)
</script>

<template>
  <AppModal
    :open="open"
    :title="state.title"
    size="sm"
    :close-on-backdrop="state.dismissible"
    :hide-close="!state.dismissible"
    @close="cancel"
  >
    <p v-if="state.message" class="text-sm leading-relaxed text-secondary">{{ state.message }}</p>
    <template #footer>
      <AppButton variant="ghost" data-autofocus @click="cancel">{{ state.cancelLabel }}</AppButton>
      <AppButton
        :variant="state.tone === 'danger' ? 'danger' : 'primary'"
        @click="accept"
      >
        {{ state.confirmLabel }}
      </AppButton>
    </template>
  </AppModal>
</template>
