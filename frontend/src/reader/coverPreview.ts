import { siteMediaUrl } from '../api/siteMedia'

const variants = import.meta.glob<string>('../assets/catalog/**/*.webp', {
  eager: true,
  query: '?url',
  import: 'default',
})
const prefix = '/api/media/catalog/'

export function coverPreview(url: string) {
  if (!url.startsWith(prefix)) return url
  const path = url.slice(prefix.length)
  const localVariant = variants['../assets/catalog/' + path]
  return localVariant ? siteMediaUrl('catalog/' + path, localVariant) : url
}
