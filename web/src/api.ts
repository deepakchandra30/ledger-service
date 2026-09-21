import type { Account, Transfer } from './types'

const token = () => localStorage.getItem('ledger.token') ?? ''

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token()}`,
      ...(init?.headers ?? {}),
    },
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => ({ detail: response.statusText }))
    throw { status: response.status, detail: problem.detail } as const
  }
  return response.json() as Promise<T>
}

export const listAccounts = () => request<Account[]>('/api/accounts')

export const postTransfer = (from: string, to: string, amount: string) =>
  request<Transfer>('/api/transfers', {
    method: 'POST',
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body: JSON.stringify({ from, to, amount }),
  })
