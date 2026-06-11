<template>
  <Alert :variant="alertVariant" class="rpa-safety-bar">
    <AlertTriangle v-if="simulatedMode" class="h-4 w-4" />
    <AlertCircle v-else class="h-4 w-4" />
    <AlertTitle>{{ title }}</AlertTitle>
    <AlertDescription>
      <p class="rpa-safety-bar__desc">{{ description }}</p>
      <div class="rpa-safety-bar__actions">
        <Button size="sm" variant="outline" :disabled="locking" @click="handleLock">
          {{ locking ? '锁定中…' : '一键锁定测试' }}
        </Button>
        <Button size="sm" variant="destructive" :disabled="locking || unlocking" @click="handleUnlock">
          {{ unlocking ? '开启中…' : '开启平台联调' }}
        </Button>
        <Button size="sm" variant="secondary" :disabled="locking || unlocking || resetting" @click="handleReset">
          {{ resetting ? '恢复中…' : '恢复配置默认' }}
        </Button>
        <Button size="sm" variant="link" :disabled="loading" @click="refresh">
          {{ loading ? '刷新中…' : '刷新状态' }}
        </Button>
      </div>
    </AlertDescription>
  </Alert>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AlertTriangle, AlertCircle } from 'lucide-vue-next'
import { Alert, AlertTitle, AlertDescription, Button } from '@/components/ui'
import { toast } from '@/lib/notify'
import { confirm } from '@/lib/confirm'
import { getRpaStatus, lockRpaTesting, unlockRpaLive, resetRpaAccess } from '@/api/modules/agent'

const emit = defineEmits<{ change: [Record<string, unknown>] }>()

const loading = ref(false)
const locking = ref(false)
const unlocking = ref(false)
const resetting = ref(false)
const status = ref<Record<string, unknown>>({})

const simulatedMode = computed(() => status.value.simulatedMode !== false)
const testingLocked = computed(() => status.value.testingLocked === true)

const alertVariant = computed(() => (simulatedMode.value ? 'warning' : 'destructive'))

const title = computed(() => {
  if (testingLocked.value) return '已锁定：测试模式（不访问 Boss/猎聘）'
  if (simulatedMode.value) return '测试模式：使用模拟数据'
  return '实况模式：将访问 Boss/猎聘'
})

const description = computed(() => {
  if (testingLocked.value) {
    return '全部寻源工作流已暂停，平台访问已锁定。可安全进行功能测试；联调真实平台前请点击「开启平台联调」。'
  }
  if (simulatedMode.value) {
    return '当前不会连接真实招聘平台，工作流使用模拟候选人数据。需要手动联调时请开启平台联调。'
  }
  return '当前将真实访问 Boss直聘 / 猎聘，请注意操作频率与账号安全。联调结束后建议点击「一键锁定测试」。'
})

async function refresh() {
  loading.value = true
  try {
    const res: any = await getRpaStatus()
    status.value = res.data || {}
    emit('change', status.value)
  } catch {
    status.value = { simulatedMode: true, testingLocked: false }
    emit('change', status.value)
  } finally {
    loading.value = false
  }
}

async function handleLock() {
  const ok = await confirm({
    title: '一键锁定测试模式',
    message: '将暂停全部运行中的寻源工作流，并禁止系统访问 Boss/猎聘（仅模拟数据）。',
    confirmText: '确认锁定',
    cancelText: '取消',
  })
  if (!ok) return

  locking.value = true
  try {
    const res: any = await lockRpaTesting()
    toast.success(res.data?.message || '已锁定测试模式')
    await refresh()
  } catch {
    toast.error('锁定失败')
  } finally {
    locking.value = false
  }
}

async function handleUnlock() {
  const ok = await confirm({
    title: '开启平台实况联调',
    message: '开启后系统将真实访问 Boss直聘 / 猎聘，可能触发平台风控。不会自动恢复已暂停的工作流。',
    confirmText: '我了解风险，开启',
    cancelText: '取消',
    destructive: true,
  })
  if (!ok) return

  unlocking.value = true
  try {
    const res: any = await unlockRpaLive({ confirm: true, reason: 'ui manual unlock' })
    toast.error(res.data?.message || '已开启平台联调')
    await refresh()
  } catch {
    toast.error('开启失败')
  } finally {
    unlocking.value = false
  }
}

async function handleReset() {
  resetting.value = true
  try {
    const res: any = await resetRpaAccess()
    toast.success(res.data?.message || '已恢复配置默认')
    await refresh()
  } catch {
    toast.error('恢复失败')
  } finally {
    resetting.value = false
  }
}

onMounted(refresh)

defineExpose({ refresh, simulatedMode, testingLocked })
</script>

<style lang="scss" scoped>
.rpa-safety-bar {
  margin-bottom: 16px;

  &__desc {
    margin: 0 0 10px;
    font-size: 13px;
    line-height: 1.5;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
  }
}
</style>
