<template>
  <div class="page-container page-stack page-viewport-fill conversation-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">对话记录</h2>
        <p class="page-subtitle">查看与候选人的沟通历史并发送消息</p>
      </div>
    </div>

    <div class="conversation-layout page-viewport-fill__body">
      <!-- Left Sidebar -->
      <div class="sidebar">
        <div class="sidebar-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索候选人"
            :prefix-icon="Search"
            clearable
            size="small"
          />
        </div>
        <div class="conversation-list">
          <div
            v-for="conv in filteredConversations"
            :key="conv.id"
            class="conversation-item"
            :class="{ active: activeConversation?.id === conv.id }"
            @click="selectConversation(conv)"
          >
            <div class="conv-avatar" :style="{ background: conv.avatarColor }">
              {{ conv.candidateName.charAt(0) }}
            </div>
            <div class="conv-info">
              <div class="conv-top">
                <span class="conv-name">{{ conv.candidateName }}</span>
                <span class="conv-time">{{ conv.lastTime }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-preview">{{ conv.lastMessage }}</span>
                <el-badge v-if="conv.unread > 0" :value="conv.unread" :max="99" class="conv-badge" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel -->
      <div class="chat-panel">
        <template v-if="activeConversation">
          <!-- Chat Header -->
          <div class="chat-header">
            <div class="chat-header-info">
              <span class="chat-candidate-name">{{ activeConversation.candidateName }}</span>
              <el-tag :type="channelTagMap[activeConversation.channel]" size="small">
                {{ channelLabelMap[activeConversation.channel] }}
              </el-tag>
              <el-tag :type="activeConversation.status === '进行中' ? 'success' : 'info'" size="small" effect="plain">
                {{ activeConversation.status }}
              </el-tag>
            </div>
            <div class="chat-header-actions">
              <el-button :icon="User" size="small" text>查看简历</el-button>
              <el-button :icon="MoreFilled" size="small" text />
            </div>
          </div>

          <!-- Message Timeline -->
          <div ref="messageListRef" class="message-list">
            <div v-for="msg in messages" :key="msg.id" class="message-wrapper" :class="msg.type">
              <!-- System message -->
              <div v-if="msg.type === 'system'" class="system-message">
                <span>{{ msg.content }}</span>
              </div>

              <!-- Agent / Outgoing message -->
              <div v-else-if="msg.type === 'outgoing'" class="message-bubble outgoing">
                <div class="bubble-content">
                  <p>{{ msg.content }}</p>
                </div>
                <div class="bubble-meta">
                  <span class="msg-time">{{ msg.time }}</span>
                  <el-icon v-if="msg.status === 'read'" class="status-icon read"><Check /></el-icon>
                  <el-icon v-else-if="msg.status === 'delivered'" class="status-icon delivered"><Check /></el-icon>
                  <el-icon v-else class="status-icon sent"><Loading /></el-icon>
                </div>
              </div>

              <!-- Candidate / Incoming message -->
              <div v-else class="message-bubble incoming">
                <div class="bubble-avatar" :style="{ background: activeConversation.avatarColor }">
                  {{ activeConversation.candidateName.charAt(0) }}
                </div>
                <div>
                  <div class="bubble-content">
                    <p>{{ msg.content }}</p>
                  </div>
                  <div class="bubble-meta">
                    <span class="msg-time">{{ msg.time }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Input Area -->
          <div class="chat-input">
            <div class="input-actions">
              <el-button :icon="Document" size="small" text @click="templateDrawerVisible = true">插入话术</el-button>
            </div>
            <div class="input-row">
              <el-input
                v-model="messageInput"
                type="textarea"
                :rows="2"
                placeholder="输入消息内容..."
                resize="none"
                @keydown.enter.ctrl.exact.prevent="sendMessage"
              />
              <el-button type="primary" :icon="Promotion" @click="sendMessage" :disabled="!messageInput.trim()">
                发送
              </el-button>
            </div>
          </div>
        </template>

        <!-- Empty State -->
        <div v-else class="empty-chat">
          <el-icon :size="64" color="#dcdfe6"><ChatLineRound /></el-icon>
          <p>请从左侧选择一个对话</p>
        </div>
      </div>
    </div>

    <!-- Template Drawer -->
    <el-drawer v-model="templateDrawerVisible" title="选择话术模板" size="400px">
      <div class="template-list">
        <div
          v-for="tpl in templateOptions"
          :key="tpl.id"
          class="template-option"
          @click="insertTemplate(tpl.content)"
        >
          <div class="tpl-name">{{ tpl.name }}</div>
          <div class="tpl-preview">{{ tpl.content }}</div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { Search, User, MoreFilled, Document, Promotion, ChatLineRound, Check, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getConversationList, getConversationDetail, sendMessage as sendConversationMessage, getTemplateList,
} from '@/api/modules/communication'

type Channel = 'SMS' | 'EMAIL' | 'WECHAT' | 'FEISHU'
type MsgType = 'system' | 'outgoing' | 'incoming'
type MsgStatus = 'sent' | 'delivered' | 'read'

interface Conversation {
  id: number
  candidateName: string
  channel: Channel
  status: string
  lastMessage: string
  lastTime: string
  unread: number
  avatarColor: string
}

interface Message {
  id: number
  type: MsgType
  content: string
  time: string
  status?: MsgStatus
}

const channelTagMap: Record<Channel, string> = {
  SMS: '',
  EMAIL: 'success',
  WECHAT: 'success',
  FEISHU: 'primary',
}

const channelLabelMap: Record<Channel, string> = {
  SMS: '短信',
  EMAIL: '邮件',
  WECHAT: '企微',
  FEISHU: '飞书',
}

const avatarColors = ['#3B82F6', '#059669', '#D97706', '#DC2626', '#64748B']
const conversations = ref<Conversation[]>([])

function formatMsgTime(t?: string) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function mapConversation(row: any, index: number): Conversation {
  const lastMsg = row.messages?.[row.messages.length - 1]
  return {
    id: row.id,
    candidateName: row.candidateName || '未知',
    channel: (row.channel || 'WECHAT') as Channel,
    status: row.status === 'CLOSED' ? '已完成' : '进行中',
    lastMessage: lastMsg?.content || `共 ${row.messageCount || 0} 条消息`,
    lastTime: formatMsgTime(row.lastMessageAt),
    unread: 0,
    avatarColor: avatarColors[index % avatarColors.length],
  }
}

function mapMessages(msgs: any[]): Message[] {
  return (msgs || []).map((m) => ({
    id: m.id,
    type: m.senderType === 'SYSTEM' ? 'system' : m.senderType === 'AGENT' || m.senderType === 'HR' ? 'outgoing' : 'incoming',
    content: m.content,
    time: formatMsgTime(m.sentAt || m.createdAt),
    status: m.status === 'READ' ? 'read' : m.status === 'DELIVERED' ? 'delivered' : 'sent',
  }))
}

async function loadConversations() {
  try {
    const res: any = await getConversationList({ pageNum: 1, pageSize: 50 })
    const list = res.data?.list || res.data?.records || []
    conversations.value = list.map(mapConversation)
  } catch {
    conversations.value = []
  }
}

const searchKeyword = ref('')
const activeConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const messageInput = ref('')
const messageListRef = ref<HTMLElement>()
const templateDrawerVisible = ref(false)

const templateOptions = ref<{ id: number; name: string; content: string }[]>([])

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  return conversations.value.filter((c) =>
    c.candidateName.includes(searchKeyword.value)
  )
})

async function selectConversation(conv: Conversation) {
  activeConversation.value = conv
  conv.unread = 0
  try {
    const res: any = await getConversationDetail(conv.id)
    const detail = res.data || res
    messages.value = mapMessages(detail.messages)
  } catch {
    messages.value = []
  }
  nextTick(scrollToBottom)
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function sendMessage() {
  if (!messageInput.value.trim() || !activeConversation.value) return
  const content = messageInput.value.trim()
  try {
    const res: any = await sendConversationMessage(activeConversation.value.id, { content })
    const detail = res.data || res
    messages.value = mapMessages(detail.messages)
    activeConversation.value.lastMessage = content
    activeConversation.value.lastTime = '刚刚'
    messageInput.value = ''
    nextTick(scrollToBottom)
    ElMessage.success('消息已发送')
  } catch {
    ElMessage.error('发送失败')
  }
}

onMounted(async () => {
  await loadConversations()
  try {
    const res: any = await getTemplateList({ pageNum: 1, pageSize: 50 })
    const list = res.data?.list || res.data?.records || []
    templateOptions.value = list.map((t: any) => ({
      id: t.id,
      name: t.templateName || t.name,
      content: t.content,
    }))
  } catch {
    templateOptions.value = []
  }
})

function insertTemplate(content: string) {
  messageInput.value = content
  templateDrawerVisible.value = false
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.conversation-page {
  min-height: calc(100vh - var(--layout-chrome-height));
}

.conversation-layout {
  display: flex;
  flex: 1;
  border: 1px solid $border-color-light;
  border-radius: 8px;
  overflow: hidden;
  min-height: 0;
}

/* Sidebar */
.sidebar {
  width: 320px;
  border-right: 1px solid $border-color-light;
  display: flex;
  flex-direction: column;
  background: $bg-muted;
  flex-shrink: 0;
}

.sidebar-search {
  padding: 12px;
  border-bottom: 1px solid $border-color-light;
  flex-shrink: 0;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid $border-color-light;

  &:hover {
    background: $primary-lighter;
  }

  &.active {
    background: $primary-lighter;
    border-right: 3px solid $primary-color;
  }
}

.conv-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-name {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

.conv-time {
  font-size: 12px;
  color: $text-secondary;
  flex-shrink: 0;
}

.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conv-preview {
  font-size: 12px;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

/* Chat Panel */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid $border-color-light;
  flex-shrink: 0;
  background: $bg-card;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-candidate-name {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.chat-header-actions {
  display: flex;
  gap: 4px;
}

/* Messages */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: $bg-muted;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-wrapper.system {
  display: flex;
  justify-content: center;
}

.system-message {
  background: $border-color-light;
  color: $text-secondary;
  font-size: 12px;
  padding: 4px 16px;
  border-radius: 12px;
  text-align: center;
}

.message-bubble {
  display: flex;
  max-width: 70%;

  &.outgoing {
    align-self: flex-end;
    flex-direction: column;
    align-items: flex-end;

    .bubble-content {
      background: $primary-color;
      color: #fff;
      border-radius: 12px 12px 4px 12px;
    }
  }

  &.incoming {
    align-self: flex-start;
    align-items: flex-start;
    gap: 8px;

    .bubble-content {
      background: $bg-card;
      color: $text-primary;
      border-radius: 12px 12px 12px 4px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
    }
  }
}

.bubble-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.bubble-content {
  padding: 10px 14px;

  p {
    margin: 0;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding: 0 4px;
}

.msg-time {
  font-size: 11px;
  color: $text-placeholder;
}

.status-icon {
  font-size: 14px;

  &.read {
    color: $primary-color;
  }
  &.delivered {
    color: $success-color;
  }
  &.sent {
    color: $text-placeholder;
  }
}

/* Input Area */
.chat-input {
  border-top: 1px solid $border-color-light;
  padding: 12px 20px;
  flex-shrink: 0;
  background: $bg-card;
}

.input-actions {
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;

  .el-textarea {
    flex: 1;
  }
}

/* Empty State */
.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;

  p {
    color: $text-placeholder;
    font-size: 14px;
    margin: 0;
  }
}

/* Template Drawer */
.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.template-option {
  padding: 12px;
  border: 1px solid $border-color-light;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $primary-color;
    background: $primary-lighter;
  }
}

.tpl-name {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 6px;
}

.tpl-preview {
  font-size: 12px;
  color: $text-secondary;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
