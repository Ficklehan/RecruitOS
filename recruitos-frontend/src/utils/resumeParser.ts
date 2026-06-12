import { educationLabel } from '@/constants/businessLabels'

export interface WorkExperienceItem {
  company: string
  position: string
  startDate?: string
  endDate?: string
  description?: string
}

export interface ProjectExperienceItem {
  name: string
  role?: string
  startDate?: string
  endDate?: string
  description?: string
}

export interface EducationItem {
  school: string
  degree?: string
  major?: string
  period?: string
}

export interface NormalizedResume {
  id?: number
  fileName?: string
  fileUrl?: string
  fileType?: string
  parseStatus?: string
  rawText?: string
  summary?: string
  name: string
  phone?: string
  email?: string
  company?: string
  position?: string
  workYears?: number | string
  education?: string
  school?: string
  major?: string
  expectedSalary?: string
  source?: string
  skills: string[]
  workExperience: WorkExperienceItem[]
  projectExperience: ProjectExperienceItem[]
  educationHistory: EducationItem[]
}

function parseJson<T>(raw: unknown): T | null {
  if (!raw) return null
  if (typeof raw === 'object') return raw as T
  if (typeof raw !== 'string') return null
  try {
    return JSON.parse(raw) as T
  } catch {
    return null
  }
}

function asString(v: unknown): string {
  return v == null ? '' : String(v)
}

function parseSkills(raw: unknown, candidateTags?: unknown): string[] {
  const fromParsed = Array.isArray(raw) ? raw.map(asString).filter(Boolean) : []
  if (fromParsed.length) return fromParsed
  const tagParsed = parseJson<unknown[]>(candidateTags)
  if (Array.isArray(tagParsed)) return tagParsed.map(asString).filter(Boolean)
  if (typeof candidateTags === 'string' && candidateTags.includes(',')) {
    return candidateTags.split(/[,，、]/).map(s => s.trim()).filter(Boolean)
  }
  return []
}

function estimateStartDate(workYears?: number | string): string {
  const years = Number(workYears)
  if (!years || Number.isNaN(years)) return ''
  const y = new Date().getFullYear() - years
  return `${y}-01`
}

function normalizeWorkList(raw: unknown, basic: Record<string, unknown>, summary?: string): WorkExperienceItem[] {
  const list = Array.isArray(raw) ? raw : []
  const mapped = list.map((item: any) => ({
    company: asString(item.company || item.employer),
    position: asString(item.position || item.title || item.role),
    startDate: asString(item.startDate || item.start || item.from),
    endDate: asString(item.endDate || item.end || item.to) || '至今',
    description: asString(item.description || item.duty || item.content),
  })).filter(i => i.company || i.position)

  if (mapped.length) return mapped

  const company = asString(basic.company)
  const position = asString(basic.position)
  if (!company && !position) return []

  return [{
    company: company || '—',
    position: position || '—',
    startDate: estimateStartDate(basic.workYears as number | string),
    endDate: '至今',
    description: summary || '',
  }]
}

function normalizeProjectList(raw: unknown): ProjectExperienceItem[] {
  if (!Array.isArray(raw)) return []
  return raw.map((item: any) => ({
    name: asString(item.name || item.project),
    role: asString(item.role || item.position),
    startDate: asString(item.startDate || item.start),
    endDate: asString(item.endDate || item.end) || '至今',
    description: asString(item.description || item.summary),
  })).filter(i => i.name)
}

function normalizeEducationList(
  raw: unknown,
  basic: Record<string, unknown>,
  candidate?: Record<string, unknown>,
): EducationItem[] {
  const list = Array.isArray(raw) ? raw : []
  const mapped = list.map((item: any) => ({
    school: asString(item.school || item.university),
    degree: educationLabel(asString(item.degree || item.education)),
    major: asString(item.major),
    period: asString(item.period || item.startDate || item.year),
  })).filter(i => i.school)

  if (mapped.length) return mapped

  const school = asString(candidate?.school || basic.school)
  const edu = asString(candidate?.education || basic.education)
  const major = asString(candidate?.major || basic.major)
  if (!school && !edu) return []

  return [{
    school: school || '—',
    degree: educationLabel(edu) || edu,
    major: major || '—',
    period: '',
  }]
}

/** 将简历 API 数据 + 候选人信息合并为结构化简历视图 */
export function normalizeResumeData(
  apiData?: Record<string, unknown> | null,
  candidate?: Record<string, unknown> | null,
): NormalizedResume {
  const parsed = parseJson<Record<string, unknown>>(apiData?.parsedJson) || {}
  const basic = (parsed.basic as Record<string, unknown>) || {}

  const name = asString(
    apiData?.name || candidate?.name || basic.name || '未识别',
  )
  const summary = asString(parsed.summary || apiData?.summary)

  const workExperience = normalizeWorkList(
    parsed.workExperience || parsed.work_experience || parsed.experiences,
    basic,
    summary,
  )

  const educationHistory = normalizeEducationList(
    parsed.educationHistory || parsed.education || parsed.educations,
    basic,
    candidate || undefined,
  )

  return {
    id: apiData?.id as number | undefined,
    fileName: asString(apiData?.fileName),
    fileUrl: asString(apiData?.fileUrl),
    fileType: asString(apiData?.fileType || 'pdf'),
    parseStatus: asString(apiData?.parseStatus),
    rawText: asString(apiData?.rawText),
    summary,
    name,
    phone: asString(apiData?.phone || candidate?.phone || basic.phone),
    email: asString(apiData?.email || candidate?.email || basic.email),
    company: asString(apiData?.company || candidate?.currentCompany || basic.company),
    position: asString(apiData?.position || candidate?.currentTitle || basic.position),
    workYears: (apiData?.workYears ?? candidate?.workYears ?? basic.workYears) as number | string | undefined,
    education: educationLabel(asString(apiData?.education || candidate?.education || basic.education)),
    school: asString(candidate?.school || basic.school),
    major: asString(candidate?.major || basic.major),
    expectedSalary: asString(apiData?.expectedSalary || candidate?.expectedSalary),
    source: asString(apiData?.source || candidate?.source),
    skills: parseSkills(parsed.skills, candidate?.tags),
    workExperience,
    projectExperience: normalizeProjectList(parsed.projectExperience || parsed.projects),
    educationHistory,
  }
}

export function resumeFileUrl(fileUrl?: string): string {
  if (!fileUrl) return ''
  if (fileUrl.startsWith('http')) return fileUrl
  // Ensure relative paths start with / for proper URL resolution
  return fileUrl.startsWith('/') ? fileUrl : `/${fileUrl}`
}

export function isPdfResume(resume: Pick<NormalizedResume, 'fileType' | 'fileUrl' | 'fileName'>): boolean {
  const type = (resume.fileType || '').toLowerCase()
  const name = (resume.fileName || resume.fileUrl || '').toLowerCase()
  return type === 'pdf' || name.endsWith('.pdf')
}

export function parseStatusLabel(status?: string): string {
  const s = String(status || '')
  if (s === '2' || s.toUpperCase() === 'SUCCESS') return '已解析'
  if (s === '1' || s.toUpperCase() === 'PARSING') return '解析中'
  if (s === '3' || s.toUpperCase() === 'FAILED') return '解析失败'
  if (s === '0' || s.toUpperCase() === 'PENDING') return '待解析'
  return status || '未知'
}
