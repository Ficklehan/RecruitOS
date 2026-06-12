import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const talentRoutes: RouteRecordRaw[] = [
  {
    path: '/talent',
    component: AppLayout,
    redirect: '/talent/pool',
    meta: { title: '人才库', permission: 'talent' },
    children: [
      {
        path: 'pool',
        name: 'TalentPool',
        component: () => import('@/views/candidate/CandidatePool.vue'),
        meta: { title: '人才库', permission: 'talent:pool', icon: 'Files' },
      },
      {
        path: 'agent-feed',
        name: 'TalentAgentFeed',
        component: () => import('@/views/talent/AgentFeed.vue'),
        meta: { title: 'AI 推荐', permission: 'talent:pool', icon: 'Sparkles' },
      },
      {
        path: 'search',
        name: 'TalentSearch',
        component: () => import('@/views/talent/ActiveSearch.vue'),
        meta: { title: '主动搜索', permission: 'talent:resume', icon: 'Search' },
      },
      {
        path: 'resumes',
        name: 'TalentResumes',
        component: () => import('@/views/resume/ResumeList.vue'),
        meta: { title: '简历收件', permission: 'talent:resume', icon: 'Document' },
      },
      {
        path: 'resumes/upload',
        name: 'TalentResumeUpload',
        component: () => import('@/views/resume/ResumeUpload.vue'),
        meta: { title: '上传简历', hidden: true },
      },
      {
        path: 'resumes/:id',
        name: 'TalentResumeDetail',
        component: () => import('@/views/resume/ResumeDetail.vue'),
        meta: { title: '简历详情', hidden: true },
      },
      {
        path: 'channels',
        name: 'TalentChannels',
        component: () => import('@/views/channel/ChannelList.vue'),
        meta: { title: '渠道与账号', permission: 'talent:channel', icon: 'Share' },
      },
      {
        path: 'channel-staging',
        name: 'TalentChannelStaging',
        component: () => import('@/views/channel/ChannelStaging.vue'),
        meta: { title: '渠道暂存库', permission: 'talent:channel', icon: 'Box' },
      },
      {
        path: 'communication-profile',
        name: 'TalentCommunicationProfile',
        component: () => import('@/views/communication/CommunicationProfile.vue'),
        meta: { title: '沟通 Profile', permission: 'talent:template', icon: 'ChatDotRound' },
      },
      {
        path: 'channels/agents',
        name: 'TalentAgents',
        component: () => import('@/views/agent/AgentList.vue'),
        meta: { title: '自动招聘任务', permission: 'talent:channel:agent', icon: 'Connection' },
      },
      {
        path: 'channels/workflows',
        name: 'TalentWorkflows',
        component: () => import('@/views/agent/AgentWorkflow.vue'),
        meta: { title: '工作流', permission: 'talent:channel:agent', icon: 'MagicStick' },
      },
      {
        path: 'channels/accounts',
        redirect: to => ({ path: '/talent/channels', query: to.query }),
      },
      {
        path: 'channels/logs',
        name: 'TalentLogs',
        component: () => import('@/views/agent/AgentLog.vue'),
        meta: { title: '运行日志', permission: 'talent:channel:agent', icon: 'Notebook' },
      },
      {
        path: 'referral',
        name: 'TalentReferral',
        component: () => import('@/views/referral/ReferralList.vue'),
        meta: { title: '内推', permission: 'talent:referral', icon: 'Share' },
      },
      {
        path: 'referral/rewards',
        name: 'TalentReferralRewards',
        component: () => import('@/views/referral/ReferralReward.vue'),
        meta: { title: '内推奖励', permission: 'talent:referral', icon: 'Present' },
      },
      {
        path: 'headhunters',
        name: 'TalentHeadhunters',
        component: () => import('@/views/headhunter/HeadhunterVendor.vue'),
        meta: { title: '猎头', permission: 'talent:headhunter', icon: 'OfficeBuilding' },
      },
      {
        path: 'headhunters/performance',
        name: 'TalentHeadhunterPerf',
        component: () => import('@/views/headhunter/HeadhunterPerformance.vue'),
        meta: { title: '猎头效果', permission: 'talent:headhunter', icon: 'DataLine' },
      },
      {
        path: 'templates',
        name: 'TalentTemplates',
        component: () => import('@/views/communication/CommunicationTemplate.vue'),
        meta: { title: '话术模板', permission: 'talent:template', icon: 'ChatLineRound' },
      },
      {
        path: 'conversations',
        name: 'TalentConversations',
        component: () => import('@/views/communication/CommunicationConversation.vue'),
        meta: { title: '对话存档', permission: 'talent:template', icon: 'ChatSquare' },
      },
    ],
  },
]

export default talentRoutes
