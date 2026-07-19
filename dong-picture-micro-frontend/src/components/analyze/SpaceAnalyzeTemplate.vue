
<template>
<!--使用echart的一个模板，包含重要的几个部分-->
  <div class="space-xxx-analyze">
<!--    统计组件风格，使用card包装-->
    <a-card title="分析需求名称">
      <v-chart
        :option="option"
        style="height: 320px"
        :loading="loading"/>
    </a-card>
  </div>
</template>
<script setup lang="ts">

// 后端公共请求属性
interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})


const option = ref({})

const loading = ref(false)


</script>

<style scoped>

</style>
