import { useCallback, useEffect, useRef, useState } from 'react'
import { getOrder, getOrderErrorMessage } from '../services/orderService'
import type { Order } from '../types/order'

export function useOrderDetail(orderId: number | null) {
  const [order, setOrder] = useState<Order | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryToken, setRetryToken] = useState(0)
  const requestId = useRef(0)
  const pendingOrderId = useRef<number | null>(null)

  const retry = useCallback(() => {
    setRetryToken((current) => current + 1)
  }, [])

  useEffect(() => {
    const currentRequestId = ++requestId.current

    if (orderId === null) {
      pendingOrderId.current = null
      queueMicrotask(() => {
        if (currentRequestId === requestId.current) {
          setOrder(null)
          setIsLoading(false)
          setError(null)
        }
      })

      return () => {
        if (currentRequestId === requestId.current) {
          requestId.current += 1
        }
      }
    }

    if (pendingOrderId.current !== orderId) {
      pendingOrderId.current = null
    }

    const abortController = new AbortController()
    let pollingTimeout: ReturnType<typeof setTimeout> | undefined

    function schedulePolling() {
      pollingTimeout = setTimeout(() => {
        if (currentRequestId === requestId.current) {
          setRetryToken((current) => current + 1)
        }
      }, 2000)
    }

    queueMicrotask(() => {
      if (currentRequestId === requestId.current) {
        setOrder((current) => current?.id === orderId ? current : null)
        setIsLoading(true)
        setError(null)
      }
    })

    getOrder(orderId, abortController.signal)
      .then((response) => {
        if (currentRequestId === requestId.current) {
          setOrder(response)

          if (response.status === 'PENDIENTE') {
            pendingOrderId.current = orderId
            schedulePolling()
          } else {
            pendingOrderId.current = null
          }
        }
      })
      .catch((requestError: unknown) => {
        if (currentRequestId === requestId.current) {
          setError(getOrderErrorMessage(requestError, 'No fue posible cargar el detalle de la orden'))

          if (pendingOrderId.current === orderId) {
            schedulePolling()
          }
        }
      })
      .finally(() => {
        if (currentRequestId === requestId.current) {
          setIsLoading(false)
        }
      })

    return () => {
      abortController.abort()

      if (pollingTimeout) {
        clearTimeout(pollingTimeout)
      }

      if (currentRequestId === requestId.current) {
        requestId.current += 1
      }
    }
  }, [orderId, retryToken])

  return { order, isLoading, error, retry }
}
