import { act, fireEvent, render, screen } from '@testing-library/react'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getOrder } from '../services/orderService'
import type { Order } from '../types/order'
import { OrderDetailModal } from './OrderDetailModal'

vi.mock('../services/orderService', async (importOriginal) => {
  const original = await importOriginal<typeof import('../services/orderService')>()
  return { ...original, getOrder: vi.fn() }
})

const getOrderMock = vi.mocked(getOrder)
const paidOrder: Order = {
  id: 25,
  productName: 'Monitor',
  quantity: 2,
  amount: 199.99,
  status: 'PAGADO',
  createdAt: '2026-09-06T12:00:00Z',
  updatedAt: '2026-09-06T12:00:02Z',
}

function ModalHarness() {
  const [orderId, setOrderId] = useState<number | null>(null)

  return (
    <>
      <button type="button" onClick={() => setOrderId(25)}>Abrir detalle</button>
      <OrderDetailModal orderId={orderId} onClose={() => setOrderId(null)} />
    </>
  )
}

describe('OrderDetailModal', () => {
  beforeEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
    getOrderMock.mockResolvedValue(paidOrder)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('consulta y muestra el detalle público de una orden', async () => {
    render(<OrderDetailModal orderId={25} onClose={vi.fn()} />)

    expect(await screen.findByText('Monitor')).toBeInTheDocument()
    expect(screen.getByText('Orden #25')).toBeInTheDocument()
    expect(screen.getByText('PAGADO')).toBeInTheDocument()
    expect(getOrderMock).toHaveBeenCalledWith(25, expect.any(AbortSignal))
  })

  it('cierra el modal desde el botón y con Escape', async () => {
    const onClose = vi.fn()
    render(<OrderDetailModal orderId={25} onClose={onClose} />)
    await screen.findByText('Monitor')

    fireEvent.click(screen.getByRole('button', { name: 'Cerrar detalle' }))
    expect(onClose).toHaveBeenCalledTimes(1)

    fireEvent.keyDown(window, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(2)
  })

  it('administra el foco durante la navegación por teclado', async () => {
    render(<ModalHarness />)
    const trigger = screen.getByRole('button', { name: 'Abrir detalle' })
    trigger.focus()
    fireEvent.click(trigger)

    const closeButton = await screen.findByRole('button', { name: 'Cerrar detalle' })
    expect(closeButton).toHaveFocus()

    fireEvent.keyDown(window, { key: 'Tab' })
    expect(closeButton).toHaveFocus()

    fireEvent.keyDown(window, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  it('cancela la consulta pendiente cuando se cierra el modal', async () => {
    getOrderMock.mockReturnValueOnce(new Promise(() => undefined))
    const { rerender } = render(<OrderDetailModal orderId={25} onClose={vi.fn()} />)

    await act(async () => {
      await Promise.resolve()
    })
    const signal = getOrderMock.mock.calls[0][1]
    expect(signal?.aborted).toBe(false)

    rerender(<OrderDetailModal orderId={null} onClose={vi.fn()} />)
    expect(signal?.aborted).toBe(true)
  })

  it('reintenta la consulta cuando falla el detalle', async () => {
    getOrderMock.mockRejectedValueOnce(new Error('sin conexión')).mockResolvedValueOnce(paidOrder)
    render(<OrderDetailModal orderId={25} onClose={vi.fn()} />)

    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible cargar el detalle de la orden')
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Monitor')).toBeInTheDocument()
    expect(getOrderMock).toHaveBeenCalledTimes(2)
  })

  it('actualiza cada dos segundos cuando la orden sigue pendiente', async () => {
    vi.useFakeTimers()
    getOrderMock
      .mockResolvedValueOnce({ ...paidOrder, status: 'PENDIENTE' })
      .mockResolvedValueOnce({ ...paidOrder, status: 'PAGADO' })

    render(<OrderDetailModal orderId={25} onClose={vi.fn()} />)

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

    expect(getOrderMock).toHaveBeenCalledTimes(2)
    expect(screen.getByText('PAGADO')).toBeInTheDocument()
  })

  it('continúa actualizando una orden pendiente después de un error transitorio', async () => {
    vi.useFakeTimers()
    getOrderMock
      .mockResolvedValueOnce({ ...paidOrder, status: 'PENDIENTE' })
      .mockRejectedValueOnce(new Error('sin conexión'))
      .mockResolvedValueOnce({ ...paidOrder, status: 'PAGADO' })

    render(<OrderDetailModal orderId={25} onClose={vi.fn()} />)

    await act(async () => {
      await Promise.resolve()
      await Promise.resolve()
    })

    await act(async () => {
      vi.advanceTimersByTime(2000)
      await Promise.resolve()
      await Promise.resolve()
    })
    expect(screen.getByRole('alert')).toHaveTextContent('No fue posible cargar el detalle de la orden')

    await act(async () => {
      vi.advanceTimersByTime(2000)
      await Promise.resolve()
      await Promise.resolve()
    })

    expect(getOrderMock).toHaveBeenCalledTimes(3)
    expect(screen.getByText('PAGADO')).toBeInTheDocument()
  })
})
