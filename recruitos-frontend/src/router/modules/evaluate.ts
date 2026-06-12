import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const evaluateRoutes: RouteRecordRaw[] = [
  {
    path: '/evaluate',
    component: AppLayout,
    redirect: '/evaluate/prep',
    meta: { title: '面试评估', permission: 'pipeline' },
    children: [
      {
        path: 'prep',
        name: 'EvaluatePrep',
        component: () => import('@/views/evaluate/EvaluatePrep.vue'),
        meta: { title: '面试准备', permission: 'pipeline:candidate', icon: 'ClipboardList' },
      },
      {
        path: 'scorecard',
        name: 'EvaluateScorecard',
        component: () => import('@/views/ai/InterviewScorecard.vue'),
        meta: { title: '面试评估', permission: 'pipeline:candidate', icon: 'PenTool' },
      },
      {
        path: 'interviewer',
        name: 'EvaluateInterviewer',
        component: () => import('@/views/ai/InterviewerQualityPage.vue'),
        meta: { title: '面试官质量', permission: 'insight:interviewer', icon: 'TrendingUp' },
      },
    ],
  },
]

export default evaluateRoutes
