<template>
  <div />
</template>

<script setup lang="ts">
/** @deprecated 已合并至 /talent/channels，保留文件供旧引用重定向 */
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  router.replace({ path: '/talent/channels', query: route.query })
})
</script>
