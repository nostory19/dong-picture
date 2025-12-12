<template>
  <div id="homePage">
<!--    搜索框-->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从海量图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch" />
    </div>
<!--    标签分类和标签筛选-->
    <a-tabs
      v-model:active-key="selectedCategory"
      @change="doSearch">
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
<!--    图片列表-->
<!--    <a-list-->
<!--      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"-->
<!--      :data-source="dataList"-->
<!--      :pagination="pagination"-->
<!--      :loading="loading"-->
<!--    >-->
<!--      &lt;!&ndash;      获取到列表中的每个元素，使用item:picture，赋值给picture&ndash;&gt;-->
<!--      <template #renderItem="{ item: picture }">-->
<!--        <a-list-item style="padding: 0">-->
<!--          <a-card hoverable @click="doClickPicture(picture)">-->
<!--            <template #cover>-->
<!--              <img-->
<!--                :alt="picture.name"-->
<!--                :src="picture.thumbnailUrl ?? picture.url"-->
<!--                style="height: 180px; object-fit: cover"-->
<!--              />-->
<!--              &lt;!&ndash;            使用object-fit让图片组件自适应宽高&ndash;&gt;-->
<!--            </template>-->
<!--            <a-card-meta :title="picture.name">-->
<!--              <template #description>-->
<!--                <a-flex>-->
<!--                  &lt;!&ndash;                  将分类和标签统一标识&ndash;&gt;-->
<!--                  <a-tag color="green">-->
<!--                    {{ picture.category ?? '模板' }}-->
<!--                  </a-tag>-->
<!--                  <a-tag v-for="tag in picture.tags" :key="tag">-->
<!--                    {{ tag }}-->
<!--                  </a-tag>-->
<!--                </a-flex>-->
<!--              </template>-->
<!--            </a-card-meta>-->
<!--          </a-card>-->
<!--        </a-list-item>-->
<!--      </template>-->
<!--    </a-list>-->
<!--    使用可复用的-->
    <PictureList :dataList="dataList" :loading="loading"/>
<!--    添加分页组件-->
    <a-pagination
      style="text-align: right"
    v-model:current="searchParams.current"
    v-model:pageSize="searchParams.pageSize"
    :total="total"
    @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { listPictureTagCategoryUsingGet, listPictureVoByPageUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import * as sea from 'node:sea'
import { useRouter } from 'vue-router'
import PictureList from '@/components/PictureList.vue'

// 数据
const dataList = ref<API.PictureVO[]>([])
// const dataList = ref([])
const total = ref(0)
const loading = ref(true)


// 标签和分类列表
const categoryList = ref<string[]>([]);
const tagList = ref<string[]>([]);
// 已有选中的分类
const selectedCategory = ref<string>('all');
const selectedTagList = ref<string[]>([]);


// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据函数
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all'){
    params.category = selectedCategory.value

  }
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag){
      params.tags.push(tagList.value[index])
    }
  })
  // 调用接口
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败, ' + res.data.message)
  }
  loading.value = false
}

// 表格变化后重新获取数据
// const doTableChange = (page: any) => {
//   searchParams.current = page.current
//   searchParams.pageSize = page.pageSize
//   fetchData()
// }


// 分页参数
// 利用计算属性computed，接收一个渲染函数，pagination就会动态变化
// const pagination = computed(() => {
//   return {
//     current: searchParams.current ?? 1,
//     pageSize: searchParams.pageSize ?? 10,
//     total: total.value,
//     onChange: (page: number, pageSize: number) => {
//       searchParams.current = page
//       searchParams.pageSize = pageSize
//       fetchData()
//     },
//   }
// })

const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
};

// 自动加载
onMounted(() => {
  fetchData()
})

const doSearch = () =>{
  // 和其他页面的搜索类似，触发搜索的时候回到第一页
  searchParams.current = 1
  fetchData()

}



const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data){
    tagList.value = res.data.data.tagList ?? [];
    categoryList.value = res.data.data.categoryList  ?? [];

  }else{
    message.error("获取标签分类列表失败" + res.data.message)
  }
}

const router = useRouter()

// 点击图片跳转到图片详情页
// const doClickPicture = (picture: API.PictureVO) => {
//   router.push({
//     path: `/picture/${picture.id}`,
//   })
// }


onMounted(() => {
  getTagCategoryOptions()
})


</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}
#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}

#homePage .tag-bar {
  margin-bottom: 16px;
}
</style>
