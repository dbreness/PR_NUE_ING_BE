import { useCallback, useEffect, useRef, useState } from 'react'
import { getOrderErrorMessage, getOrders } from '../services/orderService'
import type { OrderListParams, OrderPage } from '../types/order'

const EMPTY_PAGE: OrderPage = {
  items: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
}

export function useOrders(params: OrderListParams, refreshToken: number) {
  const [ordersPage, setOrdersPage] = useState<OrderPage>(EMPTY_PAGE)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryToken, setRetryToken] = useState(0)
  const requestId = useRef(0)

  const retry = useCallback(() => {
    setRetryToken((current) => current + 1)
  }, [])

  useEffect(() => {
    const currentRequestId = ++requestId.current

    queueMicrotask(() => {
      if (currentRequestId === requestId.current) {
        setIsLoading(true)
        setError(null)
      }
    })

    getOrders(params)
      .then((response) => {
        if (currentRequestId === requestId.current) {
          setOrdersPage(response)
        }
      })
      .catch((requestError: unknown) => {
        if (currentRequestId === requestId.current) {
          setError(getOrderErrorMessage(requestError, 'No fue posible cargar las órdenes'))
        }
      })
      .finally(() => {
        if (currentRequestId === requestId.current) {
          setIsLoading(false)
        }
      })
  }, [params, refreshToken, retryToken])

  return { ordersPage, isLoading, error, retry }
}
