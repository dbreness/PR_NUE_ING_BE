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

export type OrderSort =
  | 'createdAt,desc'
  | 'createdAt,asc'
  | 'productName,asc'
  | 'amount,desc'

export interface OrderListParams {
  status?: OrderStatus
  productName?: string
  page: number
  size: number
  sort: OrderSort
}

export interface OrderPage {
  items: Order[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface OrderApiError {
  timestamp: string
  status: number
  message: string
  fieldErrors?: Record<string, string>
}
