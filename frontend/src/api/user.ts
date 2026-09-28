import { request } from './request'

export interface AuthResult {
  token: string
  userId: number
  username: string
  nickname: string
  role: number
}

export interface TastePreference {
  maxSpiceLevel: number
  budgetMin?: number
  budgetMax?: number
  likeTagIds: number[]
  dislikeTagIds: number[]
  dietType: number
  profileSummary?: string
  questionnaireVersion?: string
}

export interface TasteProfileItem {
  tagId: number
  tagName: string
  tagType: number
  weight: number
  source: number
}

export const login = (username: string, password: string) =>
  request.post<never, AuthResult>('/auth/login', { username, password })
export const register = (username: string, password: string, nickname: string) =>
  request.post<never, AuthResult>('/auth/register', { username, password, nickname })
export const logout = () => request.post('/auth/logout')
export const getPreference = () => request.get<never, TastePreference>('/preferences/me')
export const savePreference = (body: TastePreference) =>
  request.put<never, TastePreference>('/preferences/me', body)
export const getTasteProfile = () =>
  request.get<never, TasteProfileItem[]>('/preferences/me/profile')
