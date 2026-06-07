import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const screeningRoutes: RouteRecordRaw[] = [
  {
    path: '/screening',
    component: AppLayout,
    redirect: '/screening/resume',
    meta: { title: '筛选评估', icon: 'Filter' },
    children: [
      {
        path: 'resume',
        name: 'ScreeningResume',
        component: () => import('@/views/candidate/CandidateList.vue'),
        meta: { title: '简历筛选', icon: 'Document' },
      },
      {
        path: 'business',
        name: 'ScreeningBusiness',
        component: () => import('@/views/candidate/CandidateList.vue'),
        meta: { title: '业务筛选', icon: 'UserFilled' },
      },
      {
        path: 'decision',
        name: 'ScreeningDecision',
        component: () => import('@/views/candidate/CandidateDecision.vue'),
        meta: { title: '匹配度分析', icon: 'DataAnalysis' },
      },
    ],
  },
]

export default screeningRoutes
