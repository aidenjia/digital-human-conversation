import { useState, useRef, useCallback } from 'react';
import { AgoraRTCService } from '../services/agoraRTC';
import { formatRtcError } from '@/utils/mediaSupport';
import logger from '@/utils/logger';
export const useAgoraVoice = () => {
  const [isMicEnabled, setIsMicEnabled] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const rtcServiceRef = useRef<AgoraRTCService | null>(null);

  const joinChannel = useCallback(async (config: api.JoinConfig) => {
    setIsLoading(true);
    setError(null);
    try {
      const service = new AgoraRTCService();
      await service.join({
        appId: config.appId,
        channel: config.channel,
        token: config.token,
        uid: config.uid,
      });
      rtcServiceRef.current = service;
      setIsConnected(true);
    } catch (err: unknown) {
      setError(formatRtcError(err));
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const enableMic = useCallback(async () => {
    if (!rtcServiceRef.current) {
      const msg = 'Please join channel first';
      setError(msg);
      throw new Error(msg);
    }
    try {
      await rtcServiceRef.current.enableMicrophone();
      setIsMicEnabled(true);
    } catch (err: unknown) {
      setError(formatRtcError(err));
      throw err;
    }
  }, []);

  const disableMic = useCallback(async () => {
    if (!rtcServiceRef.current) return;
    await rtcServiceRef.current.disableMicrophone();
    setIsMicEnabled(false);
  }, []);

  const leaveChannel = useCallback(async () => {
    if (rtcServiceRef.current) {
      await rtcServiceRef.current.leave();
      logger.info('[对话] 离开频道成功');
      rtcServiceRef.current = null;
    }
    setIsMicEnabled(false);
    setIsConnected(false);
  }, []);

  return {
    isMicEnabled,
    isConnected,
    isLoading,
    error,
    joinChannel,
    enableMic,
    disableMic,
    leaveChannel,
  };
};