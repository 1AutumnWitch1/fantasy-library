export const useMinioSiteMedia = import.meta.env.VITE_SITE_MEDIA_FROM_MINIO === 'true'

export function siteMediaUrl(path: string, fallback: string) {
  return useMinioSiteMedia ? '/api/media/site/' + path : fallback
}

export function configureSiteTheme(style: CSSStyleDeclaration) {
  if (!useMinioSiteMedia) return
  const assets = {
    'library-hall': 'shelf/library-hall.webp',
    'walnut-grain': 'shelf/walnut-grain.webp',
    'leather-grain': 'shelf/leather-grain.webp',
    'paper-grain': 'reader/parchment.webp',
    'ledger': 'account/ledger.webp',
    'ledger-small': 'account/ledger-small.webp',
  }
  for (const [name, path] of Object.entries(assets)) {
    style.setProperty('--remote-' + name, 'url("' + siteMediaUrl(path, '') + '")')
  }
}
