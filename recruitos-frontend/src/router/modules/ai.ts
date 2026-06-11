import type { RouteRecordRaw } from 'vue-router'

const AppLayout = () => import('@/components/Layout/AppLayout.vue')

const aiRoutes: RouteRecordRaw[] = [
  {
    path: '/ai',
    component: AppLayout,
    meta: { title: 'AI 助手', icon: 'Sparkles' },
    children: [
      {
        path: '',
        name: 'AIHome',
        component: () => import('@/views/ai/AssistantHome.vue'),
        meta: { title: 'AI 助手', icon: 'Sparkles' },
      },
      {
        path: 'demand/create',
        name: 'AIDemandCreate',
        component: () => import('@/views/ai/DemandCreateAI.vue'),
        meta: { title: 'AI 需求诊断', icon: 'Search' },
      },
      {
        path: 'interview/:interviewId/scorecard',
        name: 'AIScorecard',
        component: () => import('@/views/ai/InterviewScorecard.vue'),
        meta: { title: 'AI 面试评价' },
      },
      {
        path: 'calibration/:candidateId',
        name: 'AICalibration',
        component: () => import('@/views/ai/CalibrationSession.vue'),
        meta: { title: 'AI 校准会', icon: 'Scale' },
      },
      {
        path: 'intent/:candidateId',
        name: 'AIIntent',
        component: () => import('@/views/ai/CandidateIntentPage.vue'),
        meta: { title: 'AI 意向预测', icon: 'TrendingUp' },
      },
      {
        path: 'cycle-prediction/:jobId',
        name: 'AICycle',
        component: () => import('@/views/ai/CyclePredictionPage.vue'),
        meta: { title: '招聘周期预测', icon: 'Clock' },
      },
      {
        path: 'offer-strategy/:candidateId',
        name: 'AIOfferStrategy',
        component: () => import('@/views/ai/OfferStrategyPage.vue'),
        meta: { title: 'AI Offer 策略', icon: 'DollarSign' },
      },
      {
        path: 'interviewer-quality/:interviewerId',
        name: 'AIInterviewerQuality',
        component: () => import('@/views/ai/InterviewerQualityPage.vue'),
        meta: { title: '面试官质量', icon: 'UserCheck' },
      },
      {
        path: 'talent-density/:orgId',
        name: 'AITalentDensity',
        component: () => import('@/views/ai/TalentDensityPage.vue'),
        meta: { title: '人才密度评估', icon: 'BarChart3' },
      },
      {
        path: 'diagnose',
        name: 'AIDiagnose',
        component: () => import('@/views/ai/WeeklyReport.vue'),
        meta: { title: '诊断中心', icon: 'Activity' },
      },
      {
        path: 'report',
        name: 'AIReport',
        component: () => import('@/views/ai/WeeklyReport.vue'),
        meta: { title: '周报', icon: 'FileText' },
      },
    ],
  },
]

export default aiRoutes
