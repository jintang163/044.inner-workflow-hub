const isHttps = window.location.protocol === 'https:'
const wsProtocol = isHttps ? 'wss:' : 'ws:'
const host = window.location.host

export const WS_BASE_URL = `${wsProtocol}//${host}${import.meta.env.VITE_APP_BASE_API || ''}/ws/approval`

export const RESULT_CODE_VERSION_CONFLICT = 2002
