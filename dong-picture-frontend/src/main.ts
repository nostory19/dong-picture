// import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
// import VueCropper from 'vue-cropper'
// import 'vue-cropper/dist/index.css'
import '@/access.ts'



const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)
// app.use()
app.mount('#app')
