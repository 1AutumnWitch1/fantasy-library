import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'
import * as api from '../api/account'

export const useAccountStore = defineStore('account', () => {
  const profile = shallowRef<api.Profile | null>(null)
  const status = ref<'idle' | 'loading' | 'guest' | 'signed-in' | 'error'>('idle')
  const error = ref('')
  let pending: Promise<void> | null = null
  let revision = 0

  function signedOut() {
    revision++
    pending = null
    profile.value = null
    status.value = 'guest'
    error.value = ''
  }

  function restore(force = false) {
    if (!force && pending) return pending
    if (!force && status.value !== 'idle' && status.value !== 'error') return Promise.resolve()
    const request = ++revision
    status.value = 'loading'
    error.value = ''
    pending = api.getProfile().then(value => {
      if (request !== revision) return
      profile.value = value
      status.value = 'signed-in'
    }).catch(reason => {
      if (request !== revision) return
      if (api.needsLogin(reason)) signedOut()
      else {
        profile.value = null
        status.value = 'error'
        error.value = api.accountError(reason)
      }
    }).finally(() => {
      if (request === revision) pending = null
    })
    return pending
  }

  async function signIn(credentials: api.Credentials) {
    await api.login(credentials)
    await restore(true)
    if (!profile.value) throw new Error(error.value || '无法读取登录状态')
  }

  async function signOut() {
    await api.logout()
    signedOut()
  }

  async function saveProfile(form: { displayName: string; bio: string }) {
    const id = profile.value?.id
    const updated = await api.updateProfile(form)
    if (id === profile.value?.id) profile.value = updated
  }

  async function saveAvatar(file: File | null) {
    const owner = profile.value?.id
    const updated = file ? await api.uploadAvatar(file) : await api.removeAvatar()
    if (profile.value && owner === profile.value.id) {
      profile.value = { ...profile.value, avatarKey: updated.avatarKey }
    }
  }

  return { profile, status, error, restore, signIn, signOut, signedOut, saveProfile, saveAvatar }
})
