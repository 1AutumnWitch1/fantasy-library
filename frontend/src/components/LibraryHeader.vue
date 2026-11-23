<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BookSigil from './shelf/BookSigil.vue'
import { useAccountStore } from '../stores/account'

const route = useRoute()
const account = useAccountStore()
const accountLink = computed(() => ({
  name: 'account',
  query: route.name === 'account' ? route.query : { next: route.fullPath },
}))
void account.restore()
</script>

<template>
  <header class="library-header">
    <div class="header-inner">
      <RouterLink class="library-name" :to="{ name: 'shelf' }">
        <span class="library-seal" aria-hidden="true"><BookSigil kind="compass" /></span>
        幻想图书馆
      </RouterLink>
      <nav aria-label="主导航">
        <RouterLink class="status-link" :to="{ name: 'status' }">
          <span class="status-mark" aria-hidden="true"></span>
          系统状态
        </RouterLink>
        <RouterLink class="status-link" :to="accountLink">{{ account.profile ? '个人资料' : '登录' }}</RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped lang="scss">
.library-header {
  position: relative;
  z-index: 2;
  background: linear-gradient(#22251be8, #141910f6), var(--leather-grain) center / 13rem;
  border-bottom: 1px solid var(--brass-line);
  box-shadow: 0 2px 0 #100d0b, 0 4px 18px #090604a6;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: calc(var(--library-header-height) - 1px);
  max-width: 90rem;
  margin-inline: auto;
  padding-inline: clamp(1.15rem, 5vw, 4.5rem);
}

a {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  text-decoration: none;
  color: #ddc6a1;

  &:hover, &[aria-current='page'] { color: #f6e3b8; }
  &:focus-visible { outline: 2px solid var(--brass); outline-offset: 4px; }
}

.library-name {
  gap: 0.72rem;
  font-size: 0.98rem;
  font-family: var(--display-font);
  font-weight: 700;
  letter-spacing: 0.11em;
}

.library-seal {
  display: grid;
  width: 1.8rem;
  height: 1.8rem;
  place-items: center;
  color: var(--brass);
  border: 1px solid #87663d;
  border-radius: 50%;
  font-size: 1.24rem;
}

nav { display: flex; align-items: center; gap: clamp(.55rem, 2vw, 1.5rem); }
@media(max-width: 26rem) {
  .header-inner { padding-inline: .55rem; }
  .library-name { gap: .35rem; letter-spacing: .04em; }
  .status-mark { display: none; }
}
.status-link {
  gap: 0.48rem;
  color: #c5ad86;
  font-size: 0.86rem;
  letter-spacing: 0.05em;
}

.status-mark {
  width: 0.34rem;
  height: 0.34rem;
  transform: rotate(45deg);
  border: 1px solid #b9975f;
}
</style>
