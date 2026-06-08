<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">简历收件</h2>
        <p class="page-subtitle">管理各渠道收到的简历，解析后可加入人才库或关联在招职位</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
        </el-button>
        <el-button type="primary" @click="$router.push('/talent/resumes/upload')">
          <el-icon><Upload /></el-icon>
          上传简历
        </el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #EFF6FF; color: #3B82F6">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">简历总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #D1FAE5; color: #059669">
          <el-icon :size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.parsed }}</div>
          <div class="stat-label">已解析</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEF3C7; color: #D97706">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待解析</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #FEE2E2; color: #DC2626">
          <el-icon :size="24"><Warning /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.failed }}</div>
          <div class="stat-label">解析失败</div>
        </div>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索姓名、公司、技能..."
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="filterSource" placeholder="来源渠道" clearable style="width: 140px">
        <el-option label="Boss直聘" value="BOSS" />
        <el-option label="猎聘" value="LIEPIN" />
        <el-option label="手动上传" value="MANUAL" />
        <el-option label="内推" value="REFERRAL" />
        <el-option label="猎头" value="HEADHUNTER" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="解析状态" clearable style="width: 140px">
        <el-option label="已解析" value="PARSED" />
        <el-option label="待解析" value="PENDING" />
        <el-option label="解析失败" value="FAILED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset">
        <el-icon><RefreshRight /></el-icon>
        重置
      </el-button>
      <div style="flex: 1" />
      <el-button v-if="selectedIds.length" type="success" @click="handleBatchImport">
        <el-icon><FolderAdd /></el-icon>
        批量加入人才库 ({{ selectedIds.length }})
      </el-button>
    </div>

    <div class="data-card">
      <el-table
        v-if="resumeList.length"
        :data="resumeList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="name" label="姓名" width="120">
          <template #default="{ row }">
            <span class="title-link" @click="goDetail(row)">{{ row.name || '未识别' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="company" label="当前公司" min-width="160" show-overflow-tooltip />
        <el-table-column prop="position" label="当前职位" min-width="140" show-overflow-tooltip />
        <el-table-column prop="workYears" label="工作年限" width="90" align="center">
          <template #default="{ row }">{{ row.workYears ? row.workYears + '年' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="education" label="学历" width="90" align="center">
          <template #default="{ row }">{{ educationLabel(row.education) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="sourceTagMap[row.source] || 'info'" size="small" disable-transitions>
              {{ sourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="parseStatusTagMap[row.parseStatus]" size="small" disable-transitions>
              {{ parseStatusLabelMap[row.parseStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row)">查看</el-button>
            <el-button v-if="row.parseStatus !== 'PARSED'" type="success" link size="small" @click="handleParse(row)">解析</el-button>
            <el-button type="warning" link size="small" @click="handleImportPool(row)">加入人才库</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyStateCta
        v-else
        title="暂无简历"
        description="上传简历后将自动进入收件列表，解析后可加入人才库进行匹配"
        :actions="[
          { label: '上传简历', type: 'primary', onClick: () => router.push('/talent/resumes/upload') },
          { label: '去人才库', type: 'default', onClick: () => router.push('/talent/pool') },
        ]"
      />
    </div>

    <div v-if="total > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight, Refresh, Upload, Document, CircleCheck, Clock, Warning, FolderAdd } from '@element-plus/icons-vue'
import EmptyStateCta from '@/components/common/EmptyStateCta.vue'
import { sourceLabel, educationLabel } from '@/constants/businessLabels'
import { getResumeList, parseResume, deleteResume, importToTalentPool, batchImportToTalentPool } from '@/api/modules/resume'

const router = useRouter()

const searchKeyword = ref('')
const filterSource = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const resumeList = ref<any[]>([])
const selectedIds = ref<number[]>([])

const stats = computed(() => {
  const total = resumeList.value.length
  const parsed = resumeList.value.filter(r => r.parseStatus === 'PARSED').length
  const pending = resumeList.value.filter(r => r.parseStatus === 'PENDING').length
  const failed = resumeList.value.filter(r => r.parseStatus === 'FAILED').length
  return { total, parsed, pending, failed }
})

const sourceTagMap: Record<string, string> = {
  BOSS: '', LIEPIN: 'danger', MANUAL: 'info', REFERRAL: 'success', HEADHUNTER: 'warning',
}
const parseStatusTagMap: Record<string, string> = {
  PARSED: 'success', PENDING: 'warning', FAILED: 'danger',
}
const parseStatusLabelMap: Record<string, string> = {
  PARSED: '已解析', PENDING: '待解析', FAILED: '解析失败',
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows.map(r => r.id)
}

function goDetail(row: any) {
  router.push(`/talent/resumes/${row.id}`)
}

async function handleParse(row: any) {
  try {
    await parseResume(row.id)
    row.parseStatus = 'PARSED'
    ElMessage.success('解析完成')
  } catch {
    ElMessage.error('解析失败')
  }
}

async function handleImportPool(row: any) {
  try {
    await importToTalentPool(row.id)
    ElMessage.success('已加入人才库')
  } catch {
    ElMessage.error('加入失败')
  }
}

async function handleBatchImport() {
  try {
    await batchImportToTalentPool(selectedIds.value)
    ElMessage.success(`已批量加入人才库 ${selectedIds.value.length} 份简历`)
    selectedIds.value = []
  } catch {
    ElMessage.error('批量加入失败')
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该简历？', '删除确认', { type: 'warning' })
  try {
    await deleteResume(row.id)
    resumeList.value = resumeList.value.filter(r => r.id !== row.id)
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handleReset() {
  searchKeyword.value = ''
  filterSource.value = ''
  filterStatus.value = ''
  currentPage.value = 1
  loadData()
}

function handleRefresh() {
  loadData()
  ElMessage.success('已刷新')
}

async function loadData() {
  try {
    const res: any = await getResumeList({
      keyword: searchKeyword.value,
      source: filterSource.value,
      parseStatus: filterStatus.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    const data = res.data || res
    resumeList.value = Array.isArray(data) ? data : data.records || data.list || []
    total.value = data.total || resumeList.value.length
  } catch {
    resumeList.value = []
    total.value = 0
  }
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.header-actions {
  display: flex;
  gap: 8px;
}

.title-link {
  color: $primary-color;
  cursor: pointer;
  font-weight: 500;
  &:hover { text-decoration: underline; }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: $spacing-lg;
  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
}

.stat-card {
  background: $bg-card;
  border-radius: $border-radius;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  .stat-icon {
    width: 48px; height: 48px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  }
  .stat-value { font-size: 24px; font-weight: 700; color: $text-primary; line-height: 1.2; }
  .stat-label { font-size: 13px; color: $text-secondary; margin-top: 2px; }
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
