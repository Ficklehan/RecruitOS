import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const hiringRoutes: RouteRecordRaw[] = [
  {
    path: '/hiring',
    component: AppLayout,
    redirect: '/hiring/offer',
    meta: { title: '录用管理', icon: 'Tickets' },
    children: [
      {
        path: 'salary',
        name: 'HiringSalary',
        component: () => import('@/views/offer/OfferList.vue'),
        meta: { title: '定薪审批', icon: 'Coin' },
      },
      {
        path: 'offer',
        name: 'HiringOffer',
        component: () => import('@/views/offer/OfferList.vue'),
        meta: { title: 'Offer管理', icon: 'List' },
      },
      {
        path: 'offer/approval',
        name: 'HiringOfferApproval',
        component: () => import('@/views/offer/OfferApproval.vue'),
        meta: { title: 'Offer审批', icon: 'Checked' },
      },
      {
        path: 'background',
        name: 'HiringBackground',
        component: () => import('@/views/offer/OfferList.vue'),
        meta: { title: '背景调查', icon: 'Shield' },
      },
    ],
  },
]

export default hiringRoutes
