<script setup lang="ts">
withDefaults(
  defineProps<{
    size?: 'sm' | 'md' | 'lg' | 'xl'
    framed?: boolean
  }>(),
  { size: 'md', framed: false },
)

const sizes = {
  sm: 'h-8 w-8',
  md: 'h-10 w-10',
  lg: 'h-12 w-12',
  xl: 'h-16 w-16',
}
</script>

<template>
  <span
    class="inline-flex shrink-0 items-center justify-center"
    :class="[
      sizes[size],
      framed ? 'rounded-2xl bg-[#f7f4ec] p-1.5 shadow-card ring-1 ring-black/10' : '',
    ]"
    aria-hidden="true"
  >
    <img
      src="/brand/aurevant-mark.png"
      alt=""
      class="h-full w-full object-contain"
      draggable="false"
    />
  </span>
</template>
