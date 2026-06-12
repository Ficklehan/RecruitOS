import type { RouteRecordRaw } from 'vue-router'

const PortalLayout = () => import('@/components/portal/CandidatePortalLayout.vue')

const portalRoutes: RouteRecordRaw[] = [
  {
    path: '/portal',
    component: PortalLayout,
    redirect: '/portal/overview',
    meta: { title: '候选人中心', hidden: true },
    children: [
      {
        path: 'overview',
        name: 'PortalOverview',
        component: () => import('@/views/portal/CandidatePortal.vue'),
        meta: { title: '我的进度' },
      },
      {
        path: 'interviews',
        name: 'PortalInterviews',
        component: () => import('@/views/portal/CandidatePortal.vue'),
        meta: { title: '面试' },
      },
      {
        path: 'offer',
        name: 'PortalOffer',
        component: () => import('@/views/portal/CandidatePortal.vue'),
        meta: { title: '录用' },
      },
      {
        path: 'onboard',
        name: 'PortalOnboard',
        component: () => import('@/views/portal/CandidatePortal.vue'),
        meta: { title: '入职' },
      },
    ],
  },
]

export default portalRoutes
