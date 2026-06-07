<template>
  <div class="header">
    <div class="header-left">
      <el-icon class="collapse-trigger" @click="appStore.toggleSidebar()">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
      <!-- 面包屑或页面标题可由 Breadcrumb 组件处理 -->
    </div>

    <div class="header-right">
      <!-- 搜索入口 -->
      <div class="search-hint" @click="handleSearchHint">
        <el-icon :size="15"><Search /></el-icon>
        <span>搜索...</span>
        <kbd>⌘K</kbd>
      </div>

      <!-- 通知 -->
      <el-popover placement="bottom-end" :width="320" trigger="click">
        <template #reference>
          <div class="header-icon-btn">
            <el-badge :value="3" :max="99" :offset="[-4, 4]">
              <el-icon :size="19"><Bell /></el-icon>
            </el-badge>
          </div>
        </template>
        <div class="notification-panel">
          <div class="notification-title">通知</div>
          <div class="notification-list">
            <div class="notification-item unread">
              <div class="notification-dot"></div>
              <div>
                <p>张三 通过了「高级前端工程师」面试</p>
                <span>10 分钟前</span>
              </div>
            </div>
            <div class="notification-item unread">
              <div class="notification-dot"></div>
              <div>
                <p>新需求「产品经理」等待审批</p>
                <span>30 分钟前</span>
              </div>
            </div>
            <div class="notification-item">
              <div class="notification-dot-placeholder"></div>
              <div>
                <p>李四 的 Offer 已发送</p>
                <span>1 小时前</span>
              </div>
            </div>
          </div>
        </div>
      </el-popover>

      <!-- 用户下拉菜单 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <div class="user-avatar">
            {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
          </div>
          <span class="user-name">{{ userStore.userInfo?.realName || '用户' }}</span>
          <el-icon class="user-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人信息
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>
              修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

function handleSearchHint() {
  // 预留全局搜索
}

async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      break
    case 'password':
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
          confirmButtonText: '退出',
          cancelButtonText: '取消',
          type: 'warning',
        })
        await userStore.logout()
        router.push('/login')
      } catch {
        // 取消
      }
      break
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header {
  height: $header-height;
  background-color: $header-bg;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-xl;
  border-bottom: 1px solid $header-border;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-trigger {
  font-size: 20px;
  cursor: pointer;
  color: $text-secondary;
  transition: color $transition-fast;
  display: none;
  padding: 4px;
  border-radius: $border-radius-sm;

  &:hover {
    color: $text-primary;
    background-color: $bg-muted;
  }

  @media (max-width: 768px) {
    display: flex;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

// 搜索提示框
.search-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: $border-radius-sm;
  border: 1px solid $border-color;
  color: $text-placeholder;
  font-size: 13px;
  cursor: pointer;
  transition: all $transition-fast;
  min-width: 180px;

  &:hover {
    border-color: $primary-light;
    color: $text-secondary;
  }

  span {
    flex: 1;
  }

  kbd {
    font-family: inherit;
    font-size: 11px;
    padding: 1px 5px;
    border-radius: 4px;
    border: 1px solid $border-color;
    background: $bg-muted;
    color: $text-placeholder;
  }
}

.header-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: $border-radius-sm;
  cursor: pointer;
  color: $text-secondary;
  transition: all $transition-fast;

  &:hover {
    background-color: $bg-muted;
    color: $text-primary;
  }
}

.notification-panel {
  .notification-title {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-color-light;
    margin-bottom: 8px;
  }

  .notification-list {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .notification-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 10px 8px;
    border-radius: $border-radius-sm;
    cursor: pointer;
    transition: background $transition-fast;

    &:hover {
      background: $bg-muted;
    }

    &.unread p {
      font-weight: 500;
      color: $text-primary;
    }

    p {
      font-size: 13px;
      color: $text-regular;
      line-height: 1.5;
      margin-bottom: 2px;
    }

    span {
      font-size: 12px;
      color: $text-placeholder;
    }
  }

  .notification-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: $primary-color;
    flex-shrink: 0;
    margin-top: 6px;
  }

  .notification-dot-placeholder {
    width: 7px;
    height: 7px;
    flex-shrink: 0;
    margin-top: 6px;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: $border-radius-sm;
  transition: background-color $transition-fast;

  &:hover {
    background-color: $bg-muted;
  }
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: linear-gradient(135deg, $primary-color, $primary-dark);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  color: $text-regular;
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: $text-placeholder;
}
</style>
