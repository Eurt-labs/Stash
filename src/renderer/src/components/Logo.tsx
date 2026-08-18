import React from 'react'

interface LogoProps {
  size?: number
  className?: string
}

export const Logo: React.FC<LogoProps> = ({ size = 36, className = '' }) => {
  return (
    <div
      className={className}
      style={{
        width: size,
        height: size,
        borderRadius: Math.round(size * 0.24),
        backgroundColor: '#0c0e14',
        border: '1px solid rgba(255, 255, 255, 0.12)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        flexShrink: 0,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.25)',
        transition: 'all 0.2s ease',
        overflow: 'hidden'
      }}
    >
      <svg
        width={Math.round(size * 0.72)}
        height={Math.round(size * 0.72)}
        viewBox="0 0 100 100"
        fill="none"
      >
        {/* 1. Musical Note (Top / Center) */}
        <path
          d="M58 20 C68 20 74 26 74 34"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
        />
        <path
          d="M58 20 V54"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
        />
        <circle
          cx="44"
          cy="54"
          r="14"
          stroke="#ffffff"
          strokeWidth="5"
        />

        {/* 2. Precision Download Arrow (Bottom / Center) */}
        <path
          d="M50 58 V80"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
        />
        <path
          d="M38 68 L50 80 L62 68"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  )
}
