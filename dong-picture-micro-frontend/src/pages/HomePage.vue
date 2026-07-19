<template>
  <div id="homePage">
    <!-- 精选专题 Banner 轮播 -->
    <div class="featured-banner">
      <a-carousel autoplay :autoplay-speed="4000" effect="fade">
        <div class="banner-slide slide-1">
          <div class="banner-content">
            <h2 class="banner-title">精选专题页</h2>
            <p class="banner-desc">汇集优质图片资源，发现更多精彩视觉内容</p>
            <a-button type="primary" size="large" class="banner-btn">点击直达</a-button>
          </div>
        </div>
        <div class="banner-slide slide-2">
          <div class="banner-content">
            <h2 class="banner-title">VIDEO 精选</h2>
            <p class="banner-desc">高质量视频素材，满足您的多元创作需求</p>
            <a-button type="primary" size="large" class="banner-btn">点击直达</a-button>
          </div>
        </div>
        <div class="banner-slide slide-3">
          <div class="banner-content">
            <h2 class="banner-title">MUSIC 精选</h2>
            <p class="banner-desc">丰富的音乐素材库，为作品注入灵魂</p>
            <a-button type="primary" size="large" class="banner-btn">点击直达</a-button>
          </div>
        </div>
      </a-carousel>
    </div>

    <!-- 全局搜索框 -->
    <div class="global-search">
      <a-input-group compact>
        <a-select v-model:value="searchType" size="large" class="search-type-select">
          <a-select-option value="picture">
            <picture-outlined />
            <span style="margin-left: 4px">图片</span>
          </a-select-option>
        </a-select>
        <a-input
          v-model:value="searchParams.searchText"
          size="large"
          placeholder="尝试输入画面描述，如：'客厅摆放的木质物架'"
          class="search-input"
          @pressEnter="doSearch"
        />
        <a-button size="large" class="camera-btn" @click="doSearchByImage">
          <template #icon><camera-outlined /></template>
        </a-button>
        <a-button type="primary" size="large" @click="doSearch" class="search-btn">
          <template #icon><search-outlined /></template>
          搜索
        </a-button>
      </a-input-group>
    </div>

    <!-- 素材总量展示 -->
    <div class="material-count">
      <span class="count-dot"></span>
      <span class="count-label">当前上架素材</span>
      <span class="count-number">{{ animatedCount.toLocaleString() }}</span>
    </div>

    <!-- 热门搜索 -->
    <div v-if="hotSearchTags.length > 0" class="hot-search">
      <span class="hot-search-label">热门搜索：</span>
      <a-space :size="[8, 8]" wrap>
        <a-tag
          v-for="tag in hotSearchTags"
          :key="tag"
          color="processing"
          @click="doHotSearch(tag)"
          class="hot-search-tag"
        >
          {{ tag }}
        </a-tag>
      </a-space>
    </div>

    <!-- 标签分类和标签筛选 -->
    <a-tabs
      v-model:active-key="selectedCategory"
      @change="doSearch"
      class="category-tabs"
    >
      <a-tab-pane key="all" tab="全部"/>
      <a-tab-pane v-for="category in categoryList" :tab="category" :key="category"/>
    </a-tabs>
    <div class="tag-bar">
      <span style="margin-right: 8px">标签: </span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch">
          {{tag}}
        </a-checkable-tag>
      </a-space>
    </div>

    <!-- 排序切换 -->
    <div class="sort-bar">
      <a-radio-group v-model:value="sortType" @change="onSortChange" button-style="solid" size="small">
        <a-radio-button value="latest">最新</a-radio-button>
        <a-radio-button value="hot">最热</a-radio-button>
      </a-radio-group>
    </div>

    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading"/>

    <!-- 分页组件 -->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />

    <!-- 悬浮侧边栏 -->
    <div class="floating-sidebar">
      <a-tooltip title="积分翻倍兑" placement="left">
        <div class="float-item" @click="doPointsExchange">
          <gift-outlined />
        </div>
      </a-tooltip>
      <a-tooltip title="客服帮助" placement="left">
        <div class="float-item" @click="doCustomerService">
          <customer-service-outlined />
        </div>
      </a-tooltip>
      <a-tooltip title="返回顶部" placement="left">
        <div class="float-item" @click="doBackToTop">
          <vertical-align-top-outlined />
        </div>
      </a-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import {
  PictureOutlined,
  VideoCameraOutlined,
  CustomerServiceOutlined,
  CameraOutlined,
  SearchOutlined,
  GiftOutlined,
  VerticalAlignTopOutlined,
} from '@ant-design/icons-vue'
import PictureList from '@/components/PictureList.vue'

const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)
const searchType = ref('picture')

// 素材总量
const totalMaterials = ref(0)
const animatedCount = ref(0)

// 热门搜索标签
const hotSearchTags = ref<string[]>([])

// 标签和分类列表
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const selectedTagList = ref<string[]>([])

// 排序类型
const sortType = ref<string>('latest')

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const onSortChange = () => {
  if (sortType.value === 'latest') {
    searchParams.sortField = 'createTime'
    searchParams.sortOrder = 'descend'
  } else if (sortType.value === 'hot') {
    searchParams.sortField = 'thumbCount'
    searchParams.sortOrder = 'descend'
  }
  doSearch()
}

const fetchData = async () => {
  loading.value = true
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }
  loading.value = false
}

const fetchTotalCount = async () => {
  const res = await listPictureVoByPageUsingPost({
    current: 1,
    pageSize: 1,
  })
  if (res.data.data) {
    totalMaterials.value = res.data.data.total ?? 0
    animateCount()
  }
}

const animateCount = () => {
  const target = totalMaterials.value
  if (target <= 0) return
  const duration = 1500
  const startTime = Date.now()
  const tick = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    // easeOutExpo
    const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress)
    animatedCount.value = Math.floor(eased * target)
    if (progress < 1) {
      requestAnimationFrame(tick)
    }
  }
  requestAnimationFrame(tick)
}

const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

const doHotSearch = (tag: string) => {
  searchParams.searchText = tag
  doSearch()
}

const doSearchByImage = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = (e) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (file) {
      message.info('以图搜图功能开发中')
    }
  }
  input.click()
}

const doPointsExchange = () => {
  message.info('积分翻倍兑活动即将上线')
}

const doCustomerService = () => {
  message.info('客服功能开发中')
}

const doBackToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
    hotSearchTags.value = (res.data.data.tagList ?? []).slice(0, 8)
  } else {
    message.error('获取标签分类列表失败' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
  fetchTotalCount()
  getTagCategoryOptions()
})
</script>

<style scoped>
#homePage {
  margin: -28px -28px 16px;
}

/* Banner 轮播 */
.featured-banner {
  margin-bottom: 32px;
}
.featured-banner :deep(.ant-carousel .slick-slide) {
  overflow: hidden;
}
.banner-slide {
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.slide-1 {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.slide-2 {
  background: linear-gradient(135deg, #0d1b2a 0%, #1b2838 50%, #1a3a4a 100%);
}
.slide-3 {
  background: linear-gradient(135deg, #1a1a2e 0%, #2d1b69 50%, #0f3460 100%);
}
.banner-content {
  color: #fff;
}
.banner-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: 2px;
}
.banner-desc {
  font-size: 16px;
  margin-bottom: 28px;
  opacity: 0.8;
}

/* 全局搜索框 */
.global-search {
  max-width: 720px;
  margin: 0 auto 28px;
  padding: 0 16px;
}
.search-type-select {
  width: 110px;
}
.search-input {
  width: 320px;
}

/* 素材总量 */
.material-count {
  text-align: center;
  margin-bottom: 24px;
  font-size: 15px;
  color: #666;
}
.count-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #722ed1;
  margin-right: 6px;
  vertical-align: middle;
}
.count-label {
  vertical-align: middle;
}
.count-number {
  font-size: 22px;
  font-weight: 700;
  color: #1677ff;
  margin-left: 6px;
  vertical-align: middle;
}

/* 热门搜索 */
.hot-search {
  max-width: 720px;
  margin: 0 auto 24px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.hot-search-label {
  color: #999;
  font-size: 14px;
  white-space: nowrap;
}
.hot-search-tag {
  cursor: pointer;
}

/* 分类 Tabs */
.category-tabs {
  max-width: 960px;
  margin: 0 auto;
}

/* 标签筛选 */
.tag-bar {
  max-width: 960px;
  margin: 0 auto 16px;
}

/* 排序切换 */
.sort-bar {
  max-width: 960px;
  margin: 0 auto 16px;
}

/* 图片列表 & 分页 */
#homePage :deep(.picture-list),
#homePage > .ant-pagination {
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  padding: 0 16px;
}

/* 悬浮侧边栏 */
.floating-sidebar {
  position: fixed;
  right: 20px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 1000;
}
.float-item {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 20px;
  color: #666;
  transition: all 0.3s;
}
.float-item:hover {
  color: #1677ff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}
</style>
