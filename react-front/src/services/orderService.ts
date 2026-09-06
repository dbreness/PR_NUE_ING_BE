import axios from 'axios'
import { httpClient } from './httpClient'
import type { CreateOrderRequest, Order, OrderApiError } from '../types/order'

export async function createOrder(payload: CreateOrderRequest): Promise<Order> {
  const response = await httpClient.post<Order>('/orders', payload)
  return response.data
}

export function getOrderErrorMessage(error: unknown): string {
  if (axios.isAxiosError<OrderApiError>(error)) {
    const apiError = error.response?.data
    const validationMessages = Object.values(apiError?.fieldErrors ?? {})

    if (validationMessages.length > 0) {
      return validationMessages.join('. ')
    }

    return apiError?.message ?? 'No fue posible comunicarse con el servicio de órdenes'
  }

  return 'No fue posible crear la orden'
}
