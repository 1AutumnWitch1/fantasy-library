import { afterEach, expect, it, vi } from 'vitest'
import { register } from '../src/api/account'
import { apiClient } from '../src/api/client'

afterEach(() => vi.restoreAllMocks())

it('sends the server-provided registration proof without Axios replacing it from a cookie', async () => {
  const request = vi.spyOn(apiClient, 'request')
    .mockResolvedValueOnce({ data: { code: 'OK', data: { headerName: 'X-XSRF-TOKEN', token: 'fresh-proof' } } })
    .mockResolvedValueOnce({ data: { code: 'OK' } })
  const details = { username: 'test_account', password: 'test-input', displayName: '测试' }

  await register(details)

  expect(request).toHaveBeenNthCalledWith(1, { url: '/auth/csrf' })
  expect(request).toHaveBeenNthCalledWith(2, {
    method: 'POST', url: '/auth/register', data: details, withXSRFToken: false,
    headers: { 'X-XSRF-TOKEN': 'fresh-proof' },
  })
})
