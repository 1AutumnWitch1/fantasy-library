import { apiClient } from './client'

export interface HealthResponse {
  application: 'UP'
  database: 'NOT_CONFIGURED' | 'UP' | 'DOWN'
}

export async function fetchHealth(): Promise<HealthResponse> {
  const { data } = await apiClient.get<Partial<HealthResponse> | null>('/health')

  if (data?.application !== 'UP' ||
      (data.database !== 'NOT_CONFIGURED' && data.database !== 'UP' && data.database !== 'DOWN')) {
    throw new Error('Invalid health response')
  }

  return { application: data.application, database: data.database }
}
