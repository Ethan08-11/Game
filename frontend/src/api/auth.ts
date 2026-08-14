/**
 * 认证相关 API
 */

import { apiCall } from './client'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
}

export interface MeInfo {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  email: string | null
  phone: string | null
  level: number
  exp: number
  winCount: number
  loseCount: number
  drawCount: number
  money: number
  points: number
}

export interface AuthResult {
  token: string
  refreshToken: string
  user: UserInfo
}

export async function login(params: LoginParams): Promise<AuthResult> {
  return apiCall('/auth/login', { method: 'POST', body: params })
}

export async function register(params: RegisterParams): Promise<AuthResult> {
  return apiCall('/auth/register', { method: 'POST', body: params })
}

export async function refreshAuth(refreshToken: string): Promise<AuthResult> {
  return apiCall('/auth/refresh', {
    method: 'POST',
    body: { refreshToken },
  })
}

export async function logout(): Promise<void> {
  return apiCall('/auth/logout', {
    method: 'POST',
    body: { refreshToken: localStorage.getItem('refreshToken') || '' },
  })
}

export async function getMe(): Promise<MeInfo> {
  return apiCall('/auth/me', { method: 'GET' })
}
