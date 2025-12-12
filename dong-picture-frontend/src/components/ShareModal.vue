<template>
<!--  做成父控组件-->
  <a-modal v-model:visible="visible"
           title="分享图片"
           :footer="false"
           @cancel="closeModal">
    <h4>复制分享链接</h4>
    <a-typography-link copyable>
      {{link}}
    </a-typography-link>
    <div style="margin-bottom: 16px" />
    <h4>手机扫码查看</h4>
    <div style="display: flex; justify-content: center;">
    <a-qrcode :value="link" />
    </div>
<!--    可以给二维码加上logo-->
  </a-modal>
</template>

<script setup lang="ts">
import {defineProps, ref, withDefaults, defineExpose} from 'vue'

/**
 * 定义组件属性类型
 */
interface  Props {
  title: string
  link: string
}

/**
 * 给组件指定初始值
 */
const props = withDefaults(defineProps<Props>(), {
  title: '分享图片',
  link: 'www.baidu.com',
})

// 是否可见
const visible = ref(false)
// 打开弹窗
// 将打开弹窗这个函数暴露给父组件，让父组件打开即可
const openModal = () => {
  visible.value = true
  // console.log("visible: ", visible.value)
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露父组件
defineExpose({
  openModal,
});

</script>
