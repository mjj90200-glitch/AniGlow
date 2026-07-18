/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './components/**/*.{js,vue,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './plugins/**/*.{js,ts}',
    './app.vue',
    './error.vue'
  ],
  theme: {
    extend: {
      colors: {
        // ===== 萤火番舍核心色板 =====

        // 奶油背景系 - 柔和渐变底色
        cream: {
          50: '#FEFDFB',
          100: '#FDFBF7',
          200: '#FAF6EE',
          300: '#F5F1E8',
          400: '#EDE6D6',
          DEFAULT: '#FDFBF7',
        },

        // 萤火绿 - 主高亮色（初音绿）
        firefly: {
          50:  '#E8FFF0',
          100: '#B8FFD4',
          200: '#85FFB8',
          300: '#4AFF9A',
          400: '#00E676', // 核心初音绿
          500: '#00C853',
          600: '#00A344',
          700: '#008830',
          800: '#006E26',
          900: '#00541C',
          DEFAULT: '#00E676',
        },

        // 糖果色系标签
        candy: {
          blue: {
            DEFAULT: '#A0D8EF',
            light: '#E8F4F8',
            dark: '#7BC4E0',
            bg: 'rgba(160, 216, 239, 0.25)',
          },
          pink: {
            DEFAULT: '#FFC0CB',
            light: '#FFE4E9',
            dark: '#FF9AAE',
            bg: 'rgba(255, 192, 203, 0.25)',
          },
          purple: {
            DEFAULT: '#D4A5FF',
            light: '#F0E4FF',
            dark: '#B880FF',
            bg: 'rgba(212, 165, 255, 0.25)',
          },
          mint: {
            DEFAULT: '#A8E6CF',
            light: '#E4F7EF',
            dark: '#7DD3B0',
            bg: 'rgba(168, 230, 207, 0.25)',
          },
          peach: {
            DEFAULT: '#FFB37E',
            light: '#FFE4D4',
            dark: '#FF9A5C',
            bg: 'rgba(255, 179, 126, 0.25)',
          },
          lavender: {
            DEFAULT: '#C5B9E8',
            light: '#EBE6F5',
            dark: '#A899D4',
            bg: 'rgba(197, 185, 232, 0.25)',
          },
        },

        // 保留的兼容色
        sky: {
          light: '#E8F4F8',
          DEFAULT: '#A0D8EF',
          dark: '#7BC4E0'
        },
        sakura: {
          DEFAULT: '#FFC0CB',
          dark: '#FF9AAE',
          light: '#FFE4E9'
        },
        sun: {
          DEFAULT: '#FFB37E',
          dark: '#FF9A5C',
          light: '#FFE4D4'
        },

        // 评分金色
        rating: {
          DEFAULT: '#FFB800',
          light: '#FFD54F',
          dark: '#F9A825',
        },
      },

      fontFamily: {
        sans: ['Nunito', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif']
      },

      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
        '4xl': '2rem',
        '5xl': '2.5rem',
        '6xl': '3rem',
      },

      boxShadow: {
        // 琉璃阴影系统
        'glass': '0 8px 32px rgba(160, 216, 239, 0.15), 0 2px 8px rgba(255, 192, 203, 0.1)',
        'glass-lg': '0 12px 48px rgba(160, 216, 239, 0.2), 0 4px 12px rgba(255, 192, 203, 0.15)',
        'glass-xl': '0 16px 64px rgba(160, 216, 239, 0.25), 0 8px 24px rgba(255, 192, 203, 0.12)',

        // 琉璃别名
        'glaze': '0 8px 32px rgba(160, 216, 239, 0.18), 0 2px 8px rgba(255, 192, 203, 0.08)',
        'glaze-lg': '0 16px 48px rgba(160, 216, 239, 0.25), 0 4px 16px rgba(255, 192, 203, 0.12)',

        // 萤火光晕
        'glow': '0 0 40px rgba(0, 230, 118, 0.3)',
        'glow-sm': '0 0 20px rgba(0, 230, 118, 0.2)',
        'glow-lg': '0 0 60px rgba(0, 230, 118, 0.4)',

        // 糖果色阴影
        'candy': '0 4px 20px -2px rgba(160, 216, 239, 0.3)',
        'candy-pink': '0 4px 20px -2px rgba(255, 192, 203, 0.3)',
        'candy-purple': '0 4px 20px -2px rgba(212, 165, 255, 0.3)',

        // 柔和投影
        'soft': '0 4px 20px -2px rgba(160, 216, 239, 0.25)',
        'soft-lg': '0 10px 40px -4px rgba(160, 216, 239, 0.35)',
        'ambient': '0 8px 32px rgba(160, 216, 239, 0.2), 0 2px 8px rgba(255, 192, 203, 0.15)',
      },

      backgroundImage: {
        // 渐变背景
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'hero-cream': 'linear-gradient(135deg, #FDFBF7 0%, #FAF6EE 50%, #F5F1E8 100%)',
        'hero-warm': 'linear-gradient(135deg, #FDFBF7 0%, #FFE4E9 30%, #E8F4F8 70%, #F5F1E8 100%)',
        'glass-cream': 'linear-gradient(135deg, rgba(253, 251, 247, 0.9) 0%, rgba(250, 246, 238, 0.85) 100%)',
        'firefly-gradient': 'linear-gradient(135deg, #00E676 0%, #00C853 100%)',
      },

      backdropBlur: {
        'xs': '2px',
        '2xl': '16px',
        '3xl': '24px',
        '4xl': '32px',
      },

      animation: {
        'float': 'float 6s ease-in-out infinite',
        'float-slow': 'float 8s ease-in-out infinite',
        'float-fast': 'float 4s ease-in-out infinite',
        'pulse-glow': 'pulseGlow 3s ease-in-out infinite',
        'shimmer': 'shimmer 2s linear infinite',
        'fade-in': 'fadeIn 0.6s ease-out forwards',
        'slide-up': 'slideUp 0.5s ease-out forwards',
        'scale-in': 'scaleIn 0.3s ease-out forwards',
      },

      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-12px)' },
        },
        pulseGlow: {
          '0%, 100%': { opacity: '0.6', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.05)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        fadeIn: {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        slideUp: {
          from: { opacity: '0', transform: 'translateY(20px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        scaleIn: {
          from: { opacity: '0', transform: 'scale(0.95)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
      },

      transitionDuration: {
        '400': '400ms',
        '600': '600ms',
        '800': '800ms',
      },
    },
  },
  plugins: [
    // 自定义插件：琉璃玻璃效果
    function({ addUtilities, theme }) {
      const glassUtilities = {
        '.glass': {
          backgroundColor: 'rgba(255, 255, 255, 0.7)',
          backdropFilter: 'blur(20px)',
          borderRadius: theme('borderRadius.3xl'),
          boxShadow: theme('boxShadow.glass'),
        },
        '.glass-lg': {
          backgroundColor: 'rgba(255, 255, 255, 0.75)',
          backdropFilter: 'blur(24px)',
          borderRadius: theme('borderRadius.4xl'),
          boxShadow: theme('boxShadow.glass-lg'),
        },
        '.glass-xl': {
          backgroundColor: 'rgba(255, 255, 255, 0.8)',
          backdropFilter: 'blur(32px)',
          borderRadius: theme('borderRadius.4xl'),
          boxShadow: theme('boxShadow.glass-xl'),
        },
        '.glass-cream': {
          background: 'linear-gradient(135deg, rgba(253, 251, 247, 0.9) 0%, rgba(250, 246, 238, 0.85) 100%)',
          backdropFilter: 'blur(20px)',
          borderRadius: theme('borderRadius.3xl'),
          boxShadow: theme('boxShadow.glass'),
        },
      }
      addUtilities(glassUtilities)
    },
  ],
}
