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
        {/* 1. Lightning Bolt (Left) */}
        <path
          d="M38 18 L18 48 H34 L24 74 L48 42 H34 L42 18 Z"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* 2. Harmonious Musical Note (Right) */}
        <path
          d="M48 26 L72 18 V56"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle
          cx="60"
          cy="56"
          r="12"
          stroke="#ffffff"
          strokeWidth="5"
        />

        {/* 3. Precision Download Arrow (Bottom) */}
        <path
          d="M48 58 V82"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
        />
        <path
          d="M36 70 L48 82 L60 70"
          stroke="#ffffff"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  )
}
