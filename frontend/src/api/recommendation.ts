import { request } from './request'
import type { Dish } from './data'

export interface RecommendedDish {
  dish: Dish
  score: number
  reason: string
}

export interface RecommendationResult {
  id: number
  requestNo: string
  mealType: number
  budgetMax?: number | null
  source: number
  isFallback: number
  costMs: number
  items: RecommendedDish[]
}

export interface RecommendationFeedback {
  dishId: number
  feedbackType: 1 | 2 | 4
  reasonTag?: string
  comment?: string
}

export const createRecommendation = (body: { mealType: number; limit?: number }) =>
  request.post<never, RecommendationResult>('/recommendations', body)
export const sendRecommendationFeedback = (id: number, body: RecommendationFeedback) =>
  request.post(`/recommendations/${id}/feedback`, body)
export const getNextRecommendation = (id: number, dishId: number) =>
  request.post<never, RecommendationResult>(`/recommendations/${id}/next`, {
    dishId,
    feedbackType: 3,
    reasonTag: '换一组',
  })
