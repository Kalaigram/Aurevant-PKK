<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import AppSkeleton from './AppSkeleton.vue'

withDefaults(
  defineProps<{
    loading?: boolean
    empty?: boolean
    colspan?: number
    emptyTitle?: string
    emptyMessage?: string
    emptyIcon?: string
    caption?: string
    dense?: boolean
    skeletonRows?: number
  }>(),
  {
    loading: false,
    empty: false,
    colspan: 1,
    emptyTitle: '',
    emptyMessage: 'Belum ada data.',
    emptyIcon: 'inbox',
    caption: '',
    dense: false,
    skeletonRows: 5,
  },
)
</script>

<template>
  <div class="app-table-wrap">
    <div class="overflow-x-auto">
      <table class="app-table" :class="dense ? '[&_td]:py-2 [&_th]:py-2' : ''">
        <caption v-if="caption" class="sr-only">{{ caption }}</caption>
        <thead>
          <slot name="head" />
        </thead>
        <tbody v-if="loading">
          <tr v-for="row in skeletonRows" :key="row">
            <td :colspan="colspan">
              <AppSkeleton height="0.7rem" width="100%" />
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="empty">
          <tr>
            <td :colspan="colspan">
              <slot name="empty">
                <div class="flex flex-col items-center justify-center px-4 py-8 text-center">
                  <span class="mb-3 flex h-10 w-10 items-center justify-center rounded-full bg-surface-low text-secondary">
                    <AppIcon :name="emptyIcon" :size="22" />
                  </span>
                  <p v-if="emptyTitle" class="font-display text-sm font-semibold text-ink">
                    {{ emptyTitle }}
                  </p>
                  <p class="mt-1 max-w-sm text-sm text-secondary">{{ emptyMessage }}</p>
                  <div v-if="$slots['empty-action']" class="mt-3">
                    <slot name="empty-action" />
                  </div>
                </div>
              </slot>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <slot />
        </tbody>
        <tfoot v-if="$slots.foot">
          <slot name="foot" />
        </tfoot>
      </table>
    </div>
  </div>
</template>
