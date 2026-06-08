import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const screeningRoutes: RouteRecordRaw[] = [
  {
    path: '/screening',
    component: AppLayout,
    redirect: '/pipeline/decision',
    meta: { title: '筛选评估', permission: 'pipeline', hidden: true },
    children: [
      {
        path: 'decision',
        name: 'ScreeningDecision',
        component: () => import('@/views/candidate/CandidateDecision.vue'),
        meta: { title: '候选人匹配评估', permission: 'pipeline:candidate', icon: 'DataAnalysis' },
      },
    ],
  },
]

export default screeningRoutes
