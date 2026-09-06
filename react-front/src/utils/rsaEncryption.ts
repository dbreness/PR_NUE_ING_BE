import type { CardData } from '../types/card'

const PUBLIC_KEY_URL = '/public-key.pem'

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace('-----BEGIN PUBLIC KEY-----', '')
    .replace('-----END PUBLIC KEY-----', '')
    .replace(/\s/g, '')

  if (!base64) {
    throw new Error('La clave pública no tiene un formato válido')
  }

  try {
    const binary = atob(base64)
    const bytes = new Uint8Array(binary.length)

    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index)
    }

    return bytes.buffer
  } catch {
    throw new Error('La clave pública no tiene un formato válido')
  }
}

function arrayBufferToBase64(value: ArrayBuffer): string {
  const bytes = new Uint8Array(value)
  let binary = ''

  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }

  return btoa(binary)
}

export async function loadPublicKey(): Promise<string> {
  const errorMessage = 'No fue posible cargar la clave pública de pago'

  try {
    const response = await fetch(PUBLIC_KEY_URL)

    if (!response.ok) {
      throw new Error(errorMessage)
    }

    return await response.text()
  } catch (error) {
    if (error instanceof Error && error.message === errorMessage) {
      throw error
    }

    throw new Error(errorMessage, { cause: error })
  }
}

export async function encryptCardData(cardData: CardData, publicKeyPem: string): Promise<string> {
  try {
    const publicKey = await crypto.subtle.importKey(
      'spki',
      pemToArrayBuffer(publicKeyPem),
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256',
      },
      false,
      ['encrypt'],
    )
    const plaintext = new TextEncoder().encode(JSON.stringify(cardData))
    const encrypted = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, publicKey, plaintext)

    return arrayBufferToBase64(encrypted)
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('La clave pública')) {
      throw error
    }

    throw new Error('No fue posible cifrar los datos de la tarjeta', { cause: error })
  }
}

export async function encryptCardDataForTransport(cardData: CardData): Promise<string> {
  const publicKeyPem = await loadPublicKey()
  return encryptCardData(cardData, publicKeyPem)
}
