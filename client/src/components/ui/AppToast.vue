<script setup lang="ts">
import { useToast } from '../../composables/useToast'
import AppToastCard from './AppToastCard.vue'

const { toasts, dismiss } = useToast()
</script>

<template>
  <Teleport to="body">
    <div
      class="pointer-events-none fixed inset-x-4 bottom-24 z-[100] flex flex-col items-stretch gap-2 md:inset-x-auto md:bottom-auto md:right-4 md:top-4 md:w-96 md:items-end"
      aria-live="polite"
      aria-atomic="false"
    >
      <TransitionGroup
        name="toast"
        tag="div"
        class="flex w-full flex-col items-stretch gap-2 md:items-end"
      >
        <AppToastCard
          v-for="item in toasts"
          :key="item.id"
          :item="item"
          @dismiss="dismiss"
        />
      </TransitionGroup>
    </div>
  </Teleport>
</template>
