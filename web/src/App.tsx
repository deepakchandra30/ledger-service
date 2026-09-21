import { useEffect, useState } from 'react'
import { listAccounts, postTransfer } from './api'
import type { Account, Transfer } from './types'

export default function App() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [posted, setPosted] = useState<Transfer[]>([])
  const [error, setError] = useState<string>()

  const refresh = () => listAccounts().then(setAccounts).catch((e) => setError(e.detail))

  useEffect(() => {
    refresh()
    const timer = setInterval(refresh, 5000)
    return () => clearInterval(timer)
  }, [])

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    try {
      const transfer = await postTransfer(
        String(form.get('from')),
        String(form.get('to')),
        String(form.get('amount')),
      )
      setPosted((current) => [transfer, ...current])
      setError(undefined)
      refresh()
    } catch (e) {
      setError((e as { detail: string }).detail)
    }
  }

  return (
    <main style={{ fontFamily: 'system-ui', maxWidth: 760, margin: '2rem auto' }}>
      <h1>Ledger</h1>

      <h2>Accounts</h2>
      <table>
        <thead>
          <tr><th>Reference</th><th>Currency</th><th align="right">Balance</th></tr>
        </thead>
        <tbody>
          {accounts.map((account) => (
            <tr key={account.id}>
              <td>{account.reference}</td>
              <td>{account.currency}</td>
              <td align="right">{account.balance}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Post a transfer</h2>
      <form onSubmit={submit}>
        <input name="from" placeholder="from account id" required />
        <input name="to" placeholder="to account id" required />
        <input name="amount" placeholder="amount" required />
        <button type="submit">Post</button>
      </form>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}

      <h2>This session</h2>
      <ul>
        {posted.map((transfer) => (
          <li key={transfer.id}>{transfer.amount} — {transfer.status}</li>
        ))}
      </ul>
    </main>
  )
}
