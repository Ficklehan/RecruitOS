import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const positionRoutes: RouteRecordRaw[] = [
  {
    path: '/position',
    component: AppLayout,
    redirect: '/position/demand',
    meta: { title: '职位需求', icon: 'Document' },
    children: [
      {
        path: 'demand',
        name: 'PositionDemand',
        component: () => import('@/views/demand/DemandList.vue'),
        meta: { title: '需求列表', icon: 'List' },
      },
      {
        path: 'demand/create',
        name: 'PositionDemandCreate',
        component: () => import('@/views/demand/DemandCreate.vue'),
        meta: { title: '创建需求', icon: 'Plus', hidden: true },
      },
      {
        path: 'demand/detail/:id',
        name: 'PositionDemandDetail',
        component: () => import('@/views/demand/DemandDetail.vue'),
        meta: { title: '需求详情', icon: 'Document', hidden: true },
      },
      {
        path: 'demand/board',
        name: 'PositionDemandBoard',
        component: () => import('@/views/demand/DemandBoard.vue'),
        meta: { title: '需求看板', icon: 'Grid' },
      },
      {
        path: 'demand/approval',
        name: 'PositionDemandApproval',
        component: () => import('@/views/demand/DemandApproval.vue'),
        meta: { title: '需求审批', icon: 'Checked' },
      },
      {
        path: 'job',
        name: 'PositionJob',
        component: () => import('@/views/job/JobList.vue'),
        meta: { title: '岗位列表', icon: 'Briefcase' },
      },
      {
        path: 'job/jd-editor',
        name: 'PositionJdEditor',
        component: () => import('@/views/job/JdEditor.vue'),
        meta: { title: 'JD工作台', icon: 'EditPen' },
      },
    ],
  },
]

export default positionRoutes
