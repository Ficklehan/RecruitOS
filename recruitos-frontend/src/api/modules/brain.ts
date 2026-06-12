import request from '../request'

// ===== Types =====

export interface BrainDashboard {
  summary: {
    activeJobs: number; pendingDecisions: number; healthScore: number
    todayInterviews: number; criticalJobs: number; offerAcceptRate: number; avgCycleDays: number
  }
  urgentItems: UrgentItem[]
  insights: AIInsight[]
  healthScores?: { pipelineScore: number; interviewScore: number; offerEfficiency: number; activeJobsAbnormal: number }
}

export interface UrgentItem {
  id: string; type: string; title: string; description: string
  candidateName?: string; jobTitle?: string; severity: string; action: string; actionPath: string; aiReasoning: string
  confidence?: number
}

export interface AIInsight {
  id: string; category: string; title: string; description: string
  suggestedAction: string; actionPath?: string; confidence: number; trend?: string; data?: Record<string, any>
}

export interface WeeklyHealthReport {
  periodStart?: string; periodEnd?: string
  overallScore: number; scoreChange: number
  metrics?: { name: string; value: number; trend: number; unit: string }[]
  highlights: string[]
  concerns: string[]
}

// 触点1：需求诊断
export interface TeamGapAnalysis {
  businessObjective: string
  currentTeam: TeamMemberBrief[]
  capabilityGaps: CapabilityGap[]
  recommendation: HireRecommendation
  confidence: number; riskWarnings?: string[]
}

export interface TeamMemberBrief { name: string; level: string; skills: string[]; hasGapSkill: boolean }

export interface CapabilityGap { skill: string; required: string; current: string; coverage: number; severity: string }

export interface HireRecommendation {
  suggestedTitle: string; suggestedLevel: string; mustHaveSkills: string[]
  niceToHaveSkills: string[]; reasoning: string; budgetRange: string; suggestedChannels: string[]
  interviewDimensions: { name: string; weight: number; focus: string }[]
}

// 触点2：面试辅助
export interface InterviewPrep {
  interviewId: number; candidateName: string; jobTitle: string
  evaluatorDimension: string; dimensionWeight: number
  resumeSignals: { type: string; content: string; detail: string }[]
  suggestedQuestions: string[]; cautions: string[]
  scoreAnchors: { score: number; description: string }[]
  biasReminders?: string[]; dimensionCoverage?: { dimension: string; covered: boolean; questionsAsked: number }[]
}

// 触点3：校准会
export interface CalibrationSession {
  jobId?: number; jobTitle: string; candidateId: number; candidateName: string
  comparisons: EvaluatorComparison[]
  dimensions: { name: string; weight: number; avgScore: number; maxGap: number; disputed: boolean; aiRecommendation: string }[]
  biasDetections: { evaluatorName: string; biasType: string; description: string; confidence: number }[]
  silentDimensions: string[]; moderatorScript: string
  hireRecommendation: string; consensusScore: number; confidence: number
}

export interface EvaluatorComparison {
  evaluatorName: string
  scores: { dimension: string; score: number; evidence: string; signalStrength: string }[]
  overallVerdict: string
}

// 触点4：意向预测
export interface CandidateIntent {
  candidateId: number; candidateName: string; jobId: number; jobTitle: string
  intentScore: number; intentLevel: string; confidence: number
  riskFactors: { factor: string; level: string; detail: string }[]
  interventionSuggestions: string[]; updatedAt: string
}

// 触点5：周期预测
export interface CyclePrediction {
  jobId: number; jobTitle: string; estimatedDays: number; minDays: number; maxDays: number
  riskLevel: string
  bottlenecks: { stage: string; issue: string; impact: number }[]
  interventions: { action: string; expectedEffect: string; effortDays: number }[]
  confidence: number
}

// 触点6：Offer策略
export interface OfferStrategy {
  candidateId: number; candidateName: string; jobId: number; jobTitle: string; jobLevel: string
  suggestedRange: { min: number; mid: number; max: number; currency: string }
  components: { type: string; amount: number; note: string }[]
  negotiationTips: string[]
  risks: { risk: string; severity: string }[]
  competingScenarios: { scenario: string; competitor: string; theirOffer: string; ourSuggested: string; winProbability: string; recommendation: string; riskLevel: string }[]
  strategyOptions: { name: string; totalComp: string; baseSalary: string; bonusPercent: string; signingBonus: string; options: string; pros: string; cons: string; targetCandidate: string }[]
  strategySummary: string; confidence: number
}

// 触点7：面试官质量
export interface InterviewerQuality {
  interviewerId: number; interviewerName: string
  qualityScore: number; qualityLevel: string; totalEvaluations: number
  leniencyIndex: number; avgScore: number; globalAvgScore: number
  biasTags: { tag: string; description: string; severity: number }[]
  trend: { period: string; avgScore: number; leniency: number }[]
  predictionAccuracy: number; needsRecertification: boolean; coachingSuggestions: string[]
}

// 触点8：人才密度
export interface TalentDensity {
  orgId: number; orgName: string; densityScore: number; densityLevel: string
  heatmap: { capability: string; currentLevel: number; targetLevel: number; status: string }[]
  increments: { capability: string; contribution: number; newHireId: string }[]
  attritionImpacts: { capability: string; impact: number; critical: boolean }[]
  barRaiserVerdict: string; confidence: number
}



// AI Value Dashboard
export interface AiValueData {
  touchpoints: { name: string; totalUses: number; adoptionRate: number; trend: string }[]
  overall: {
    totalAiDecisions: number; humanAdoptionRate: number; avgTimeSavedMin: number
    offerAcceptLift: number; cycleReductionDays: number; calibrationKappaAvg: number
  }
  trend: { week: string; adoptionRate: number; offerAcceptLift: number }[]
}

// ===== API =====

export type ApiResult<T> = { code: number; data: T; msg: string }

export function getBrainDashboard() { return request.get('/api/brain/dashboard') as Promise<ApiResult<BrainDashboard>> }

// 触点1
export function analyzeTeamGap(params: { businessObjective: string; departmentId?: number }) {
  return request.post('/api/brain/analyze-gap', params) as Promise<ApiResult<TeamGapAnalysis>>
}

// 触点2
export function getInterviewPrep(interviewId: number) {
  return request.get(`/api/brain/interview-prep/${interviewId}`) as Promise<ApiResult<InterviewPrep>>
}

// 触点3
export function runCalibration(candidateId: number, evaluatorScores: Record<string, Record<string, any>>) {
  return request.post(`/api/brain/calibration/${candidateId}`, evaluatorScores) as Promise<ApiResult<CalibrationSession>>
}

// 触点4
export function getCandidateIntent(candidateId: number, jobId?: number, candidateName?: string, jobTitle?: string) {
  const params = new URLSearchParams()
  if (jobId) params.set('jobId', String(jobId))
  if (candidateName) params.set('candidateName', candidateName)
  if (jobTitle) params.set('jobTitle', jobTitle)
  return request.get(`/api/brain/intent/${candidateId}?${params}`) as Promise<ApiResult<CandidateIntent>>
}

// 触点5
export function getCyclePrediction(jobId: number) {
  return request.get(`/api/brain/cycle-prediction/${jobId}`) as Promise<ApiResult<CyclePrediction>>
}

// 触点6
export function getOfferStrategy(candidateId: number, jobId?: number, name?: string, title?: string, level?: string) {
  const params = new URLSearchParams()
  if (jobId) params.set('jobId', String(jobId))
  if (name) params.set('candidateName', name)
  if (title) params.set('jobTitle', title)
  if (level) params.set('jobLevel', level)
  return request.get(`/api/brain/offer-strategy/${candidateId}?${params}`) as Promise<ApiResult<OfferStrategy>>
}

// 触点7
export function getInterviewerQuality(interviewerId: number, name?: string) {
  const params = name ? `?interviewerName=${encodeURIComponent(name)}` : ''
  return request.get(`/api/brain/interviewer-quality/${interviewerId}${params}`) as Promise<ApiResult<InterviewerQuality>>
}

// 触点8
export function getTalentDensity(orgId: number, orgName?: string) {
  const params = orgName ? `?orgName=${encodeURIComponent(orgName)}` : ''
  return request.get(`/api/brain/talent-density/${orgId}${params}`) as Promise<ApiResult<TalentDensity>>
}

export function submitScorecard(data: {
  interviewId: number; dimensions: { name: string; score: number; evidence: string; confidence: string }[]
  overallScore: number; decision: string
}) { return request.post('/api/brain/scorecard', data) as Promise<ApiResult<void>> }

export function getWeeklyReport() { return request.get('/api/brain/weekly-report') as Promise<ApiResult<WeeklyHealthReport>> }

export function askAI(question: string) {
  return request.post('/api/brain/ask', { question }) as Promise<ApiResult<{ answer: string; confidence: number }>>
}
// AI Value Dashboard
export function getAiValue() { return request.get('/api/brain/ai-value') as Promise<ApiResult<AiValueData>> }

// Ignore reason logging
export function logIgnoreReason(data: {
  touchpoint: string; targetId?: number; targetType?: string
  reason: string; note?: string; userId?: number
}) { return request.post('/api/brain/ignore-reason', data) as Promise<ApiResult<void>> }


// ================================================================
// v8: 认知层 API
// ================================================================

export interface MenuAiStatus {
  insightAlerts: number; insightAttention: number
  insightObserve: number; totalPending: number
}
export function getMenuAiStatus() {
  return request.get('/api/cognitive/menu-status') as Promise<ApiResult<MenuAiStatus>>
}

export interface ObservationItem {
  id: number; observationType: string; severity: string
  title: string; body: string; relatedObjects: string
  suggestedAction: string; actionTaken: string
  createdAt: string; expiresAt: string
}
export interface ObservationGroup {
  critical: ObservationItem[]; warnings: ObservationItem[]
  infos: ObservationItem[]; counts: Record<string, number>
}
export function getObservations() {
  return request.get('/api/cognitive/observations') as Promise<ApiResult<ObservationGroup>>
}
export function actionObservation(id: number, action: string) {
  return request.post(`/api/cognitive/observations/${id}/action`, { action }) as Promise<ApiResult<{success: boolean}>>
}
export function feedbackObservation(id: number, feedback: string) {
  return request.post(`/api/cognitive/observations/${id}/feedback`, { feedback }) as Promise<ApiResult<{success: boolean}>>
}

export interface CognitiveJudgment {
  id: number; judgmentType: string; judgmentText: string
  judgmentJson: string; confidence: number; contradiction: string; createdAt: string
}
export function getJudgment(subjectType: string, subjectId: number) {
  return request.get(`/api/cognitive/judgments/${subjectType}/${subjectId}`) as Promise<ApiResult<CognitiveJudgment>>
}
export function judgePipeline(jobId: number, pipelineData: Record<string, any>) {
  return request.post(`/api/cognitive/judge/pipeline/${jobId}`, pipelineData) as Promise<ApiResult<Record<string, any>>>
}

export interface UserCognitiveModel {
  decisionSpeed: number; riskTolerance: number; standardRigidity: number
  scoringBiasJson: string; leniencyIndex: number; blindSpotsJson: string
  totalDecisions: number; patternStability: number
}
export function getMyCognitiveModel() {
  return request.get('/api/cognitive/my-model') as Promise<ApiResult<UserCognitiveModel>>
}

export function getTimeline(subjectType: string, subjectId: number) {
  return request.get(`/api/cognitive/timeline/${subjectType}/${subjectId}`) as Promise<ApiResult<any[]>>
}
export function getLessons() {
  return request.get('/api/cognitive/lessons') as Promise<ApiResult<any[]>>
}
export function getPatterns(patternType: string) {
  return request.get(`/api/cognitive/patterns/${patternType}`) as Promise<ApiResult<any[]>>
}
