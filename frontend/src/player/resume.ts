import type { Episode } from '../api/playback'
import type { SavedEpisode } from '../api/viewing'

type EpisodeIdentity = Pick<Episode, 'id' | 'edition' | 'number'>
const episodeKey = (workId: number, episode: EpisodeIdentity) =>
  'fantasy-library.resume.' + workId + '.' + episode.edition + '.' + episode.number

const keyFor = (workId: number, episodeId: number) => 'fantasy-library.playback.' + workId + '.' + episodeId

export function readPosition(workId: number, episodeId: number) {
  try {
    const value = Number(localStorage.getItem(keyFor(workId, episodeId)))
    return Number.isFinite(value) && value > 0 ? value : 0
  } catch {
    return 0
  }
}

export function savePosition(workId: number, episodeId: number, position: number) {
  try {
    if (Number.isFinite(position) && position > 0) localStorage.setItem(keyFor(workId, episodeId), String(position))
    else localStorage.removeItem(keyFor(workId, episodeId))
  } catch {
    // The video still plays when browser storage is unavailable.
  }
}

export function readEpisodePosition(workId: number, episode: EpisodeIdentity, saved?: SavedEpisode) {
  try {
    const value = localStorage.getItem(episodeKey(workId, episode))
    if (value) {
      const local = JSON.parse(value) as { position: number; updatedAt: number }
      if (Number.isFinite(local.position) && (!saved || local.updatedAt >= Date.parse(saved.updatedAt))) {
        return Math.max(0, local.position)
      }
    }
  } catch {
    // A blocked or outdated storage entry can still fall back to the server.
  }
  return saved ? (saved.completed ? 0 : saved.positionMs / 1000) : readPosition(workId, episode.id)
}

export function saveEpisodePosition(workId: number, episode: EpisodeIdentity, position: number) {
  try {
    localStorage.setItem(episodeKey(workId, episode), JSON.stringify({ position, updatedAt: Date.now() }))
  } catch {
    // Browser storage is optional.
  }
  savePosition(workId, episode.id, position)
}

export function restorePosition(seconds: number, duration: number, sameEpisode: boolean) {
  if (!Number.isFinite(duration) || duration <= 0 || !Number.isFinite(seconds) || seconds < 0) return 0
  if (!sameEpisode && seconds >= duration - 1) return 0
  return Math.min(seconds, Math.max(0, duration - 0.1))
}
