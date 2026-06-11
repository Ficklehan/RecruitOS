<template>
  <router-view />
  <ToastHost />
  <ConfirmHost />
  <PromptHost />
</template>

<script setup lang="ts">
import ToastHost from '@/components/common/ToastHost.vue'
import ConfirmHost from '@/components/common/ConfirmHost.vue'
import PromptHost from '@/components/common/PromptHost.vue'
</script>
