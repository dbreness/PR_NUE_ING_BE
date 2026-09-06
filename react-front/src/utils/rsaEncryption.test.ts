// @vitest-environment node
/// <reference types="node" />

import { webcrypto } from 'node:crypto'
import { describe, expect, it } from 'vitest'
import type { CardData } from '../types/card'
import { encryptCardData } from './rsaEncryption'

function toPem(value: ArrayBuffer): string {
  const base64 = Buffer.from(value).toString('base64').match(/.{1,64}/g)?.join('\n') ?? ''
  return `-----BEGIN PUBLIC KEY-----\n${base64}\n-----END PUBLIC KEY-----\n`
}

describe('encryptCardData', () => {
  it('cifra un JSON compatible con RSA-OAEP SHA-256', async () => {
    const keyPair = await webcrypto.subtle.generateKey(
      {
        name: 'RSA-OAEP',
        modulusLength: 2048,
        publicExponent: new Uint8Array([1, 0, 1]),
        hash: 'SHA-256',
      },
      true,
      ['encrypt', 'decrypt'],
    )
    const publicKey = await webcrypto.subtle.exportKey('spki', keyPair.publicKey)
    const cardData: CardData = {
      cardNumber: '4111111111111111',
      expiration: '12/30',
      cvv: '123',
    }

    const encrypted = await encryptCardData(cardData, toPem(publicKey))
    const decrypted = await webcrypto.subtle.decrypt(
      { name: 'RSA-OAEP' },
      keyPair.privateKey,
      Buffer.from(encrypted, 'base64'),
    )

    expect(JSON.parse(new TextDecoder().decode(decrypted))).toEqual(cardData)
  })
})
