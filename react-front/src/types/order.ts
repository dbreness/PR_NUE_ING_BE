export type OrderStatus = 'PENDIENTE' | 'PAGADO' | 'FALLO_PAGO'

export interface Order {
  id: number
  productName: string
  quantity: number
  amount: number
  status: OrderStatus
  createdAt: string
  updatedAt: string
}

export interface CreateOrderRequest {
  productName: string
  quantity: number
  amount: number
  encryptedCardData: string
}

export interface OrderApiError {
  timestamp: string
  status: number
  message: string
  fieldErrors?: Record<string, string>
}
