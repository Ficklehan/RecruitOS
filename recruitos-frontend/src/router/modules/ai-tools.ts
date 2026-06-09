import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const aiToolsRoutes: RouteRecordRaw[] = [
  {
    path: '/ai-tools',
    component: AppLayout,
    redirect: '/ai-tools/template',
    meta: { title: 'AI工具', icon: 'ChatDotRound' },
    children: [
      {
        path: 'workflow',
        name: 'AiToolsWorkflow',
        component: () => import('@/views/agent/AgentWorkflow.vue'),
        meta: { title: 'AI工作流', icon: 'MagicStick' },
      },
      {
        path: 'workflow/:id',
        name: 'AiToolsWorkflowDetail',
        component: () => import('@/views/agent/AgentWorkflow.vue'),
        meta: { title: '工作流详情', hidden: true },
      },
      {
        path: 'template',
        name: 'AiToolsTemplate',
        component: () => import('@/views/communication/CommunicationTemplate.vue'),
        meta: { title: '话术管理', icon: 'Document' },
      },
      {
        path: 'profile',
        name: 'AiToolsCommunicationProfile',
        component: () => import('@/views/communication/CommunicationProfile.vue'),
        meta: { title: '沟通 Profile', icon: 'User' },
      },
      {
        path: 'conversation',
        name: 'AiToolsConversation',
        component: () => import('@/views/communication/CommunicationConversation.vue'),
        meta: { title: '沟通记录', icon: 'ChatLineRound' },
      },
      {
        path: 'safety',
        name: 'AiToolsSafety',
        component: () => import('@/views/communication/CommunicationSafety.vue'),
        meta: { title: '安全审查', icon: 'Shield' },
      },
      {
        path: 'evolution/weight',
        name: 'AiToolsEvolutionWeight',
        component: () => import('@/views/evolution/EvolutionWeight.vue'),
        meta: { title: '权重面板', icon: 'Histogram' },
      },
      {
        path: 'evolution/health',
        name: 'AiToolsEvolutionHealth',
        component: () => import('@/views/evolution/EvolutionHealth.vue'),
        meta: { title: '健康监控', icon: 'Monitor' },
      },
      {
        path: 'evolution/ab-test',
        name: 'AiToolsEvolutionAbTest',
        component: () => import('@/views/evolution/EvolutionAbTest.vue'),
        meta: { title: 'A/B测试', icon: 'Switch' },
      },
      {
        path: 'evolution/proposals',
        name: 'AiToolsEvolutionProposals',
        component: () => import('@/views/evolution/EvolutionProposal.vue'),
        meta: { title: '策略进化待确认', icon: 'Bell' },
      },
    ],
  },
]

export default aiToolsRoutes
