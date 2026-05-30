import defaultSettings from '@/settings'

const title = defaultSettings.title || 'TWENTY ADMIN'

export default function getPageTitle(pageTitle) {
  if (pageTitle) {
    return `${pageTitle} - ${title}`
  }
  return `${title}`
}
