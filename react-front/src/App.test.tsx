import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import App from './App'

vi.mock('./components/OrderHistory', () => ({
  OrderHistory: () => <div>Historial de prueba</div>,
}))

describe('App', () => {
  it('muestra el encabezado principal del panel', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Gestión de órdenes' })).toBeInTheDocument()
  })
})
