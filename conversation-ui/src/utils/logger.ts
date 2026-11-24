import Debug from 'debug'
import { cloneDeep } from 'lodash-es'

const baseLog = Debug('AGORA_AI_VOICE_ASSISTANT')

export const trace = baseLog.extend('[trace]')
trace.color = '#999'

export const debug = baseLog.extend('[debug]')
debug.color = '#52c41a'

export const info = baseLog.extend('[info]')
info.color = '#4096ff'

export const warn = baseLog.extend('[warn]')
warn.color = '#faad14'

export const error = baseLog.extend('[error]')
error.color = '#f5222d'

export const performance = baseLog.extend('[info]')
error.color = '#f759ab'

export const { enabled } = baseLog

export default {
  trace: (message: string, ...args: any[]) => {
    trace(message, ...cloneDeep(args))
  },
  debug: (message: string, ...args: any[]) => {
    debug(message, ...cloneDeep(args))
  },
  info: (message: string, ...args: any[]) => {
    info(message, ...cloneDeep(args))
  },
  warn: (message: string, ...args: any[]) => {
    warn(message, ...cloneDeep(args))
  },
  error: (message: string, ...args: any[]) => {
    error(message, ...cloneDeep(args))
  },
  performance,
}
