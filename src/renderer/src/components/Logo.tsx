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
        {/* Note Head Loop at bottom-left */}
        <circle
          cx="36"
          cy="62"
          r="14"
          stroke="#ffffff"
          strokeWidth="5.5"
        />

        {/* Stem rising up from note tangent, curving over the top arch, and descending down as arrow shaft */}
        <path
          d="M50 62 V28 C50 15 70 15 70 28 V56"
          stroke="#ffffff"
          strokeWidth="5.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Downward Arrowhead */}
        <path
          d="M59 46 L70 56 L81 46"
          stroke="#ffffff"
          strokeWidth="5.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  )
}
