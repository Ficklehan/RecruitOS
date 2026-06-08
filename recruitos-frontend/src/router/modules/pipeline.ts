import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const pipelineRoutes: RouteRecordRaw[] = [
  {
    path: '/pipeline',
    component: AppLayout,
    redirect: '/pipeline/board',
    meta: { title: '招聘执行', permission: 'pipeline' },
    children: [
      {
        path: 'board',
        name: 'PipelineBoard',
        component: () => import('@/views/pipeline/PipelineBoard.vue'),
        meta: { title: '招聘进展', permission: 'pipeline:board', icon: 'Grid' },
      },
      {
        path: 'decision',
        name: 'PipelineDecision',
        component: () => import('@/views/candidate/CandidateDecision.vue'),
        meta: { title: '候选人匹配评估', permission: 'pipeline:candidate', icon: 'DataAnalysis' },
      },
      {
        path: 'candidates',
        name: 'PipelineCandidates',
        component: () => import('@/views/candidate/CandidateList.vue'),
        meta: { title: '候选人', permission: 'pipeline:candidate', icon: 'User' },
      },
      {
        path: 'candidates/:id',
        name: 'PipelineCandidateDetail',
        component: () => import('@/views/candidate/CandidateWorkspace.vue'),
        meta: { title: '候选人工作台', hidden: true },
      },
      {
        path: 'workspace/:candidateId',
        name: 'CandidateWorkspace',
        component: () => import('@/views/candidate/CandidateWorkspace.vue'),
        meta: { title: '候选人工作台', hidden: true },
      },
      {
        path: 'calendar',
        name: 'PipelineCalendar',
        component: () => import('@/views/interview/InterviewCalendar.vue'),
        meta: { title: '面试日历', permission: 'pipeline:calendar', icon: 'Calendar' },
      },
      {
        path: 'offers',
        name: 'PipelineOffers',
        component: () => import('@/views/offer/OfferList.vue'),
        meta: { title: '录用通知', permission: 'pipeline:offer', icon: 'Tickets' },
      },
      {
        path: 'offers/:id',
        name: 'PipelineOfferDetail',
        component: () => import('@/views/pipeline/OfferDetail.vue'),
        meta: { title: 'Offer详情', hidden: true },
      },
      {
        path: 'offers/approvals',
        name: 'PipelineOfferApprovals',
        component: () => import('@/views/offer/OfferApproval.vue'),
        meta: { title: 'Offer审批', permission: 'pipeline:offer', icon: 'Checked' },
      },
      {
        path: 'onboards',
        name: 'PipelineOnboards',
        component: () => import('@/views/onboard/OnboardList.vue'),
        meta: { title: '入职', permission: 'pipeline:onboard', icon: 'Finished' },
      },
      {
        path: 'onboards/tasks',
        name: 'PipelineOnboardTasks',
        component: () => import('@/views/onboard/OnboardTask.vue'),
        meta: { title: '入职任务', permission: 'pipeline:onboard', icon: 'List' },
      },
    ],
  },
]

export default pipelineRoutes
