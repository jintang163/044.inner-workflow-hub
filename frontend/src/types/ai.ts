export interface AiFactorVO {
  key: string
  value: string
  weight: number
}

export interface AiRecommendationVO {
  id: number
  taskId: number
  approveProbability: number
  recommendedAction: number
  recommendedActionName: string
  reason: string
  factors: AiFactorVO[]
  modelVersion: string
  inferenceMs: number
  adopted: number
  adoptedName: string
}

export interface AiStatsVO {
  totalTrainedSamples: number
  lastTrainingTime: string
  currentModelVersion: string
  accuracy: number
  featureImportance: Record<string, number>
  adoptionRate: number
  totalRecommendations: number
}
