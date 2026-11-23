import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { PageSide } from '../reader/pagePosition'
import { readReaderPreferences, storeReaderPreferences, type MotionPreference } from '../reader/preferences'
import type { PersonalChapter } from '../api/personal'
import type { RecommendationType } from '../api/recommendations'
import type { ShelfBookId } from '../data/shelfBooks'

interface ReadingState {
  spreadIndex: number
  selectedTags: string[]
  searchQuery: string
  slideIndexes: Record<number, number>
  carouselPaused: boolean
}

export const useReaderStore = defineStore('reader', () => {
  const reading = ref<Partial<Record<ShelfBookId, ReadingState>>>({})
  const initial = readReaderPreferences()
  const motionPreference = ref<MotionPreference>(initial.motion)
  const soundEnabled = ref(initial.sound)
  const singleSides = ref<Partial<Record<ShelfBookId, PageSide>>>({})
  watch([motionPreference, soundEnabled], ([motion, sound]) => storeReaderPreferences({ motion, sound }), { flush: 'sync' })
  const recommendationType = ref<RecommendationType | null>(null)
  const personalChapter = ref<PersonalChapter>('anime')

  function stateFor(id: ShelfBookId) {
    if (!reading.value[id]) {
      reading.value[id] = {
        spreadIndex: 0,
        selectedTags: [],
        searchQuery: '',
        slideIndexes: {},
        carouselPaused: false,
      }
    }
    return reading.value[id]
  }

  return { reading, motionPreference, soundEnabled, singleSides, recommendationType, personalChapter, stateFor }
})
