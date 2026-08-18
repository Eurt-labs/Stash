import React from 'react'
import { CheckCircle2, AlertCircle, Info } from 'lucide-react'

export interface ToastItem {
  id: string
  type: 'success' | 'error' | 'info'
  message: string
}

interface ToastProps {
  toasts: ToastItem[]
  onDismiss: (id: string) => void
}

export const Toast: React.FC<ToastProps> = ({ toasts, onDismiss }) => {
  if (toasts.length === 0) return null

  return (
    <div className="toast-container">
      {toasts.map((t) => (
        <div key={t.id} className={`toast ${t.type}`} onClick={() => onDismiss(t.id)}>
          {t.type === 'success' && <CheckCircle2 size={16} />}
          {t.type === 'error' && <AlertCircle size={16} />}
          {t.type === 'info' && <Info size={16} />}
          <span>{t.message}</span>
        </div>
      ))}
    </div>
  )
}
