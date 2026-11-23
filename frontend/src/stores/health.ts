import { defineStore } from 'pinia'
import { fetchHealth, type HealthResponse } from '../api/health'

interface HealthState {
  phase: 'idle' | 'loading' | 'ready' | 'failed'
  health: HealthResponse | null
}

export const useHealthStore = defineStore('health', {
  state: (): HealthState => ({ phase: 'idle', health: null }),

  actions: {
    async refresh() {
      if (this.phase === 'loading') return

      this.phase = 'loading'
      this.health = null

      try {
        this.health = await fetchHealth()
        this.phase = 'ready'
      } catch {
        this.phase = 'failed'
      }
    },
  },
})
