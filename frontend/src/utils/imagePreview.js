import { useImagePreview } from '../composables/useImagePreview'

const imagePreview = useImagePreview()

export async function openImagePreview({
  http,
  fileId,
  title = '图片预览',
  galleryIds = [],
  fileNameMap = {}
} = {}) {
  await imagePreview.open({
    http,
    fileId,
    title,
    galleryIds,
    fileNameMap
  })
}

export async function previewImageById(
  http,
  fileId,
  title = '图片预览',
  galleryIds = [],
  fileNameMap = {}
) {
  await openImagePreview({
    http,
    fileId,
    title,
    galleryIds,
    fileNameMap
  })
}

