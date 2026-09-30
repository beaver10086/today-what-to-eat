import { request } from './request'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface Canteen {
  id: number
  canteenName: string
  campus: string
  location?: string | null
  status: number
  description?: string | null
  coverUrl?: string | null
  openTime?: string | null
  closeTime?: string | null
}

export interface Shop {
  id: number
  canteenId: number
  shopName: string
  locationDesc?: string | null
  cuisine?: string | null
  avgPrice?: number | null
  queueHeat?: number | null
  status: number
  description?: string | null
  openTime?: string | null
  closeTime?: string | null
  coverUrl?: string | null
  sortOrder?: number
}

export interface Dish {
  id: number
  shopId: number
  shopName: string
  canteenId: number
  canteenName: string
  dishName: string
  price: number
  category: number
  mealType: number
  spiceLevel: number
  description?: string | null
  imageUrl?: string | null
  isSignature: number
  isAvailable: number
  rating: number
  calorie?: number | null
  takeoutSuitability?: number | null
  dataSource?: string | null
}

export interface Tag {
  id: number
  tagName: string
  tagType: number
  sortOrder?: number
}

export interface DishFilters {
  page?: number
  size?: number
  canteenId?: number
  shopId?: number
  category?: number
  spiceLevel?: number
  mealType?: number
  tagId?: number
  minPrice?: number
  maxPrice?: number
  keyword?: string
}

export interface SearchIntent {
  rawQuery: string
  budgetMax?: number | null
  maxSpiceLevel?: number | null
  canteenKeyword?: string | null
  shopKeyword?: string | null
  mealType?: number | null
  category?: number | null
  keyword?: string | null
  nearby: boolean
}

export interface DiscoveryResult {
  conditions: SearchIntent
  result: PageResult<Dish>
  notice: string
}

export interface AssistantAnswer {
  answer: string
  conditions: SearchIntent
  dishes: Dish[]
}

export const listDishes = (params: DishFilters) =>
  request.get<never, PageResult<Dish>>('/dishes', { params })
export const listAdminDishes = (page: number) =>
  request.get<never, PageResult<Dish>>('/dishes/admin', { params: { page, size: 100 } })
export const getRandomDish = (params: DishFilters) =>
  request.get<never, Dish>('/dishes/random', { params })
export const getDish = (id: number) => request.get<never, Dish>(`/dishes/${id}`)
export const listCanteens = () =>
  request.get<never, PageResult<Canteen>>('/canteens', { params: { size: 100 } })
export const getCanteen = (id: number) => request.get<never, Canteen>(`/canteens/${id}`)
export const listShops = async (canteenId?: number) => {
  const first = await request.get<never, PageResult<Shop>>('/shops', { params: { page: 1, size: 100 } })
  // ponytail: keep campus-scale shop options in memory; paginate the picker if this grows much larger.
  const rest = await Promise.all(
    Array.from({ length: Math.max(0, Math.ceil(first.total / 100) - 1) }, (_, index) =>
      request.get<never, PageResult<Shop>>('/shops', { params: { page: index + 2, size: 100 } }),
    ),
  )
  const all = [...first.records, ...rest.flatMap((page) => page.records)]
  const records = canteenId ? all.filter((shop) => shop.canteenId === canteenId) : all
  return { ...first, records, total: records.length }
}
export const getShop = (id: number) => request.get<never, Shop>(`/shops/${id}`)
export const listTags = () =>
  request.get<never, PageResult<Tag>>('/tags', { params: { size: 100 } })
export const searchByNaturalLanguage = (message: string, page = 1) =>
  request.post<never, DiscoveryResult>('/assistant/search', { message, page })
export const askAssistant = (message: string) =>
  request.post<never, AssistantAnswer>('/assistant/ask', { message })

export const createCanteen = (body: Partial<Canteen>) => request.post('/canteens', body)
export const updateCanteen = (id: number, body: Partial<Canteen>) =>
  request.put(`/canteens/${id}`, body)
export const deleteCanteen = (id: number) => request.delete(`/canteens/${id}`)
export const createShop = (body: Partial<Shop>) => request.post('/shops', body)
export const updateShop = (id: number, body: Partial<Shop>) => request.put(`/shops/${id}`, body)
export const deleteShop = (id: number) => request.delete(`/shops/${id}`)
export const createDish = (body: Partial<Dish>) =>
  request.post<never, Dish, Partial<Dish>>('/dishes', body)
export const updateDish = (id: number, body: Partial<Dish>) => request.put(`/dishes/${id}`, body)
export const deleteDish = (id: number) => request.delete(`/dishes/${id}`)
export const importDishesCsv = (file: File) => {
  const body = new FormData()
  body.append('file', file)
  return request.post<never, { imported: number }>('/dishes/import', body)
}
export const getDishTags = (id: number) => request.get<never, number[]>(`/dishes/${id}/tags`)
export const setDishTags = (id: number, tagIds: number[]) =>
  request.put(`/dishes/${id}/tags`, tagIds)
export const createTag = (body: Partial<Tag>) => request.post('/tags', body)
export const updateTag = (id: number, body: Partial<Tag>) => request.put(`/tags/${id}`, body)
export const deleteTag = (id: number) => request.delete(`/tags/${id}`)
