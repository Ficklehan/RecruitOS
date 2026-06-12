<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { RCard, RButton, RBadge, RInput, RSelect, RSkeleton } from '@/components/ui'
import {
  Building2, MapPin, Search, TrendingUp, Heart, Users, Zap, Sparkles,
  ArrowRight, Star, Quote, Clock, Briefcase, Globe, ChevronRight
} from 'lucide-vue-next'

const router = useRouter()
const loading = ref(true)
const searchKeyword = ref('')
const selectedDept = ref('')
const selectedLocation = ref('')

const departments = ['全部部门', '技术', '产品', '设计', '运营', '市场', '职能']
const locations = ['全部城市', '北京', '上海', '深圳', '杭州', '成都']

// Mock data — 实际从 API 获取
const company = ref({
  name: 'XX 科技',
  tagline: '用技术让世界更简单',
  description: '我们是一家全球领先的科技公司，致力于通过创新技术解决真实世界的问题。在全球 12 个城市设有办公室，服务超过 10 亿用户。',
  stats: [
    { label: '全球员工', value: '8,000+' },
    { label: '技术专利', value: '1,200+' },
    { label: '服务用户', value: '10 亿+' },
    { label: '城市分布', value: '12' },
  ],
})

const cultureHighlights = [
  { icon: Users, title: '扁平协作', desc: 'CEO 就在你隔壁，想法可以直接说' },
  { icon: Zap, title: '快速迭代', desc: '两周一个 sprint，想法快速落地' },
  { icon: Sparkles, title: '技术驱动', desc: '工程师文化，代码说话' },
  { icon: Globe, title: '全球化视野', desc: '跨时区协作，全球化项目' },
]

const openPositions = ref([
  { id: 1, title: '资深后端工程师', dept: '技术', location: '北京', level: 'P7-P8', type: '全职', hot: true, new: false },
  { id: 2, title: '增长产品经理', dept: '产品', location: '上海', level: 'P7', type: '全职', hot: true, new: true },
  { id: 3, title: '前端架构师', dept: '技术', location: '深圳', level: 'P8', type: '全职', hot: false, new: false },
  { id: 4, title: '交互设计师', dept: '设计', location: '北京', level: 'P6-P7', type: '全职', hot: false, new: true },
  { id: 5, title: '数据分析师', dept: '运营', location: '杭州', level: 'P6', type: '全职', hot: false, new: false },
  { id: 6, title: '品牌营销经理', dept: '市场', location: '上海', level: 'P7', type: '全职', hot: false, new: false },
])

const employeeStories = ref([
  {
    id: 1,
    name: '张明',
    role: '技术总监 · 入职 5 年',
    avatar: '',
    quote: '从 P6 到 P8，公司给了我足够的技术挑战和成长空间。最打动我的是这里的工程师文化——代码审查是学习，不是挑刺。',
    tags: ['技术成长', '工程师文化'],
  },
  {
    id: 2,
    name: '李婷',
    role: '产品 Lead · 入职 3 年',
    avatar: '',
    quote: '产品经理在这里不只是画原型。从 0 到 1 定义产品方向、跟工程一起做技术选型、看数据做增长——是全链路的产品 ownership。',
    tags: ['产品全链路', 'Ownership'],
  },
  {
    id: 3,
    name: '陈飞',
    role: '资深设计师 · 入职 2 年',
    avatar: '',
    quote: '设计师在这里有话语权。设计评审不是你展示漂亮稿，而是大家一起讨论怎么让用户用得更好。',
    tags: ['设计话语权', '用户导向'],
  },
])

const filteredPositions = computed(() => {
  return openPositions.value.filter(p => {
    const matchKeyword = !searchKeyword.value || p.title.includes(searchKeyword.value)
    const matchDept = !selectedDept.value || selectedDept.value === '全部部门' || p.dept === selectedDept.value
    const matchLoc = !selectedLocation.value || selectedLocation.value === '全部城市' || p.location === selectedLocation.value
    return matchKeyword && matchDept && matchLoc
  })
})

const hotPositions = computed(() => filteredPositions.value.filter(p => p.hot).slice(0, 3))

function goToPosition(id: number) {
  // TODO: 需要创建 /portal/job/:id 路由和对应组件
  console.warn(`职位详情页未实现: /portal/job/${id}`)
}

function goToReferral() {
  // /referral/submit/:token 需要 token 参数，跳转到内推列表页
  router.push('/talent/referral')
}

onMounted(() => { loading.value = false })
</script>

<template>
  <div class="career-site min-h-screen bg-white">
    <!-- ====== Hero ====== -->
    <section class="relative overflow-hidden bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white">
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-20 left-10 w-72 h-72 bg-blue-500 rounded-full blur-3xl" />
        <div class="absolute bottom-10 right-20 w-96 h-96 bg-purple-500 rounded-full blur-3xl" />
      </div>
      <div class="relative max-w-6xl mx-auto px-6 py-24 md:py-32">
        <div class="max-w-3xl">
          <div class="flex items-center gap-2 mb-4">
            <Building2 class="h-5 w-5 text-blue-400" />
            <span class="text-sm font-medium text-blue-400 tracking-wide uppercase">{{ company.name }}</span>
          </div>
          <h1 class="text-4xl md:text-5xl lg:text-6xl font-bold leading-tight mb-6">
            和我们一起<br />
            <span class="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-purple-400">{{ company.tagline }}</span>
          </h1>
          <p class="text-lg text-gray-300 max-w-xl mb-8 leading-relaxed">
            {{ company.description }}
          </p>
          <div class="flex flex-wrap gap-3">
            <RButton size="lg" class="bg-blue-500 hover:bg-blue-600 text-white border-0" @click="goToReferral">
              查看在招岗位 <ChevronRight class="h-4 w-4 ml-1" />
            </RButton>
            <RButton size="lg" variant="outline" class="border-gray-600 text-gray-200 hover:bg-gray-800" @click="goToReferral">
              了解我们
            </RButton>
          </div>
        </div>
      </div>

      <!-- Stats bar -->
      <div class="relative max-w-6xl mx-auto px-6 pb-12">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div v-for="stat in company.stats" :key="stat.label" class="text-center">
            <div class="text-2xl md:text-3xl font-bold text-white mb-1">{{ stat.value }}</div>
            <div class="text-sm text-gray-400">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== Culture ====== -->
    <section class="max-w-6xl mx-auto px-6 py-20">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-gray-900 mb-3">我们的文化</h2>
        <p class="text-gray-500 max-w-lg mx-auto">不只是贴在墙上的价值观，而是每天工作的方式</p>
      </div>
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-6">
        <div v-for="(item, i) in cultureHighlights" :key="i"
             class="group p-6 rounded-xl border border-gray-100 hover:border-blue-200 hover:shadow-md transition-all duration-200">
          <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center mb-4 group-hover:bg-blue-100 transition-colors">
            <component :is="item.icon" class="h-5 w-5 text-blue-600" />
          </div>
          <h3 class="font-semibold text-gray-900 mb-2">{{ item.title }}</h3>
          <p class="text-sm text-gray-500 leading-relaxed">{{ item.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ====== Hot Positions ====== -->
    <section v-if="hotPositions.length" class="bg-gray-50 py-20">
      <div class="max-w-6xl mx-auto px-6">
        <div class="flex items-center justify-between mb-8">
          <div>
            <h2 class="text-2xl font-bold text-gray-900 mb-2">🔥 热招岗位</h2>
            <p class="text-gray-500 text-sm">这些岗位正在急聘，投递简历快速响应</p>
          </div>
          <RButton variant="ghost" class="text-blue-600" @click="goToReferral">
            查看全部 <ArrowRight class="h-4 w-4 ml-1" />
          </RButton>
        </div>
        <div class="grid md:grid-cols-3 gap-4">
          <RCard v-for="pos in hotPositions" :key="pos.id"
                 class="p-5 cursor-pointer hover:border-blue-300 hover:shadow-sm transition-all"
                 @click="goToPosition(pos.id)">
            <div class="flex items-start justify-between mb-3">
              <h3 class="font-semibold text-gray-900">{{ pos.title }}</h3>
              <div class="flex gap-1.5">
                <RBadge v-if="pos.new" variant="secondary" class="text-xs">新</RBadge>
                <RBadge variant="default" class="text-xs bg-red-50 text-red-600 border-red-200">急聘</RBadge>
              </div>
            </div>
            <div class="flex items-center gap-3 text-sm text-gray-500">
              <span class="flex items-center gap-1"><MapPin class="h-3.5 w-3.5" />{{ pos.location }}</span>
              <span class="flex items-center gap-1"><Briefcase class="h-3.5 w-3.5" />{{ pos.dept }}</span>
              <span>{{ pos.level }}</span>
            </div>
          </RCard>
        </div>
      </div>
    </section>

    <!-- ====== All Positions ====== -->
    <section class="max-w-6xl mx-auto px-6 py-20">
      <div class="text-center mb-10">
        <h2 class="text-2xl font-bold text-gray-900 mb-2">在招岗位</h2>
        <p class="text-gray-500">找到适合你的位置</p>
      </div>

      <!-- Search filters -->
      <div class="flex flex-col sm:flex-row gap-3 mb-8 max-w-3xl mx-auto">
        <div class="flex-1 relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input v-model="searchKeyword"
                 class="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-300"
                 placeholder="搜索岗位..." />
        </div>
        <select v-model="selectedDept"
                class="px-4 py-2.5 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-300">
          <option v-for="d in departments" :key="d" :value="d === '全部部门' ? '' : d">{{ d }}</option>
        </select>
        <select v-model="selectedLocation"
                class="px-4 py-2.5 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-300">
          <option v-for="l in locations" :key="l" :value="l === '全部城市' ? '' : l">{{ l }}</option>
        </select>
      </div>

      <!-- Position list -->
      <div class="max-w-3xl mx-auto space-y-3">
        <div v-for="pos in filteredPositions" :key="pos.id"
             class="flex items-center justify-between p-4 border border-gray-100 rounded-lg hover:border-blue-200 hover:bg-blue-50/30 cursor-pointer transition-all group"
             @click="goToPosition(pos.id)">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-lg bg-gray-50 flex items-center justify-center group-hover:bg-blue-100 transition-colors">
              <Briefcase class="h-5 w-5 text-gray-400 group-hover:text-blue-600" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h3 class="font-medium text-gray-900">{{ pos.title }}</h3>
                <RBadge v-if="pos.new" variant="secondary" class="text-xs">新</RBadge>
                <RBadge v-if="pos.hot" variant="default" class="text-xs bg-red-50 text-red-600 border-red-200">急</RBadge>
              </div>
              <div class="flex items-center gap-3 text-sm text-gray-500 mt-0.5">
                <span class="flex items-center gap-1"><MapPin class="h-3.5 w-3.5" />{{ pos.location }}</span>
                <span>{{ pos.dept }}</span>
                <span>{{ pos.level }}</span>
                <span>{{ pos.type }}</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <ChevronRight class="h-4 w-4 text-gray-300 group-hover:text-blue-500 transition-colors" />
          </div>
        </div>

        <div v-if="filteredPositions.length === 0" class="text-center py-12 text-gray-400">
          没有匹配的岗位，试试调整筛选条件
        </div>
      </div>
    </section>

    <!-- ====== Employee Stories ====== -->
    <section class="bg-gray-50 py-20">
      <div class="max-w-6xl mx-auto px-6">
        <div class="text-center mb-12">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">他们在 XX 的故事</h2>
          <p class="text-gray-500">听听在 XX 工作的人怎么说</p>
        </div>
        <div class="grid md:grid-cols-3 gap-6">
          <div v-for="story in employeeStories" :key="story.id"
               class="bg-white rounded-xl p-6 border border-gray-100 hover:shadow-md transition-all">
            <Quote class="h-6 w-6 text-blue-200 mb-4" />
            <p class="text-gray-700 text-sm leading-relaxed mb-4 line-clamp-4">"{{ story.quote }}"</p>
            <div class="flex items-center gap-3 mb-3">
              <div class="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-purple-500 flex items-center justify-center text-white text-sm font-semibold">
                {{ story.name[0] }}
              </div>
              <div>
                <div class="font-medium text-gray-900 text-sm">{{ story.name }}</div>
                <div class="text-xs text-gray-500">{{ story.role }}</div>
              </div>
            </div>
            <div class="flex gap-1.5">
              <RBadge v-for="tag in story.tags" :key="tag" variant="secondary" class="text-xs">{{ tag }}</RBadge>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ====== CTA ====== -->
    <section class="max-w-6xl mx-auto px-6 py-20 text-center">
      <div class="max-w-2xl mx-auto">
        <h2 class="text-3xl font-bold text-gray-900 mb-4">准备好加入我们了吗？</h2>
        <p class="text-gray-500 mb-8">发现适合你的岗位，开启 XX 之旅</p>
        <div class="flex justify-center gap-3">
          <RButton size="lg" class="bg-blue-500 hover:bg-blue-600 text-white border-0" @click="goToReferral">
            查看全部岗位 <ArrowRight class="h-4 w-4 ml-1" />
          </RButton>
          <RButton size="lg" variant="outline" @click="goToReferral">
            内推投递
          </RButton>
        </div>
      </div>
    </section>

    <!-- ====== Footer ====== -->
    <footer class="border-t border-gray-100 bg-gray-50">
      <div class="max-w-6xl mx-auto px-6 py-8 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div class="flex items-center gap-2 text-sm text-gray-500">
          <Building2 class="h-4 w-4" />
          <span>{{ company.name }} · 招聘官网</span>
        </div>
        <div class="flex gap-6 text-sm text-gray-400">
          <a href="#" class="hover:text-gray-600 transition-colors">隐私政策</a>
          <a href="#" class="hover:text-gray-600 transition-colors">Cookie 设置</a>
          <a href="#" class="hover:text-gray-600 transition-colors">联系我们</a>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.career-site {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.line-clamp-4 {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
