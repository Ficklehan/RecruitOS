<template>
  <div class="login-container">
    <div class="brand-section">
      <div class="brand-decor">
        <div class="decor-line decor-line-1"></div>
        <div class="decor-line decor-line-2"></div>
        <div class="decor-line decor-line-3"></div>
        <div class="decor-grid">
          <div v-for="i in 16" :key="i" class="grid-dot"></div>
        </div>
      </div>

      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 44 44" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="44" height="44" rx="12" fill="white" fill-opacity="0.15"/>
            <path d="M13 16h18M13 22h13M13 28h8" stroke="white" stroke-width="2.2" stroke-linecap="round"/>
            <circle cx="31" cy="28" r="4" fill="white" opacity="0.85"/>
          </svg>
        </div>
        <h1 class="brand-title">RecruitOS</h1>
        <p class="brand-tagline">集团级 AI 招聘操作系统</p>

        <div class="brand-features">
          <div v-for="feature in features" :key="feature" class="feature-item">
            <div class="feature-icon">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M13.3 4.3L6 11.6 2.7 8.3" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </div>
            <span>{{ feature }}</span>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        &copy; 2024 RecruitOS. All rights reserved.
      </div>
    </div>

    <div class="login-section">
      <div class="login-form-wrapper">
        <div class="mobile-logo">
          <svg viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="36" height="36" rx="10" fill="$primary-color"/>
            <path d="M11 13h14M11 18.5h10M11 24h6" stroke="white" stroke-width="2.2" stroke-linecap="round"/>
            <circle cx="27" cy="24" r="3.5" fill="white" opacity="0.9"/>
          </svg>
        </div>

        <h2 class="login-title">欢迎回来</h2>
        <p class="login-desc">登录你的 RecruitOS 账号继续工作</p>

        <form class="login-form" @submit.prevent="handleLogin" @keyup.enter="handleLogin">
          <FormField v-if="!isPlatformLogin" label="所属租户" required :error="errors.tenantId">
            <RSelect
              v-model="loginForm.tenantId"
              :options="tenantOptions"
              placeholder="选择所属租户"
              class="w-full"
            />
          </FormField>

          <FormField label="用户名" required :error="errors.username">
            <div class="relative">
              <User class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <RInput v-model="loginForm.username" placeholder="用户名" class="pl-9" />
            </div>
          </FormField>

          <FormField label="密码" required :error="errors.password">
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <RInput
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="密码"
                class="pl-9 pr-9"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
          </FormField>

          <div class="form-options">
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <RCheckbox v-model="rememberMe" />
              <span>记住我</span>
            </label>
            <a href="javascript:;" class="forgot-link">忘记密码？</a>
          </div>

          <RButton type="submit" class="login-btn" :disabled="loading">
            <Loader2 v-if="loading" class="mr-2 h-4 w-4 animate-spin" />
            {{ loading ? '登录中...' : '登 录' }}
          </RButton>
        </form>

        <div class="platform-toggle">
          <a href="javascript:;" @click="isPlatformLogin = !isPlatformLogin">
            {{ isPlatformLogin ? '← 租户用户登录' : '平台管理员登录 →' }}
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Eye, EyeOff, Loader2 } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { toast } from '@/lib/notify'
import FormField from '@/components/app/FormField.vue'
import { RButton, RInput, RSelect, RCheckbox } from '@/components/ui'
import { getSimpleTenantList } from '@/api/modules/tenant'
import { platformLoginApi } from '@/api/modules/platform'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const rememberMe = ref(false)
const showPassword = ref(false)
const isPlatformLogin = ref(false)

const loginForm = reactive({
  tenantId: undefined as number | undefined,
  username: '',
  password: '',
})

const errors = reactive({
  tenantId: '',
  username: '',
  password: '',
})

const tenantList = ref<any[]>([])
const tenantOptions = computed(() =>
  tenantList.value.map((t) => ({ label: t.companyName, value: t.id }))
)

const features = [
  'AI 驱动的智能简历解析与匹配',
  '全流程招聘管理，从需求到入职',
  '数据驱动的招聘决策分析',
  'Boss、猎聘等渠道一体化招聘',
]

async function loadTenantList() {
  try {
    const res: any = await getSimpleTenantList()
    const data = res.data
    tenantList.value = Array.isArray(data) ? data : (data?.list || [])
  } catch {
    tenantList.value = [{ id: 1, companyName: '默认租户' }]
  }
}

function validate(): boolean {
  errors.tenantId = ''
  errors.username = ''
  errors.password = ''
  let ok = true
  if (!isPlatformLogin.value && !loginForm.tenantId) {
    errors.tenantId = '请选择租户'
    ok = false
  }
  if (!loginForm.username.trim()) {
    errors.username = '请输入用户名'
    ok = false
  }
  if (!loginForm.password) {
    errors.password = '请输入密码'
    ok = false
  }
  return ok
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  try {
    if (isPlatformLogin.value) {
      const { data } = await platformLoginApi({
        username: loginForm.username,
        password: loginForm.password,
      })
      userStore.setToken(data.token)
      localStorage.setItem('isPlatformAdmin', 'true')
      localStorage.setItem('platformAdminName', data.realName || data.username)
      localStorage.removeItem('tenantId')
      toast.success('登录成功')
      router.push('/platform/tenants')
    } else {
      await userStore.login({
        username: loginForm.username,
        password: loginForm.password,
        tenantId: loginForm.tenantId,
      })
      localStorage.setItem('tenantId', String(loginForm.tenantId))
      localStorage.removeItem('isPlatformAdmin')
      localStorage.removeItem('platformAdminName')
      if (rememberMe.value) {
        localStorage.setItem('rememberedUser', loginForm.username)
      }
      toast.success('登录成功')
      router.push('/')
    }
  } catch {
    // errors handled in store
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTenantList()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.login-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.brand-section {
  flex: 1;
  background: linear-gradient(160deg, $primary-dark 0%, $primary-color 40%, $primary-700 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 56px 52px;
  position: relative;
  overflow: hidden;
}

.brand-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.decor-line {
  position: absolute;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 1px;

  &-1 {
    width: 1px;
    height: 60%;
    top: -10%;
    right: 25%;
    transform: rotate(15deg);
    transform-origin: top;
  }

  &-2 {
    width: 1px;
    height: 45%;
    bottom: -5%;
    right: 40%;
    transform: rotate(-10deg);
    transform-origin: bottom;
  }

  &-3 {
    width: 30%;
    height: 1px;
    top: 35%;
    right: -5%;
    transform: rotate(8deg);
  }
}

.decor-grid {
  position: absolute;
  top: 15%;
  right: 8%;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  opacity: 0.12;

  .grid-dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: white;
  }
}

.brand-content {
  position: relative;
  z-index: 1;
}

.brand-logo {
  width: 56px;
  height: 56px;
  margin-bottom: 28px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.brand-title {
  font-size: 34px;
  font-weight: 700;
  color: var(--r-bg-card);
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.brand-tagline {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 48px;
  font-weight: 400;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 14px;
  font-weight: 400;
}

.feature-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-footer {
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  position: relative;
  z-index: 1;
}

.login-section {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--r-bg-card);
}

.login-form-wrapper {
  width: 100%;
  max-width: 340px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mobile-logo {
  display: none;
  width: 44px;
  height: 44px;
  margin-bottom: 32px;

  svg {
    width: 100%;
    height: 100%;
  }
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.login-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 36px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.forgot-link {
  font-size: 13px;
  color: $text-secondary;
  text-decoration: none;
  transition: color $transition-fast;

  &:hover {
    color: $primary-color;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: $border-radius-sm;
  letter-spacing: 0.02em;
}

.platform-toggle {
  text-align: center;
  margin-top: 20px;

  a {
    font-size: 13px;
    color: $text-placeholder;
    text-decoration: none;
    transition: color $transition-fast;

    &:hover {
      color: $primary-color;
    }
  }
}

@media (max-width: 768px) {
  .brand-section {
    display: none;
  }

  .login-section {
    width: 100%;
  }

  .mobile-logo {
    display: block;
  }
}
</style>
