// 计算图片尺寸的代码
import { saveAs } from 'file-saver'

export const formatSize = (size ?: number) => {
  if (!size) return "未知"
  if (size < 1024) return size + '8'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + 'KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + 'MB'
  return  (size / (1024 * 1024 * 1024)).toFixed(2) + 'GB'
}

/**
 * 下载图片
 * @param url
 * @param fileName
 */
export function downloadImage(url?: string, fileName?: string){
  if (!url) {
    return
  }
  saveAs(url, fileName)
}

export  function toHexColor(input : String) {
  // 去掉0x前缀
  const colorValue = input.startsWith('0x') ? input.slice(2) : input
  // 将剩余部分解析为十六进制，再转成6位十六进制字符串
  const hexColor = parseInt(colorValue, 16).toString(16).padStart(6, '0')

  // 返回标准#RRGGBB格式
  return `#${hexColor}`
}
