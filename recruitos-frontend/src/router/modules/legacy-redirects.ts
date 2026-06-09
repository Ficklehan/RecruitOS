import type { RouteRecordRaw } from 'vue-router'

const legacyRedirects: RouteRecordRaw[] = [
  { path: '/dashboard', redirect: '/workspace/dashboard' },
  { path: '/position/demand/create', redirect: '/planning/demands/create' },
  { path: '/position/demand/detail/:id', redirect: to => `/planning/demands/${to.params.id}` },
  { path: '/position/demand/board', redirect: '/planning/demands/board' },
  { path: '/position/demand/approval', redirect: '/planning/approvals/pending' },
  { path: '/position/demand', redirect: '/planning/demands' },
  { path: '/position/job/jd-editor', redirect: to => `/planning/jobs/${to.query.id || ''}/jd` },
  { path: '/position/job', redirect: '/planning/jobs' },
  { path: '/talent/candidate', redirect: '/pipeline/candidates' },
  { path: '/talent/resume/:pathMatch(.*)*', redirect: to => {
    const pm = to.params.pathMatch
    const path = Array.isArray(pm) ? pm.join('/') : String(pm || '')
    if (path.startsWith('detail/')) return `/talent/resumes/${path.slice('detail/'.length)}`
    if (path === 'list') return '/talent/resumes'
    return `/talent/resumes/${path}`
  }},
  { path: '/talent/channel/:pathMatch(.*)*', redirect: to => `/talent/channels/${to.params.pathMatch}` },
  { path: '/candidate/decision', redirect: to => ({ path: '/pipeline/decision', query: to.query }) },
  { path: '/screening/decision', redirect: to => ({ path: '/pipeline/decision', query: to.query }) },
  { path: '/interview/:pathMatch(.*)*', redirect: to => (to.path.includes('calendar') ? '/pipeline/calendar' : '/pipeline/board') },
  { path: '/hiring/:pathMatch(.*)*', redirect: to => (to.path.includes('approval') ? '/pipeline/offers/approvals' : '/pipeline/offers') },
  { path: '/onboard/:pathMatch(.*)*', redirect: to => (to.path.includes('task') ? '/pipeline/onboards/tasks' : '/pipeline/onboards') },
  { path: '/ai-tools/:pathMatch(.*)*', redirect: to => {
    const p = String(to.params.pathMatch || '')
    if (p.startsWith('evolution/proposals')) return '/planning/evolution/proposals'
    if (p.startsWith('evolution/health')) return '/settings/compliance/safety'
    if (p.startsWith('evolution')) return '/planning/evolution/proposals'
    if (p.startsWith('profile')) return '/talent/communication-profile'
    if (p.startsWith('workflow')) return '/talent/channels/workflows'
    if (p.startsWith('conversation')) return '/talent/conversations'
    if (p.startsWith('safety')) return '/settings/compliance/safety'
    return '/talent/templates'
  }},
  { path: '/agent/:pathMatch(.*)*', redirect: to => `/talent/channels/${to.params.pathMatch}` },
  { path: '/evolution/proposals', redirect: '/planning/evolution/proposals' },
  { path: '/evolution/:pathMatch(.*)*', redirect: '/planning/evolution/proposals' },
  { path: '/communication/:pathMatch(.*)*', redirect: to => `/talent/${to.path.includes('template') ? 'templates' : 'conversations'}` },
  { path: '/analytics/:pathMatch(.*)*', redirect: to => `/insight/${to.params.pathMatch}` },
  { path: '/referral/:pathMatch(.*)*', redirect: to => `/talent/referral${to.path.includes('reward') ? '/rewards' : ''}` },
  { path: '/headhunter/:pathMatch(.*)*', redirect: to => `/talent/headhunters${to.path.includes('performance') ? '/performance' : ''}` },
  { path: '/demand/:pathMatch(.*)*', redirect: to => `/planning/demands/${to.params.pathMatch}` },
  { path: '/job/:pathMatch(.*)*', redirect: to => `/planning/jobs/${to.params.pathMatch}` },
]

export default legacyRedirects
