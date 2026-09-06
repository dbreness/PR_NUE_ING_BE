import { useState } from 'react'
import { OrderHistory } from './components/OrderHistory'
import { OrderForm } from './components/OrderForm'

function App() {
  const [historyRefreshToken, setHistoryRefreshToken] = useState(0)

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-8 text-slate-900 sm:px-6 sm:py-12">
      <div className="mx-auto min-w-0 max-w-6xl">
        <p className="text-sm font-semibold uppercase tracking-widest text-blue-700">
          Prueba técnica backend
        </p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Gestión de órdenes</h1>
        <p className="mt-4 max-w-2xl text-lg text-slate-600">
          Crea órdenes y consulta el resultado de su procesamiento de pago.
        </p>
        <div className="mt-10 grid min-w-0 gap-8">
          <div className="min-w-0 max-w-2xl">
            <OrderForm
              onOrderCreated={() => setHistoryRefreshToken((current) => current + 1)}
            />
          </div>
          <OrderHistory refreshToken={historyRefreshToken} />
        </div>
      </div>
    </main>
  )
}

export default App
