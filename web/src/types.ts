export interface Account {
  id: string
  reference: string
  currency: string
  balance: string
}

export interface Transfer {
  id: string
  from: string
  to: string
  amount: string
  status: 'POSTED' | 'SETTLED' | 'FAILED'
  createdAt: string
}

export interface ApiError {
  status: number
  detail: string
}
