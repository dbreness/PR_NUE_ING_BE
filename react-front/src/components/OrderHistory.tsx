import { useCallback, useMemo, useState, type FormEvent } from 'react'
import { useOrders } from '../hooks/useOrders'
import type { OrderSort, OrderStatus } from '../types/order'
import { OrderDetailModal } from './OrderDetailModal'

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

interface OrderHistoryProps {
  refreshToken?: number
}

export function OrderHistory({ refreshToken = 0 }: OrderHistoryProps) {
  const [productInput, setProductInput] = useState('')
  const [productName, setProductName] = useState('')
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [sort, setSort] = useState<OrderSort>('createdAt,desc')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null)

  const query = useMemo(
    () => ({
      ...(productName ? { productName } : {}),
      ...(status ? { status } : {}),
      page,
      size,
      sort,
    }),
    [page, productName, size, sort, status],
  )
  const { ordersPage, isLoading, error, retry } = useOrders(query, refreshToken)

  function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setProductName(productInput.trim())
  }

  const handleDetailClose = useCallback(() => {
    setSelectedOrderId(null)
  }, [])

  const currentPage = ordersPage.totalPages === 0 ? 1 : ordersPage.page + 1
  const displayedTotalPages = Math.max(ordersPage.totalPages, 1)

  return (
    <section
      className="rounded-2xl bg-white p-6 shadow-sm ring-1 ring-slate-200"
      aria-labelledby="order-history-title"
    >
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 id="order-history-title" className="text-2xl font-semibold">Historial de órdenes</h2>
          <p className="mt-2 text-sm text-slate-600">
            Consulta los resultados aplicando filtros y ordenamiento en el servidor.
          </p>
        </div>
        <p className="text-sm font-medium text-slate-600" aria-live="polite">
          {ordersPage.totalElements} {ordersPage.totalElements === 1 ? 'orden' : 'órdenes'}
        </p>
      </div>

      <form className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4" onSubmit={handleFilterSubmit}>
        <label className="grid gap-2 text-sm font-medium text-slate-700">
          Producto
          <input
            className="rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            value={productInput}
            onChange={(event) => setProductInput(event.target.value)}
            placeholder="Buscar por nombre"
          />
        </label>

        <label className="grid gap-2 text-sm font-medium text-slate-700">
          Estado
          <select
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            value={status}
            onChange={(event) => {
              setPage(0)
              setStatus(event.target.value as OrderStatus | '')
            }}
          >
            <option value="">Todos</option>
            <option value="PENDIENTE">Pendiente</option>
            <option value="PAGADO">Pagado</option>
            <option value="FALLO_PAGO">Fallo de pago</option>
          </select>
        </label>

        <label className="grid gap-2 text-sm font-medium text-slate-700">
          Ordenar por
          <select
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
            value={sort}
            onChange={(event) => {
              setPage(0)
              setSort(event.target.value as OrderSort)
            }}
          >
            <option value="createdAt,desc">Más recientes</option>
            <option value="createdAt,asc">Más antiguas</option>
            <option value="productName,asc">Producto A–Z</option>
            <option value="amount,desc">Mayor monto</option>
          </select>
        </label>

        <div className="flex items-end">
          <button
            className="w-full rounded-lg bg-slate-800 px-4 py-2 font-semibold text-white transition hover:bg-slate-900 focus:outline-none focus:ring-2 focus:ring-slate-300"
            type="submit"
          >
            Aplicar filtros
          </button>
        </div>
      </form>

      {error && (
        <div className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
          <p>{error}</p>
          <button className="mt-2 font-semibold underline" type="button" onClick={retry}>
            Reintentar
          </button>
        </div>
      )}

      <div className="mt-6 overflow-x-auto">
        <table className="w-full min-w-[760px] border-collapse text-left text-sm">
          <caption className="sr-only">Órdenes registradas</caption>
          <thead>
            <tr className="border-b border-slate-200 text-slate-600">
              <th className="px-3 py-3 font-semibold" scope="col">Orden</th>
              <th className="px-3 py-3 font-semibold" scope="col">Producto</th>
              <th className="px-3 py-3 text-right font-semibold" scope="col">Cantidad</th>
              <th className="px-3 py-3 text-right font-semibold" scope="col">Monto</th>
              <th className="px-3 py-3 font-semibold" scope="col">Estado</th>
              <th className="px-3 py-3 font-semibold" scope="col">Creada</th>
              <th className="px-3 py-3 font-semibold" scope="col">Actualizada</th>
              <th className="px-3 py-3 font-semibold" scope="col">Detalle</th>
            </tr>
          </thead>
          <tbody className={isLoading ? 'opacity-50' : undefined} aria-busy={isLoading}>
            {ordersPage.items.map((order) => (
              <tr className="border-b border-slate-100 last:border-0" key={order.id}>
                <td className="px-3 py-4 font-semibold text-slate-900">#{order.id}</td>
                <td className="px-3 py-4 text-slate-700">{order.productName}</td>
                <td className="px-3 py-4 text-right text-slate-700">{order.quantity}</td>
                <td className="px-3 py-4 text-right text-slate-700">
                  {amountFormatter.format(order.amount)}
                </td>
                <td className="px-3 py-4">
                  <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusStyles[order.status]}`}>
                    {order.status.replace('_', ' ')}
                  </span>
                </td>
                <td className="px-3 py-4 text-slate-600">{dateFormatter.format(new Date(order.createdAt))}</td>
                <td className="px-3 py-4 text-slate-600">{dateFormatter.format(new Date(order.updatedAt))}</td>
                <td className="px-3 py-4">
                  <button
                    className="rounded-lg border border-slate-300 px-3 py-2 font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-100"
                    onClick={() => setSelectedOrderId(order.id)}
                    type="button"
                  >
                    Ver detalle
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isLoading && ordersPage.items.length === 0 && (
        <p className="py-10 text-center text-sm text-slate-500" role="status">Cargando órdenes…</p>
      )}

      {!isLoading && !error && ordersPage.items.length === 0 && (
        <p className="py-10 text-center text-sm text-slate-500">No hay órdenes que coincidan con la búsqueda.</p>
      )}

      <div className="mt-6 flex flex-col gap-4 border-t border-slate-200 pt-5 sm:flex-row sm:items-center sm:justify-between">
        <label className="flex items-center gap-2 text-sm text-slate-600">
          Mostrar
          <select
            aria-label="Órdenes por página"
            className="rounded-lg border border-slate-300 bg-white px-2 py-1.5"
            value={size}
            onChange={(event) => {
              setPage(0)
              setSize(Number(event.target.value))
            }}
          >
            <option value="5">5</option>
            <option value="10">10</option>
            <option value="20">20</option>
          </select>
          por página
        </label>

        <nav className="flex items-center gap-3" aria-label="Paginación de órdenes">
          <button
            className="rounded-lg border border-slate-300 px-3 py-2 font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
            type="button"
            disabled={isLoading || page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Anterior
          </button>
          <span className="text-sm text-slate-600">
            Página {currentPage} de {displayedTotalPages}
          </span>
          <button
            className="rounded-lg border border-slate-300 px-3 py-2 font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
            type="button"
            disabled={isLoading || page + 1 >= ordersPage.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </button>
        </nav>
      </div>

      <OrderDetailModal
        orderId={selectedOrderId}
        onClose={handleDetailClose}
      />
    </section>
  )
}
