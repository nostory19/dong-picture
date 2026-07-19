<template>
  <div class="picture-upload">
<!--    action提供默认的后端服务，自定义如何上传后端-->
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :before-upload="beforeUpload"
      :custom-request="handleUpload"
    >
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">点击或拖拽上传图片</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import type { UploadChangeParam, UploadProps } from 'ant-design-vue';
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO;
  spaceId?: number;
  onsuccess?: (newPicture: API.PictureVO) => void
}


const props = defineProps<Props>();


const loading = ref<boolean>(false);
// const imageUrl = ref<string>('');


const handleUpload =  async  ({file}: any
) => {
  // file就是可以上传的文件
  loading.value = true;
  try{
    // 获取业务参数，判断图片是否存在，存在则是图片id，反之为空
    const params = props.picture?{id: props.picture.id} : {};
    params.spaceId = props.spaceId;
    // 调用后端接口上传图片
    const res = await uploadPictureUsingPost(params, {}, file);
    if (res.data.code === 0 && res.data.data) {
      message.success("图片上传成功");
      // 调用父组件的回调函数
      props.onsuccess?.(res.data.data);
    }else{
      message.error("图片上传失败, " + res.data.message);
    }
  }catch (error){
    console.log("图片上传失败. ", error);
    message.error("图片上传失败, " + error.message);
  }

  // 执行完成
  loading.value = false;

}


/**
 * 上传前的校验
 * @param file
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  // 校验图片格式
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('不支持上传该格式的图片，推荐jpg或png');
  }
  // 校验图片大小
  const isLt2M = file.size / 1024 / 1024 < 8; // 后端<8
  if (!isLt2M) {
    message.error('不能上传超过8M的图片!');
  }
  return isJpgOrPng && isLt2M;
};
</script>
<style scoped>
.picture-upload :deep(.ant-upload){
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}
.picture-upload img {
  max-width: 100%;
  max-height: 480px;
}
.avatar-uploader > .ant-upload {
  width: 128px;
  height: 128px;
}
.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>
