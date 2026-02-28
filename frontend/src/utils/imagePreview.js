function writeLoadingPage(win, title) {
  if (!win) return
  const safeTitle = title || '图片预览'
  win.document.title = safeTitle
  win.document.body.style.margin = '0'
  win.document.body.style.fontFamily = '"Microsoft YaHei", "PingFang SC", Arial, sans-serif'
  win.document.body.style.display = 'flex'
  win.document.body.style.alignItems = 'center'
  win.document.body.style.justifyContent = 'center'
  win.document.body.style.background = '#f4f6f9'
  win.document.body.innerHTML = '<div style="color:#334155;font-size:14px;">图片加载中...</div>'
}

export async function previewImageById(http, fileId, title = '图片预览') {
  if (!fileId) return

  // Open first to keep it in the same user gesture; prevents popup blocking.
  const previewWindow = window.open('', '_blank')
  writeLoadingPage(previewWindow, title)

  try {
    const resp = await http.get(`/files/${fileId}/download`, { responseType: 'blob' })
    const contentType = resp?.headers?.['content-type'] || 'image/jpeg'
    const blob = resp?.data instanceof Blob ? resp.data : new Blob([resp.data], { type: contentType })
    const url = URL.createObjectURL(blob)

    if (previewWindow && !previewWindow.closed) {
      previewWindow.location.href = url
    } else {
      const a = document.createElement('a')
      a.href = url
      a.target = '_blank'
      a.rel = 'noopener'
      a.click()
    }

    setTimeout(() => URL.revokeObjectURL(url), 120_000)
  } catch (e) {
    if (previewWindow && !previewWindow.closed) {
      previewWindow.close()
    }
    throw e
  }
}

