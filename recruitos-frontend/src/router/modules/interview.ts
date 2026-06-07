import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const interviewRoutes: RouteRecordRaw[] = [
  {
    path: '/interview',
    component: AppLayout,
    redirect: '/interview/board',
    meta: { title: '面试管理', icon: 'Calendar' },
    children: [
      {
        path: 'board',
        name: 'InterviewBoard',
        component: () => import('@/views/interview/InterviewBoard.vue'),
        meta: { title: '面试看板', icon: 'Grid' },
      },
      {
        path: 'calendar',
        name: 'InterviewCalendar',
        component: () => import('@/views/interview/InterviewCalendar.vue'),
        meta: { title: '面试日历', icon: 'Calendar' },
      },
      {
        path: 'evaluation',
        name: 'InterviewEvaluation',
        component: () => import('@/views/interview/InterviewEvaluation.vue'),
        meta: { title: '评价管理', icon: 'Star' },
      },
    ],
  },
]

export default interviewRoutes
