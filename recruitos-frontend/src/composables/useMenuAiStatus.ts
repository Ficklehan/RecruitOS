import { ref, onMounted, onUnmounted } from 'vue'
import { getMenuAiStatus } from '@/api/modules/brain'

export function useMenuAiStatus() {
  const insightAlerts = ref(0)
  const insightAttention = ref(0)
  const insightObserve = ref(0)
  const totalPending = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  async function fetch() {
    try {
      const res = await getMenuAiStatus()
      const data = (res as any).data
      if (data) {
        insightAlerts.value = data.insightAlerts || 0
        insightAttention.value = data.insightAttention || 0
        insightObserve.value = data.insightObserve || 0
        totalPending.value = data.totalPending || 0
      }
    } catch { /* silent */ }
  }

  function startPolling(intervalMs = 60000) {
    fetch()
    timer = setInterval(fetch, intervalMs)
  }

  function stopPolling() {
    if (timer) { clearInterval(timer); timer = null }
  }

  onMounted(() => startPolling())
  onUnmounted(() => stopPolling())

  return { insightAlerts, insightAttention, insightObserve, totalPending, refresh: fetch }
}
