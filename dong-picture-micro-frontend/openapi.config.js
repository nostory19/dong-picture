import {generateService} from '@umijs/openapi'
// 生成文档的接口配置
generateService({
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v2/api-docs',
  serversPath: './src',
})

// 生成的代码放在哪个目录下，放在./src目录下

