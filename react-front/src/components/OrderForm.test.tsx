import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createOrder } from '../services/orderService'
import { encryptCardDataForTransport } from '../utils/rsaEncryption'
import { OrderForm } from './OrderForm'

vi.mock('../services/orderService', async (importOriginal) => {
  const original = await importOriginal<typeof import('../services/orderService')>()
  return { ...original, createOrder: vi.fn() }
})

vi.mock('../utils/rsaEncryption', () => ({
  encryptCardDataForTransport: vi.fn(),
}))

const createOrderMock = vi.mocked(createOrder)
const encryptCardDataMock = vi.mocked(encryptCardDataForTransport)

describe('OrderForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    encryptCardDataMock.mockResolvedValue('payload-cifrado')
    createOrderMock.mockResolvedValue({
      id: 25,
      productName: 'Monitor',
      quantity: 2,
      amount: 199.99,
      status: 'PENDIENTE',
      createdAt: '2026-09-06T12:00:00Z',
      updatedAt: '2026-09-06T12:00:00Z',
    })
  })

  it('envía únicamente el payload cifrado y limpia los datos de tarjeta', async () => {
    render(<OrderForm />)

    fireEvent.change(screen.getByLabelText('Producto'), { target: { value: 'Monitor' } })
    fireEvent.change(screen.getByLabelText('Cantidad'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Monto'), { target: { value: '199.99' } })
    fireEvent.change(screen.getByLabelText('Número de tarjeta'), { target: { value: '4111111111111111' } })
    fireEvent.change(screen.getByLabelText('Expiración'), { target: { value: '12/30' } })
    fireEvent.change(screen.getByLabelText('CVV'), { target: { value: '123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear orden' }))

    await waitFor(() => {
      expect(createOrderMock).toHaveBeenCalledWith({
        productName: 'Monitor',
        quantity: 2,
        amount: 199.99,
        encryptedCardData: 'payload-cifrado',
      })
    })

    const transportedPayload = JSON.stringify(createOrderMock.mock.calls[0][0])
    expect(transportedPayload).not.toContain('4111111111111111')
    expect(transportedPayload).not.toContain('12/30')
    expect(transportedPayload).not.toContain('123')
    expect(screen.getByLabelText('Número de tarjeta')).toHaveValue('')
    expect(screen.getByLabelText('Expiración')).toHaveValue('')
    expect(screen.getByLabelText('CVV')).toHaveValue('')
    expect(screen.getByRole('status')).toHaveTextContent('Orden #25 creada con estado PENDIENTE.')
  })

  it('no envía la orden cuando falla el cifrado', async () => {
    encryptCardDataMock.mockRejectedValue(new Error('No fue posible cargar la clave pública de pago'))
    render(<OrderForm />)

    fireEvent.change(screen.getByLabelText('Producto'), { target: { value: 'Monitor' } })
    fireEvent.change(screen.getByLabelText('Número de tarjeta'), { target: { value: '4111111111111111' } })
    fireEvent.change(screen.getByLabelText('Expiración'), { target: { value: '12/30' } })
    fireEvent.change(screen.getByLabelText('CVV'), { target: { value: '123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear orden' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No fue posible cargar la clave pública de pago')
    expect(createOrderMock).not.toHaveBeenCalled()
  })
})
