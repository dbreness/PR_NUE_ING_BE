import { OrderForm } from './components/OrderForm'

function App() {
  return (
    <main className="min-h-screen bg-slate-50 px-6 py-12 text-slate-900">
      <div className="mx-auto max-w-6xl">
        <p className="text-sm font-semibold uppercase tracking-widest text-blue-700">
          Prueba técnica backend
        </p>
        <h1 className="mt-3 text-4xl font-bold tracking-tight">Gestión de órdenes</h1>
        <p className="mt-4 max-w-2xl text-lg text-slate-600">
          Crea órdenes y consulta el resultado de su procesamiento de pago.
        </p>
        <div className="mt-10 max-w-2xl">
          <OrderForm />
        </div>
      </div>
    </main>
  )
}

export default App
