import { request } from './request'

export interface FavoriteItem {
  favoriteId: number
  targetType: 1 | 2
  targetId: number
  targetName: string
  subtitle: string
  imageUrl?: string | null
  price?: number | null
  shopId: number | null
  canteenId: number | null
  createTime: string
}

export interface FavoritePage {
  records: FavoriteItem[]
  total: number
  page: number
  size: number
}

export const addFavorite = (targetType: 1 | 2, targetId: number) =>
  request.post('/favorites', { targetType, targetId })
export const removeFavorite = (targetType: 1 | 2, targetId: number) =>
  request.delete(`/favorites/${targetType}/${targetId}`)
export const listFavorites = (params: { targetType?: 1 | 2; page?: number; size?: number }) =>
  request.get<never, FavoritePage>('/favorites', { params })
export const getFavoriteIds = (targetType: 1 | 2) =>
  request.get<never, number[]>('/favorites/ids', { params: { targetType } })
