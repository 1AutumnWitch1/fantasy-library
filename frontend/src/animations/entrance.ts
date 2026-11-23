import { gsap } from 'gsap'

export function animateStatusPanel(panel: HTMLElement) {
  const media = gsap.matchMedia()

  media.add('(prefers-reduced-motion: no-preference)', () => {
    gsap.from(panel, { autoAlpha: 0, y: 12, duration: 0.45, ease: 'power2.out' })
  })

  return () => media.revert()
}
