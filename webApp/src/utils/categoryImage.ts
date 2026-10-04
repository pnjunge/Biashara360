const MAX_FILE_BYTES = 5 * 1024 * 1024
const MAX_IMAGE_BYTES = 256 * 1024

export async function prepareCategoryImage(file: File): Promise<string> {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    throw new Error('Choose a JPEG, PNG or WebP image.')
  }
  if (file.size === 0 || file.size > MAX_FILE_BYTES) {
    throw new Error('Choose an image smaller than 5 MB.')
  }
  const url = URL.createObjectURL(file)
  try {
    const image = new Image()
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve()
      image.onerror = () => reject(new Error('This image could not be read. Try another file.'))
      image.src = url
    })
    const scale = Math.min(1, 512 / Math.max(image.naturalWidth, image.naturalHeight))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale))
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale))
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Image processing is unavailable in this browser.')
    context.fillStyle = '#ffffff'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    const result = canvas.toDataURL('image/jpeg', 0.85)
    if (!result.startsWith('data:image/jpeg;base64,') || result.split(',')[1].length > Math.ceil(MAX_IMAGE_BYTES / 3) * 4) {
      throw new Error('This image is too large after resizing. Try a smaller image.')
    }
    return result
  } finally {
    URL.revokeObjectURL(url)
  }
}
