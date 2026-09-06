import { useState, type FormEvent } from 'react'
import { useCreateOrder } from '../hooks/useCreateOrder'
import type { CardData } from '../types/card'
import type { Order } from '../types/order'
import { encryptCardDataForTransport } from '../utils/rsaEncryption'

const EMPTY_CARD: CardData = {
  cardNumber: '',
  expiration: '',
  cvv: '',
}

interface OrderFormProps {
  onOrderCreated?: (order: Order) => void
}

export function OrderForm({ onOrderCreated }: OrderFormProps) {
  const [productName, setProductName] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [amount, setAmount] = useState('0.00')
  const [cardData, setCardData] = useState<CardData>(EMPTY_CARD)
  const [isEncrypting, setIsEncrypting] = useState(false)
  const [encryptionError, setEncryptionError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const { submitOrder, isSubmitting, error: requestError } = useCreateOrder()
  const isBusy = isEncrypting || isSubmitting

  function updateCardField(field: keyof CardData, value: string) {
    setCardData((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setEncryptionError(null)
    setSuccessMessage(null)
    setIsEncrypting(true)

    try {
      if (!productName.trim()) {
        throw new Error('El producto es obligatorio')
      }

      if (Object.values(cardData).some((value) => !value.trim())) {
        throw new Error('Todos los datos de la tarjeta son obligatorios')
      }

      const encryptedCardData = await encryptCardDataForTransport(cardData)
      setCardData(EMPTY_CARD)
      setIsEncrypting(false)

      const order = await submitOrder({
        productName: productName.trim(),
        quantity: Number(quantity),
        amount: Number(amount),
        encryptedCardData,
      })

      if (order) {
        setProductName('')
        setQuantity('1')
        setAmount('0.00')
        setSuccessMessage(`Orden #${order.id} creada con estado ${order.status}.`)
        onOrderCreated?.(order)
      }
    } catch (error) {
      setEncryptionError(
        error instanceof Error ? error.message : 'No fue posible proteger los datos de la tarjeta',
      )
    } finally {
      setIsEncrypting(false)
    }
  }

  return (
    <section className="min-w-0 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200 sm:p-6" aria-labelledby="order-form-title">
      <div className="mb-6">
        <h2 id="order-form-title" className="text-2xl font-semibold">Nueva orden</h2>
        <p className="mt-2 text-sm text-slate-600">
          Los datos de la tarjeta se cifran en este dispositivo antes de enviarse.
        </p>
      </div>

      <form className="space-y-6" onSubmit={handleSubmit}>
        <fieldset disabled={isBusy} className="grid min-w-0 gap-5 disabled:opacity-70 sm:grid-cols-2">
          <label className="grid gap-2 text-sm font-medium text-slate-700 sm:col-span-2">
            Producto
            <input
              className="min-w-0 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              name="productName"
              value={productName}
              onChange={(event) => setProductName(event.target.value)}
              maxLength={100}
              required
            />
          </label>

          <label className="grid gap-2 text-sm font-medium text-slate-700">
            Cantidad
            <input
              className="min-w-0 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              name="quantity"
              type="number"
              min="1"
              step="1"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
              required
            />
          </label>

          <label className="grid gap-2 text-sm font-medium text-slate-700">
            Monto
            <input
              className="min-w-0 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              name="amount"
              type="number"
              min="0"
              step="0.01"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              required
            />
          </label>

          <label className="grid gap-2 text-sm font-medium text-slate-700 sm:col-span-2">
            Número de tarjeta
            <input
              className="min-w-0 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              name="cardNumber"
              type="password"
              inputMode="numeric"
              autoComplete="cc-number"
              value={cardData.cardNumber}
              onChange={(event) => updateCardField('cardNumber', event.target.value)}
              required
            />
          </label>

          <label className="grid gap-2 text-sm font-medium text-slate-700">
            Expiración
            <input
              className="min-w-0 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              name="expiration"
              autoComplete="cc-exp"
              value={cardData.expiration}
              onChange={(event) => updateCardField('expiration', event.target.value)}
              required
            />
          </label>

          <label className="grid gap-2 text-sm font-medium text-slate-700">
            CVV
            <input
              className="min-w-0 rounded-lg border border-slate-300 px-3 py-2 outline-none focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
              name="cvv"
              type="password"
              inputMode="numeric"
              autoComplete="cc-csc"
              value={cardData.cvv}
              onChange={(event) => updateCardField('cvv', event.target.value)}
              required
            />
          </label>
        </fieldset>

        {(encryptionError || requestError) && (
          <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800">
            {encryptionError ?? requestError}
          </p>
        )}

        {successMessage && (
          <p role="status" className="rounded-lg bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
            {successMessage}
          </p>
        )}

        <button
          className="w-full rounded-lg bg-blue-700 px-4 py-3 font-semibold text-white transition hover:bg-blue-800 focus:outline-none focus:ring-2 focus:ring-blue-300 disabled:cursor-not-allowed disabled:bg-slate-400"
          type="submit"
          disabled={isBusy}
        >
          {isBusy ? 'Procesando…' : 'Crear orden'}
        </button>
      </form>
    </section>
  )
}
