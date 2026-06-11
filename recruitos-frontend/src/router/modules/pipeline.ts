import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const pipelineRoutes: RouteRecordRaw[] = [
  {
    path: '/pipeline',
    component: AppLayout,
    redirect: '/pipeline/board',
    meta: { title: '招聘执行', permission: 'pipeline' },
    children: [
      // -- 管道看板（找人 & 面试共用入口）--
      {
        path: 'board',
        name: 'PipelineBoard',
        component: () => import('@/views/pipeline/PipelineBoard.vue'),
        meta: { title: '招聘进展', permission: 'pipeline:board', icon: 'Grid' },
      },
      // -- 面试看板 --
      {
        path: 'interview-board',
        name: 'InterviewBoard',
        component: () => import('@/views/interview/InterviewBoard.vue'),
        meta: { title: '面试看板', permission: 'pipeline:board', icon: 'Grid' },
      },
      // -- 候选人 --
      {
        path: 'candidates',
        name: 'PipelineCandidates',
        component: () => import('@/views/candidate/CandidateList.vue'),
        meta: { title: '候选人列表', permission: 'pipeline:candidate', icon: 'User' },
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
      // -- 面试日历 --
      {
        path: 'calendar',
        name: 'PipelineCalendar',
        component: () => import('@/views/interview/InterviewCalendar.vue'),
        meta: { title: '面试日历', permission: 'pipeline:calendar', icon: 'Calendar' },
      },
      // -- 面试评价 --
      {
        path: 'evaluation',
        name: 'PipelineEvaluation',
        component: () => import('@/views/interview/InterviewEvaluation.vue'),
        meta: { title: '评价管理', permission: 'pipeline:evaluation', icon: 'Edit' },
      },
      // -- 候选人决策 --
      {
        path: 'decision',
        name: 'PipelineDecision',
        component: () => import('@/views/candidate/CandidateDecision.vue'),
        meta: { title: '候选人匹配评估', permission: 'pipeline:candidate', icon: 'DataAnalysis' },
      },
      // -- Offer --
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
      // -- 入职 --
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
