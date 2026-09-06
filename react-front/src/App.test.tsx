import { render, screen } from '@testing-library/react'
import App from './App'

describe('App', () => {
  it('muestra el encabezado principal del panel', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Gestión de órdenes' })).toBeInTheDocument()
  })
})
