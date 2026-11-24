import { useCallback, useRef, useState } from 'react';
import { getConfig, delChannel } from '@/apis';
import { useAgoraVoice } from './useAgoraVoice';
import { formatRtcError } from '@/utils/mediaSupport';
import logger from '@/utils/logger';

interface JoinChannelConfig {
  appId: string;
  channel: string;
  token: string;
  uid: string;
}

export const useConversation = (digitalHumanType: api.DigitalHumanType) => {
  const [isStarting, setIsStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const agentIdRef = useRef<string | null>(null);

  const {
    isMicEnabled,
    isConnected,
    isLoading: voiceLoading,
    error: voiceError,
    joinChannel,
    enableMic,
    disableMic,
    leaveChannel,
  } = useAgoraVoice();

  const startConversation = useCallback(async () => {
    setIsStarting(true);
    setError(null);

    try {
      let config: api.ConfigResponse;

      try {
        const payload: api.GetConfigPayload = {
          deviceId: '1234567890',
          digitalHumanType,
        }
        const { data } = await getConfig(payload) as api.BaseRes<api.ConfigResponse>;
        config = data;
        agentIdRef.current = config.agentId;
        logger.info('[对话] 拿到后端返回参数 成功（远端 Agent 已由后端启动并先入频道）', { digitalHumanType, data });
      } catch (err) {
        logger.error('[对话] 拿到后端返回参数 失败', err);
        throw err;
      }

      try {
        const joinChannelConfig: JoinChannelConfig = {
          appId: config.appId,
          channel: config.channel,
          token: config.token,
          uid: config.userId,
        }
        await joinChannel({
          appId: joinChannelConfig.appId,
          channel: joinChannelConfig.channel,
          token: joinChannelConfig.token,
          uid: joinChannelConfig.uid,
        });
        logger.info('[对话] 后进频道成功', { ...joinChannelConfig });
      } catch (err) {
        logger.error('[对话] 进房失败', err);
        throw err;
      }

      try {
        await enableMic();
        logger.info('[对话] 开启麦克风成功，请说话');
      } catch (err) {
        logger.error('[对话] 开启麦克风失败', err);
        throw err;
      }

      logger.info('[对话] 入房开麦完成；说话 → 音频上行 → 声网云端 TTS → AI 回话自动播放');
    } catch (err: unknown) {
      const msg = formatRtcError(err);
      setError(msg);
      await leaveChannel();
    } finally {
      setIsStarting(false);
    }
  }, [digitalHumanType, joinChannel, enableMic, leaveChannel]);

  const endConversation = useCallback(async () => {
    logger.info('[对话] 开始结束对话...');
    try {
      if (agentIdRef.current) {
        const payload: api.LeavePayload = {
          agentId: agentIdRef.current,
        };
        await delChannel(payload);
        logger.info('[对话] 通知后端销毁 Agent 成功');
      }
    } catch (err) {
      logger.error('[对话] 通知后端销毁 Agent 失败', err);
    }
    await disableMic();
    await leaveChannel();
    agentIdRef.current = null;
    logger.info('[对话] 结束对话流程全部完成');
  }, [disableMic, leaveChannel]);

  return {
    isConnected,
    isMicEnabled,
    isStarting: isStarting || voiceLoading,
    error: error || voiceError,
    startConversation,
    endConversation,
    toggleMic: async () => {
      if (isMicEnabled) {
        await disableMic();
      } else {
        await enableMic();
      }
    },
  };
};
