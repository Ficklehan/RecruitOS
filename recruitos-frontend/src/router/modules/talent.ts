import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const talentRoutes: RouteRecordRaw[] = [
  {
    path: '/talent',
    component: AppLayout,
    redirect: '/talent/candidate',
    meta: { title: '人才获取', icon: 'User' },
    children: [
      {
        path: 'resume/list',
        name: 'TalentResumeList',
        component: () => import('@/views/resume/ResumeList.vue'),
        meta: { title: '简历管理', icon: 'Document' },
      },
      {
        path: 'resume/upload',
        name: 'TalentResumeUpload',
        component: () => import('@/views/resume/ResumeUpload.vue'),
        meta: { title: '上传简历', hidden: true },
      },
      {
        path: 'resume/detail/:id',
        name: 'TalentResumeDetail',
        component: () => import('@/views/resume/ResumeDetail.vue'),
        meta: { title: '简历详情', hidden: true },
      },
      {
        path: 'candidate',
        name: 'TalentCandidate',
        component: () => import('@/views/candidate/CandidateList.vue'),
        meta: { title: '候选人列表', icon: 'List' },
      },
      {
        path: 'pool',
        name: 'TalentPool',
        component: () => import('@/views/candidate/CandidatePool.vue'),
        meta: { title: '人才库', icon: 'UserFilled' },
      },
      {
        path: 'channel',
        name: 'TalentChannel',
        component: () => import('@/views/agent/AgentList.vue'),
        meta: { title: '招聘渠道', icon: 'Connection' },
      },
      {
        path: 'channel/account',
        name: 'TalentChannelAccount',
        component: () => import('@/views/agent/AgentAccount.vue'),
        meta: { title: '平台账号', icon: 'Monitor', hidden: true },
      },
      {
        path: 'channel/log',
        name: 'TalentChannelLog',
        component: () => import('@/views/agent/AgentLog.vue'),
        meta: { title: '行为日志', icon: 'Notebook', hidden: true },
      },
      {
        path: 'referral',
        name: 'TalentReferral',
        component: () => import('@/views/referral/ReferralList.vue'),
        meta: { title: '内推管理', icon: 'Share' },
      },
      {
        path: 'referral/reward',
        name: 'TalentReferralReward',
        component: () => import('@/views/referral/ReferralReward.vue'),
        meta: { title: '内推奖励', icon: 'Present', hidden: true },
      },
      {
        path: 'headhunter',
        name: 'TalentHeadhunter',
        component: () => import('@/views/headhunter/HeadhunterVendor.vue'),
        meta: { title: '猎头管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'headhunter/performance',
        name: 'TalentHeadhunterPerformance',
        component: () => import('@/views/headhunter/HeadhunterPerformance.vue'),
        meta: { title: '猎头效果', icon: 'DataLine', hidden: true },
      },
    ],
  },
]

export default talentRoutes
