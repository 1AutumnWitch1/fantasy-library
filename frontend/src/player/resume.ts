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

export function restorePosition(seconds: number, duration: number, sameEpisode: boolean) {
  if (!Number.isFinite(duration) || duration <= 0 || !Number.isFinite(seconds) || seconds < 0) return 0
  if (!sameEpisode && seconds >= duration - 1) return 0
  return Math.min(seconds, Math.max(0, duration - 0.1))
}
