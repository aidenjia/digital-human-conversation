import request from '@/utils/request'

export const getConfig = (d: api.GetConfigPayload): Promise<api.BaseRes<api.ConfigResponse>> => {
  return request.post('/api/convoai/agents', d)
}

export const delChannel = (d: api.LeavePayload): Promise<api.BaseRes<void>> => {
  return request.delete('/api/convoai/agents', { data: d })
}