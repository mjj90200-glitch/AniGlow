/**
 * Authing 认证 composable — 通过 Nuxt 服务端 API 代理调用 Authing
 */
export function useAuthingSDK() {
  /** 发送短信验证码（仅注册时用） */
  const sendSmsCode = async (phone: string): Promise<{ ok: boolean; message?: string }> => {
    try {
      await $fetch('/api/auth/send-sms', { method: 'POST', body: { phone } })
      return { ok: true }
    } catch (e: any) {
      return { ok: false, message: e?.data?.message || e?.message || '发送失败' }
    }
  }

  /** 手机号 + 密码登录（免费，日常使用） */
  const loginByPassword = async (
    phone: string,
    password: string
  ): Promise<{ ok: boolean; user?: any; token?: string; backendToken?: string; message?: string }> => {
    try {
      const res = await $fetch<{ token: string; backendToken?: string; user: any }>('/api/auth/login-password', {
        method: 'POST',
        body: { phone, password },
      })
      return { ok: true, user: res.user, token: res.token, backendToken: res.backendToken }
    } catch (e: any) {
      return { ok: false, message: e?.data?.message || e?.message || '登录失败' }
    }
  }

  /** 验证码登录（保留，兼容旧流程） */
  const loginByPhoneCode = async (phone: string, code: string) => {
    try {
      const res = await $fetch<{ token: string; backendToken?: string; user: any }>('/api/auth/login-sms', {
        method: 'POST', body: { phone, code },
      })
      return { ok: true, user: res.user, token: res.token, backendToken: res.backendToken }
    } catch (e: any) {
      return { ok: false, message: e?.data?.message || e?.message || '登录失败' }
    }
  }

  /** 注册：手机号 + 验证码 + 设置密码 */
  const registerWithPassword = async (
    phone: string,
    code: string,
    password: string
  ): Promise<{ ok: boolean; user?: any; token?: string; backendToken?: string; message?: string }> => {
    try {
      const res = await $fetch<{ token: string; backendToken?: string; user: any }>('/api/auth/register', {
        method: 'POST',
        body: { phone, code, password },
      })
      return { ok: true, user: res.user, token: res.token, backendToken: res.backendToken }
    } catch (e: any) {
      return { ok: false, message: e?.data?.message || e?.message || '注册失败' }
    }
  }

  return { sendSmsCode, loginByPassword, loginByPhoneCode, registerWithPassword }
}
