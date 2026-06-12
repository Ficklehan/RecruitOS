import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const knowledgeRoutes: RouteRecordRaw[] = [
  {
    path: '/knowledge',
    component: AppLayout,
    redirect: '/knowledge/profiles',
    meta: { title: '知识', permission: 'insight' },
    children: [
      {
        path: 'profiles',
        name: 'KnowledgeProfiles',
        component: () => import('@/views/knowledge/KnowledgeProfiles.vue'),
        meta: { title: '成功画像', permission: 'insight:funnel', icon: 'Target' },
      },
      {
        path: 'decisions',
        name: 'KnowledgeDecisions',
        component: () => import('@/views/knowledge/DecisionLog.vue'),
        meta: { title: '决策记录', permission: 'insight:funnel', icon: 'History' },
      },
      {
        path: 'lessons',
        name: 'KnowledgeLessons',
        component: () => import('@/views/knowledge/LessonsPage.vue'),
        meta: { title: '教训库', permission: 'insight:funnel', icon: 'BookOpen' },
      },
      {
        path: 'my-patterns',
        name: 'KnowledgeMyPatterns',
        component: () => import('@/views/knowledge/MyPatterns.vue'),
        meta: { title: '我的模式', permission: 'insight:funnel', icon: 'UserCog' },
      },
    ],
  },
]

export default knowledgeRoutes
