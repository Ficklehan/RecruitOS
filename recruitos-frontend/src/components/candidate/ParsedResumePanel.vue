<template>
  <div class="parsed-resume-panel" :class="{ embedded }">
    <div v-if="!embedded" class="profile-card">
      <div class="profile-header">
        <div class="avatar">{{ (resume.name || '?').charAt(0) }}</div>
        <div class="profile-main">
          <h3 class="profile-name">{{ resume.name }}</h3>
          <p class="profile-title">
            {{ resume.position || '—' }}
            <template v-if="resume.company"> @ {{ resume.company }}</template>
          </p>
          <div class="profile-tags">
            <el-tag v-if="resume.workYears" size="small" type="info">{{ resume.workYears }} 年经验</el-tag>
            <el-tag v-if="resume.education" size="small">{{ resume.education }}</el-tag>
            <el-tag v-if="parseStatusLabel(resume.parseStatus)" size="small" :type="parseStatusTag(resume.parseStatus)">
              {{ parseStatusLabel(resume.parseStatus) }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-cell">
          <span class="label">手机</span>
          <span class="value">{{ resume.phone || '—' }}</span>
        </div>
        <div class="info-cell">
          <span class="label">邮箱</span>
          <span class="value">{{ resume.email || '—' }}</span>
        </div>
        <div class="info-cell">
          <span class="label">期望薪资</span>
          <span class="value">{{ resume.expectedSalary || '—' }}</span>
        </div>
        <div class="info-cell">
          <span class="label">毕业院校</span>
          <span class="value">{{ resume.school || '—' }}</span>
        </div>
        <div class="info-cell">
          <span class="label">专业</span>
          <span class="value">{{ resume.major || '—' }}</span>
        </div>
        <div class="info-cell">
          <span class="label">简历文件</span>
          <span class="value">{{ resume.fileName || '—' }}</span>
        </div>
      </div>
    </div>

    <section v-if="resume.summary" class="section-card">
      <h4 class="section-title">个人总结</h4>
      <p class="section-text">{{ resume.summary }}</p>
    </section>

    <div v-if="embedded && contactFields.length" class="contact-strip">
      <div v-for="item in contactFields" :key="item.label" class="contact-item">
        <span class="contact-label">{{ item.label }}</span>
        <span class="contact-value">{{ item.value }}</span>
      </div>
    </div>

    <div v-if="!hideSkills && resume.skills.length" class="skills-inline">
      <span class="skills-label">技能</span>
      <SkillChips :skills="resume.skills" :limit="8" :compact="embedded" />
    </div>

    <section class="section-card">
      <h4 class="section-title">工作经历</h4>
      <div v-if="resume.workExperience.length" class="timeline">
        <div v-for="(exp, idx) in resume.workExperience" :key="idx" class="timeline-item">
          <div class="timeline-dot" />
          <div class="timeline-body">
            <div class="timeline-top">
              <span class="timeline-company">{{ exp.company }}</span>
              <span class="timeline-period">
                {{ exp.startDate || '—' }} — {{ exp.endDate || '至今' }}
              </span>
            </div>
            <div class="timeline-role">{{ exp.position }}</div>
            <p v-if="exp.description" class="timeline-desc">{{ exp.description }}</p>
          </div>
        </div>
      </div>
      <span v-else class="empty-hint">暂无工作经历</span>
    </section>

    <section class="section-card">
      <h4 class="section-title">项目经历</h4>
      <div v-for="(proj, idx) in resume.projectExperience" :key="idx" class="project-block">
        <div class="project-top">
          <span class="project-name">{{ proj.name }}</span>
          <span class="project-period">{{ proj.startDate || '—' }} — {{ proj.endDate || '至今' }}</span>
        </div>
        <div v-if="proj.role" class="project-role">{{ proj.role }}</div>
        <p v-if="proj.description" class="project-desc">{{ proj.description }}</p>
      </div>
      <span v-if="!resume.projectExperience.length" class="empty-hint">暂无项目经历</span>
    </section>

    <section class="section-card">
      <h4 class="section-title">教育背景</h4>
      <div v-for="(edu, idx) in resume.educationHistory" :key="idx" class="edu-row">
        <div class="edu-school">{{ edu.school }}</div>
        <div class="edu-meta">{{ edu.degree }}<template v-if="edu.major"> · {{ edu.major }}</template></div>
        <div v-if="edu.period" class="edu-period">{{ edu.period }}</div>
      </div>
      <span v-if="!resume.educationHistory.length" class="empty-hint">暂无教育背景</span>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SkillChips from '@/components/candidate/SkillChips.vue'
import type { NormalizedResume } from '@/utils/resumeParser'
import { parseStatusLabel } from '@/utils/resumeParser'

const props = withDefaults(defineProps<{
  resume: NormalizedResume
  /** 嵌入工作台时隐藏顶部重复信息 */
  embedded?: boolean
  /** 技能已在工作台顶栏展示 */
  hideSkills?: boolean
  /** 补充候选人档案字段 */
  candidate?: Record<string, unknown> | null
}>(), {
  embedded: false,
  hideSkills: false,
  candidate: null,
})

const contactFields = computed(() => {
  const c = props.candidate || {}
  const r = props.resume
  const items = [
    { label: '手机', value: r.phone || c.phone },
    { label: '邮箱', value: r.email || c.email },
    { label: '院校', value: r.school || c.school },
    { label: '专业', value: r.major || c.major },
    { label: '期望薪资', value: formatSalary(r.expectedSalary || c.expectedSalary) },
    { label: '期望地点', value: c.workLocation },
  ]
  return items
    .map(i => ({ ...i, value: i.value ? String(i.value) : '' }))
    .filter(i => i.value && i.value !== '—')
})

function formatSalary(val: unknown) {
  if (val == null || val === '') return ''
  const n = Number(val)
  if (!Number.isNaN(n) && n > 0) return `${n}K`
  return String(val)
}

function parseStatusTag(status?: string) {
  const s = String(status || '')
  if (s === '2' || s.toUpperCase() === 'SUCCESS') return 'success'
  if (s === '3' || s.toUpperCase() === 'FAILED') return 'danger'
  if (s === '1' || s.toUpperCase() === 'PARSING') return 'warning'
  return 'info'
}
</script>

<style scoped lang="scss">
.parsed-resume-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;

  &.embedded {
    gap: 8px;

    .section-card {
      padding: 12px 14px;
    }

    .section-title {
      margin-bottom: 8px;
      font-size: 13px;
    }
  }
}

.skills-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  min-height: 0;
}

.skills-label {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}

.contact-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 6px;
  margin-bottom: 4px;

  @media (max-width: 720px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.contact-label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
}

.contact-value {
  font-size: 13px;
  color: #334155;
  word-break: break-all;
}

.profile-card,
.section-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
}

.profile-header {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-name {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.profile-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: #64748b;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.info-cell .label {
  display: block;
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.info-cell .value {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
  word-break: break-all;
}

.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.section-text {
  margin: 0;
  line-height: 1.7;
  color: #475569;
  white-space: pre-wrap;
}

.skill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  border-radius: 999px;
}

.timeline {
  position: relative;
  padding-left: 4px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;

  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
  margin-top: 6px;
  flex-shrink: 0;
}

.timeline-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.timeline-company {
  font-weight: 600;
  color: #0f172a;
}

.timeline-period {
  font-size: 12px;
  color: #94a3b8;
}

.timeline-role {
  font-size: 13px;
  color: #64748b;
  margin: 4px 0;
}

.timeline-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.project-block {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 10px;

  &:last-child {
    margin-bottom: 0;
  }
}

.project-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.project-name {
  font-weight: 600;
  color: #0f172a;
}

.project-period {
  font-size: 12px;
  color: #94a3b8;
}

.project-role {
  font-size: 13px;
  color: #64748b;
  margin: 4px 0;
}

.project-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.edu-row {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

.edu-school {
  font-weight: 600;
  color: #0f172a;
}

.edu-meta,
.edu-period {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

.empty-hint {
  font-size: 13px;
  color: #94a3b8;
}
</style>
