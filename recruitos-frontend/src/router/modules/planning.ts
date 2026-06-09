import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const planningRoutes: RouteRecordRaw[] = [
  {
    path: '/planning',
    component: AppLayout,
    redirect: '/planning/demands',
    meta: { title: '招聘规划', permission: 'planning' },
    children: [
      {
        path: 'demands',
        name: 'PlanningDemands',
        component: () => import('@/views/demand/DemandList.vue'),
        meta: { title: '招聘需求', permission: 'planning:demand', icon: 'List' },
      },
      {
        path: 'demands/board',
        name: 'PlanningDemandBoard',
        component: () => import('@/views/demand/DemandBoard.vue'),
        meta: { title: '招聘需求看板', permission: 'planning:demand:board', icon: 'Grid' },
      },
      {
        path: 'demands/create',
        name: 'PlanningDemandCreate',
        component: () => import('@/views/demand/DemandCreate.vue'),
        meta: { title: '创建需求', hidden: true },
      },
      {
        path: 'demands/:id',
        name: 'PlanningDemandDetail',
        component: () => import('@/views/demand/DemandDetail.vue'),
        meta: { title: '需求详情', hidden: true },
      },
      {
        path: 'approvals/pending',
        name: 'PlanningApprovalsPending',
        component: () => import('@/views/demand/DemandApproval.vue'),
        meta: { title: '审批', permission: 'planning:approval', icon: 'Checked' },
      },
      {
        path: 'jobs',
        name: 'PlanningJobs',
        component: () => import('@/views/job/JobList.vue'),
        meta: { title: '在招职位', permission: 'planning:job', icon: 'Briefcase' },
      },
      {
        path: 'jobs/create',
        name: 'PlanningJobCreate',
        component: () => import('@/views/job/JdEditor.vue'),
        meta: { title: '创建在招职位', hidden: true },
      },
      {
        path: 'jobs/:id',
        name: 'PlanningJobDetail',
        component: () => import('@/views/planning/JobDetail.vue'),
        meta: { title: '在招职位工作台', hidden: true },
      },
      {
        path: 'jobs/:id/jd',
        name: 'PlanningJobJd',
        component: () => import('@/views/job/JdEditor.vue'),
        meta: { title: '任职要求', hidden: true },
      },
      {
        path: 'evolution/proposals',
        name: 'PlanningEvolutionProposals',
        component: () => import('@/views/evolution/EvolutionProposal.vue'),
        meta: { title: '招人方式建议', permission: 'planning:job', icon: 'Bell' },
      },
    ],
  },
]

export default planningRoutes
