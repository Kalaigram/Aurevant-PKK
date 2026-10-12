<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
import { useRoute } from 'vue-router'
import { RouterView } from 'vue-router'

const auth = useAuthStore()
const route = useRoute()
const id = Number(route.params.companyId)
if (auth.companyId !== id) {
  auth.companyId = id
  auth.persist()
}
</script>

<template><RouterView /></template>
