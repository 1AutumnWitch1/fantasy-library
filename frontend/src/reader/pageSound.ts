// Generate a short paper rustle and reuse it for the life of the reader.
export function pageSound() {
  let context: AudioContext | undefined
  let buffer: AudioBuffer | undefined
  let active: AudioBufferSourceNode | undefined

  function unlock() {
    try {
      if (!context) context = new AudioContext()
      if (context.state === 'suspended') void context.resume().catch(() => {})
    } catch {
      // Browsers can deny playback. The page turn still completes.
    }
  }

  function stop() {
    const source = active
    active = undefined
    if (!source) return
    source.stop()
    source.disconnect()
  }

  function play() {
    if (!context || context.state !== 'running') return
    stop()
    if (!buffer) {
      const length = Math.round(context.sampleRate * 0.24)
      buffer = context.createBuffer(1, length, context.sampleRate)
      const samples = buffer.getChannelData(0)
      let previous = 0
      for (let i = 0; i < length; i++) {
        const t = i / length
        const noise = Math.random() * 2 - 1
        const envelope = Math.sin(Math.PI * t) ** 2
        samples[i] = (noise - previous * 0.7) * envelope * (0.65 + Math.sin(t * 24) * 0.33)
        previous = noise
      }
    }

    const source = context.createBufferSource()
    const filter = context.createBiquadFilter()
    const gain = context.createGain()
    source.buffer = buffer
    filter.type = 'bandpass'
    filter.frequency.value = 1250
    filter.Q.value = 0.65
    gain.gain.value = 0.12
    source.connect(filter).connect(gain).connect(context.destination)
    active = source
    source.addEventListener('ended', () => {
      if (active === source) active = undefined
      source.disconnect()
      filter.disconnect()
      gain.disconnect()
    }, { once: true })
    source.start()
  }

  function dispose() {
    stop()
    if (context && context.state !== 'closed') void context.close().catch(() => {})
    context = undefined
    buffer = undefined
  }

  return { unlock, play, stop, dispose }
}
