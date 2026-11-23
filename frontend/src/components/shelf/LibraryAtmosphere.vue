<template>
  <div class="library-atmosphere" aria-hidden="true"></div>
</template>

<style scoped lang="scss">
.library-atmosphere {
  position: absolute;
  z-index: -1;
  inset: 0;
  background:
    radial-gradient(ellipse 68% 44% at 50% 24%, #e6a95c26 0%, #8a572a1c 38%, transparent 74%),
    radial-gradient(ellipse at 50% 34%, #452e1d3d 0%, #170e0a6e 60%, #0d0806e8 100%),
    linear-gradient(to bottom, #140d0ad4 0%, #160e0a2e 34%, #170e0a78 72%, #100a07 100%),
    var(--library-hall, none) center 52% / cover no-repeat,
    var(--wood-deep);
  pointer-events: none;
}

@media (max-width: 38rem) {
  .library-atmosphere {
    background-position: center, center, center, 49% top;
  }
}
</style>
