import { useEffect, useRef } from 'react'
import { useOrderDetail } from '../hooks/useOrderDetail'
import type { Order, OrderStatus } from '../types/order'

const amountFormatter = new Intl.NumberFormat('es-CR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const dateFormatter = new Intl.DateTimeFormat('es-CR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const statusStyles: Record<OrderStatus, string> = {
  PENDIENTE: 'bg-amber-100 text-amber-800',
  PAGADO: 'bg-emerald-100 text-emerald-800',
  FALLO_PAGO: 'bg-red-100 text-red-800',
}

interface OrderDetailModalProps {
  orderId: number | null
  onClose: () => void
}

function DetailRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-slate-200 px-4 py-3">
      <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className="mt-1 text-sm font-medium text-slate-900">{value}</dd>
    </div>
  )
}

function OrderSummary({ order }: { order: Order }) {
  return (
    <dl className="mt-5 grid gap-3 sm:grid-cols-2">
      <DetailRow label="Producto" value={order.productName} />
      <DetailRow label="Cantidad" value={order.quantity} />
      <DetailRow label="Monto" value={amountFormatter.format(order.amount)} />
      <div className="rounded-lg border border-slate-200 px-4 py-3">
        <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Estado</dt>
        <dd className="mt-2">
          <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusStyles[order.status]}`}>
            {order.status.replace('_', ' ')}
          </span>
        </dd>
      </div>
      <DetailRow label="Creada" value={dateFormatter.format(new Date(order.createdAt))} />
      <DetailRow label="Actualizada" value={dateFormatter.format(new Date(order.updatedAt))} />
    </dl>
  )
}

export function OrderDetailModal({ orderId, onClose }: OrderDetailModalProps) {
  const { order, isLoading, error, retry } = useOrderDetail(orderId)
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const previousFocusRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
        return
      }

      if (event.key === 'Tab') {
        const focusableElements = Array.from(
          dialogRef.current?.querySelectorAll<HTMLElement>(
            'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
          ) ?? [],
        )

        if (focusableElements.length === 0) {
          event.preventDefault()
          dialogRef.current?.focus()
          return
        }

        const firstElement = focusableElements[0]
        const lastElement = focusableElements[focusableElements.length - 1]

        if (event.shiftKey && document.activeElement === firstElement) {
          event.preventDefault()
          lastElement.focus()
        } else if (!event.shiftKey && document.activeElement === lastElement) {
          event.preventDefault()
          firstElement.focus()
        }
      }
    }

    if (orderId !== null) {
      previousFocusRef.current = document.activeElement as HTMLElement | null
      closeButtonRef.current?.focus()
      window.addEventListener('keydown', handleKeyDown)
    }

    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      previousFocusRef.current?.focus()
      previousFocusRef.current = null
    }
  }, [onClose, orderId])

  if (orderId === null) {
    return null
  }

  return (
    <div
      aria-labelledby="order-detail-title"
      aria-modal="true"
      className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-slate-950/60 p-4"
      ref={dialogRef}
      role="dialog"
      tabIndex={-1}
    >
      <div className="max-h-[calc(100dvh-2rem)] min-w-0 w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-4 shadow-xl sm:p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-widest text-blue-700">
              Detalle de orden
            </p>
            <h2 id="order-detail-title" className="mt-2 text-2xl font-semibold">
              Orden #{orderId}
            </h2>
          </div>
          <button
            aria-label="Cerrar detalle"
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-100"
            onClick={onClose}
            ref={closeButtonRef}
            type="button"
          >
            Cerrar
          </button>
        </div>

        {isLoading && !order && (
          <p className="py-10 text-center text-sm text-slate-500" role="status">
            Cargando detalle...
          </p>
        )}

        {error && (
          <div className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
            <p>{error}</p>
            <button className="mt-2 font-semibold underline" type="button" onClick={retry}>
              Reintentar
            </button>
          </div>
        )}

        {order && <OrderSummary order={order} />}

        {order?.status === 'PENDIENTE' && (
          <p className="mt-4 text-sm text-slate-500" role="status">
            Actualizando estado cada 2 segundos.
          </p>
        )}
      </div>
    </div>
  )
}
