import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getOrder, getOrders } from '../services/orderService'
import type { OrderPage } from '../types/order'
import { OrderHistory } from './OrderHistory'

vi.mock('../services/orderService', async (importOriginal) => {
  const original = await importOriginal<typeof import('../services/orderService')>()
  return { ...original, getOrder: vi.fn(), getOrders: vi.fn() }
})

const getOrdersMock = vi.mocked(getOrders)
const getOrderMock = vi.mocked(getOrder)
const firstPage: OrderPage = {
  items: [
    {
      id: 25,
      productName: 'Monitor',
      quantity: 2,
      amount: 199.99,
      status: 'PAGADO',
      createdAt: '2026-09-06T12:00:00Z',
      updatedAt: '2026-09-06T12:00:02Z',
    },
  ],
  page: 0,
  size: 10,
  totalElements: 12,
  totalPages: 2,
}

describe('OrderHistory', () => {
  beforeEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
    getOrdersMock.mockResolvedValue(firstPage)
    getOrderMock.mockResolvedValue(firstPage.items[0])
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('consulta y muestra la página inicial con los parámetros predeterminados', async () => {
    render(<OrderHistory />)

    expect(await screen.findByText('Monitor')).toBeInTheDocument()
    expect(screen.getByText('#25')).toBeInTheDocument()
    expect(screen.getByText('PAGADO')).toBeInTheDocument()
    expect(screen.getByText('12 órdenes')).toBeInTheDocument()
    expect(
      screen.getByRole('region', { name: 'Tabla de órdenes con desplazamiento horizontal' }),
    ).toHaveAttribute('tabindex', '0')
    expect(getOrdersMock).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: 'createdAt,desc',
    })
  })

  it('envía filtros, ordenamiento y paginación al servidor', async () => {
    render(<OrderHistory />)
    await screen.findByText('Monitor')

    fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'PAGADO' } })
    await waitFor(() => {
      expect(getOrdersMock).toHaveBeenLastCalledWith({
        status: 'PAGADO',
        page: 0,
        size: 10,
        sort: 'createdAt,desc',
      })
    })

    fireEvent.change(screen.getByLabelText('Producto'), { target: { value: '  monitor  ' } })
    fireEvent.change(screen.getByLabelText('Ordenar por'), { target: { value: 'amount,desc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Aplicar filtros' }))

    await waitFor(() => {
      expect(getOrdersMock).toHaveBeenLastCalledWith({
        productName: 'monitor',
        status: 'PAGADO',
        page: 0,
        size: 10,
        sort: 'amount,desc',
      })
    })

    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
    await waitFor(() => {
      expect(getOrdersMock).toHaveBeenLastCalledWith({
        productName: 'monitor',
        status: 'PAGADO',
        page: 1,
        size: 10,
        sort: 'amount,desc',
      })
    })

    fireEvent.change(screen.getByLabelText('Órdenes por página'), { target: { value: '5' } })
    await waitFor(() => {
      expect(getOrdersMock).toHaveBeenLastCalledWith({
        productName: 'monitor',
        status: 'PAGADO',
        page: 0,
        size: 5,
        sort: 'amount,desc',
      })
    })
  })

  it('recarga el historial cuando se crea una orden', async () => {
    const { rerender } = render(<OrderHistory refreshToken={0} />)
    await screen.findByText('Monitor')

    rerender(<OrderHistory refreshToken={1} />)

    await waitFor(() => expect(getOrdersMock).toHaveBeenCalledTimes(2))
  })

  it('informa errores y permite reintentar la consulta', async () => {
    getOrdersMock.mockRejectedValueOnce(new Error('sin conexión')).mockResolvedValueOnce(firstPage)
    render(<OrderHistory />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible cargar las órdenes')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Monitor')).toBeInTheDocument()
    expect(getOrdersMock).toHaveBeenCalledTimes(2)
  })

  it('abre el modal de detalle de la orden seleccionada', async () => {
    render(<OrderHistory />)
    await screen.findByText('Monitor')

    fireEvent.click(screen.getByRole('button', { name: 'Ver detalle' }))

    expect(await screen.findByRole('dialog', { name: 'Orden #25' })).toBeInTheDocument()
  })

  it('recarga cada dos segundos y se recupera de errores transitorios', async () => {
    vi.useFakeTimers()
    getOrdersMock
      .mockResolvedValueOnce({
        ...firstPage,
        items: [{ ...firstPage.items[0], status: 'PENDIENTE' }],
      })
      .mockRejectedValueOnce(new Error('sin conexión'))
      .mockResolvedValueOnce(firstPage)

    render(<OrderHistory />)

    await act(async () => {
      await Promise.resolve()
      await Promise.resolve()
    })
    expect(screen.getByText('PENDIENTE')).toBeInTheDocument()

    await act(async () => {
      vi.advanceTimersByTime(2000)
      await Promise.resolve()
      await Promise.resolve()
    })

    expect(screen.getByRole('alert')).toHaveTextContent('No fue posible cargar las órdenes')

    await act(async () => {
      vi.advanceTimersByTime(2000)
      await Promise.resolve()
      await Promise.resolve()
    })

    expect(getOrdersMock).toHaveBeenCalledTimes(3)
    expect(screen.getByText('PAGADO')).toBeInTheDocument()
  })
})
