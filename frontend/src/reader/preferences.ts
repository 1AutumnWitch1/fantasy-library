export type MotionPreference = 'auto' | 'full' | 'simple'
export const compactReaderQuery = '(max-width: 46rem)'
export const reducedMotionQuery = '(prefers-reduced-motion: reduce)'

export function simplifyReader(setting: MotionPreference, system: boolean, single: boolean, lightweight: boolean) {
  return system || single || setting === 'simple' || (setting === 'auto' && lightweight)
}

export function saveDataEnabled() {
  if (typeof navigator === 'undefined') return false
  const connection = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection
  return connection?.saveData === true
}

export function lightweightDevice() {
  if (typeof navigator === 'undefined') return false
  const memory = (navigator as Navigator & { deviceMemory?: number }).deviceMemory
  return saveDataEnabled() || (typeof memory === 'number' && memory <= 4)
    || (navigator.hardwareConcurrency > 0 && navigator.hardwareConcurrency <= 4)
}

interface StoredPreferences {
  motion: MotionPreference
  sound: boolean
}

const storageKey = 'fantasy-library.reader-preferences'

export function readReaderPreferences(): StoredPreferences {
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey) ?? '{}')
    return {
      motion: stored?.motion === 'full' || stored?.motion === 'simple' ? stored.motion : 'auto',
      sound: stored?.sound === true,
    }
  } catch {
    return { motion: 'auto', sound: false }
  }
}

export function storeReaderPreferences(value: StoredPreferences) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(value))
  } catch {
    // Reading remains available when persistent storage is blocked.
  }
}
