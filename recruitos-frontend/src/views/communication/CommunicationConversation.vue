<template>
  <div class="page-container conversation-page">
    <div class="page-header">
      <h2 class="page-title">对话记录</h2>
    </div>

    <div class="conversation-layout">
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
import { ref, computed, nextTick } from 'vue'
import { Search, User, MoreFilled, Document, Promotion, ChatLineRound, Check, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

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

// Mock conversations
const conversations = ref<Conversation[]>([
  {
    id: 1,
    candidateName: '张三',
    channel: 'WECHAT',
    status: '进行中',
    lastMessage: '好的，那我们周三下午两点面试。',
    lastTime: '14:32',
    unread: 2,
    avatarColor: '#3B82F6',
  },
  {
    id: 2,
    candidateName: '李四',
    channel: 'EMAIL',
    status: '进行中',
    lastMessage: '感谢您的offer，我需要考虑一下。',
    lastTime: '昨天',
    unread: 0,
    avatarColor: '#059669',
  },
  {
    id: 3,
    candidateName: '王五',
    channel: 'SMS',
    status: '已完成',
    lastMessage: '面试已结束，等待反馈中。',
    lastTime: '昨天',
    unread: 0,
    avatarColor: '#D97706',
  },
  {
    id: 4,
    candidateName: '赵六',
    channel: 'FEISHU',
    status: '进行中',
    lastMessage: '请问面试需要准备什么材料？',
    lastTime: '周一',
    unread: 1,
    avatarColor: '#DC2626',
  },
  {
    id: 5,
    candidateName: '孙七',
    channel: 'WECHAT',
    status: '已完成',
    lastMessage: '已收到，谢谢。',
    lastTime: '06/02',
    unread: 0,
    avatarColor: '#64748B',
  },
])

// Mock messages for conversation 1 (张三)
const allMessages: Record<number, Message[]> = {
  1: [
    { id: 1, type: 'system', content: '会话开始 - 企微渠道', time: '2026-06-06 09:00' },
    { id: 2, type: 'outgoing', content: '您好张三，我是XX公司HR，看到您的简历非常感兴趣，方便聊一下前端开发工程师岗位吗？', time: '09:01', status: 'read' },
    { id: 3, type: 'incoming', content: '您好，方便的，请问是什么样的岗位？', time: '09:15' },
    { id: 4, type: 'outgoing', content: '我们是一家专注于AI招聘的科技公司，目前在招聘高级前端工程师，负责招聘平台的前端架构设计和开发。薪资范围30-45K，不知道您是否有兴趣？', time: '09:16', status: 'read' },
    { id: 5, type: 'incoming', content: '听起来不错，我目前在看新的机会，能详细了解一下技术栈和团队情况吗？', time: '09:30' },
    { id: 6, type: 'outgoing', content: '技术栈主要是Vue3 + TypeScript + Vite，后端是Go微服务架构。团队目前有15人，前端4人。我们采用敏捷开发，两周一个迭代。', time: '09:32', status: 'read' },
    { id: 7, type: 'system', content: 'AI自动回复已生成候选回复建议', time: '09:32' },
    { id: 8, type: 'incoming', content: '了解了，我很感兴趣。方便安排一次面试吗？', time: '14:20' },
    { id: 9, type: 'outgoing', content: '当然可以！请问您周三下午或者周四上午方便吗？我们安排技术面试+主管面试，大概2小时。', time: '14:25', status: 'delivered' },
    { id: 10, type: 'incoming', content: '好的，那我们周三下午两点面试。', time: '14:32' },
  ],
  2: [
    { id: 1, type: 'system', content: '会话开始 - 邮件渠道', time: '2026-06-05 10:00' },
    { id: 2, type: 'outgoing', content: '尊敬的李四，恭喜您通过我们的面试，我们诚挚邀请您加入XX公司担任产品经理岗位。', time: '10:00', status: 'read' },
    { id: 3, type: 'incoming', content: '感谢您的offer，我需要考虑一下。', time: '16:45' },
  ],
  3: [
    { id: 1, type: 'system', content: '会话开始 - 短信渠道', time: '2026-06-05 08:00' },
    { id: 2, type: 'outgoing', content: '王五您好，提醒您今天下午3点有面试安排，请准时参加。', time: '08:00', status: 'read' },
    { id: 3, type: 'incoming', content: '收到，我会准时到场。', time: '08:20' },
    { id: 4, type: 'system', content: '面试已结束，等待反馈中。', time: '17:00' },
  ],
  4: [
    { id: 1, type: 'system', content: '会话开始 - 飞书渠道', time: '2026-06-04 11:00' },
    { id: 2, type: 'incoming', content: '请问面试需要准备什么材料？', time: '11:05' },
  ],
  5: [
    { id: 1, type: 'system', content: '会话开始 - 企微渠道', time: '2026-06-02 09:00' },
    { id: 2, type: 'outgoing', content: '孙七您好，感谢您参加我们的面试，面试结果将在3个工作日内通知您。', time: '09:00', status: 'read' },
    { id: 3, type: 'incoming', content: '已收到，谢谢。', time: '09:30' },
  ],
}

const searchKeyword = ref('')
const activeConversation = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const messageInput = ref('')
const messageListRef = ref<HTMLElement>()
const templateDrawerVisible = ref(false)

const templateOptions = [
  { id: 1, name: '面试邀请', content: '您好{candidateName}，我们诚挚邀请您参加{positionName}岗位的面试，请于{interviewTime}准时到场。' },
  { id: 2, name: '面试提醒', content: '您好{candidateName}，提醒您明天{interviewTime}有面试安排，请准时参加。' },
  { id: 3, name: 'offer通知', content: '尊敬的{candidateName}，恭喜您通过面试，我们诚挚邀请您加入{positionName}岗位。' },
  { id: 4, name: '结果通知', content: '尊敬的{candidateName}，感谢您参与面试，我们会尽快反馈面试结果。' },
]

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  return conversations.value.filter((c) =>
    c.candidateName.includes(searchKeyword.value)
  )
})

function selectConversation(conv: Conversation) {
  activeConversation.value = conv
  messages.value = allMessages[conv.id] || []
  conv.unread = 0
  nextTick(() => {
    scrollToBottom()
  })
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function sendMessage() {
  if (!messageInput.value.trim() || !activeConversation.value) return

  const newMsg: Message = {
    id: Date.now(),
    type: 'outgoing',
    content: messageInput.value.trim(),
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    status: 'sent',
  }

  messages.value.push(newMsg)

  // Update conversation preview
  activeConversation.value.lastMessage = newMsg.content
  activeConversation.value.lastTime = '刚刚'

  messageInput.value = ''

  nextTick(() => {
    scrollToBottom()
  })

  ElMessage.success('消息已发送')
}

function insertTemplate(content: string) {
  messageInput.value = content
  templateDrawerVisible.value = false
}
</script>

<style scoped lang="scss">
@import '@/assets/styles/variables.scss';
.page-container {
  padding: 20px;
  background: $bg-card;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
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
