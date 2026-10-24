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
  <div class="app-table-wrap" :aria-busy="loading || undefined">
    <div
      class="overflow-x-auto overscroll-x-contain focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary/55"
      role="region"
      tabindex="0"
      :aria-label="caption || 'Tabel data'"
    >
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
          <tr>
            <td :colspan="colspan" class="sr-only">
              <span role="status">Memuat data…</span>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="empty">
          <tr>
            <td :colspan="colspan">
              <slot name="empty">
                <div class="flex flex-col items-center justify-center px-4 py-10 text-center">
                  <span class="mb-3 flex h-11 w-11 items-center justify-center rounded-2xl bg-primary-soft/80 text-primary-ink">
                    <AppIcon :name="emptyIcon" :size="22" />
                  </span>
                  <p v-if="emptyTitle" class="font-display text-base font-semibold text-ink">
                    {{ emptyTitle }}
                  </p>
                  <p class="mt-1.5 max-w-sm text-sm leading-relaxed text-secondary">{{ emptyMessage }}</p>
                  <div v-if="$slots['empty-action']" class="mt-4">
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
