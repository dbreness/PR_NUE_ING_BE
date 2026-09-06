import { useCallback, useState } from 'react'
import { createOrder, getOrderErrorMessage } from '../services/orderService'
import type { CreateOrderRequest, Order } from '../types/order'

export function useCreateOrder() {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submitOrder = useCallback(async (payload: CreateOrderRequest): Promise<Order | null> => {
    setIsSubmitting(true)
    setError(null)

    try {
      return await createOrder(payload)
    } catch (requestError) {
      setError(getOrderErrorMessage(requestError))
      return null
    } finally {
      setIsSubmitting(false)
    }
  }, [])

  return { submitOrder, isSubmitting, error }
}
