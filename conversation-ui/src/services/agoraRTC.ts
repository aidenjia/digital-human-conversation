import AgoraRTC, {
  type IAgoraRTCClient,
  type IAgoraRTCRemoteUser,
  type IMicrophoneAudioTrack,
  type UID,
} from 'agora-rtc-sdk-ng';
import logger from '@/utils/logger';

async function pickMicrophoneId(): Promise<string | undefined> {
  let mics = await AgoraRTC.getMicrophones(true);
  if (mics.length === 0) {
    mics = await AgoraRTC.getMicrophones(false);
  }

  logger.info('[对话] 可用麦克风列表', mics);

  if (mics.length === 0) {
    throw new Error(
      '未检测到麦克风设备，请确认已接入麦克风并在系统设置中启用录音设备',
    );
  }

  return mics[0].deviceId;
}

export class AgoraRTCService {
  private client: IAgoraRTCClient | null = null;
  private localAudioTrack: IMicrophoneAudioTrack | null = null;
  private uplinkMonitorTimer: ReturnType<typeof setInterval> | null = null;
  private downlinkMonitorTimer: ReturnType<typeof setInterval> | null = null;
  private agentWaitTimer: ReturnType<typeof setTimeout> | null = null;
  private lastSendBytes = -1;
  private lastReceiveBytes = -1;
  private remoteUserJoined = false;
  private remoteAudioSubscribed = false;
  private agentUid: UID | null = null;
  private subscribedRemoteUids = new Set<string>();
  private startUplinkMonitor(): void {
    this.stopUplinkMonitor();
    this.lastSendBytes = -1;

    this.uplinkMonitorTimer = setInterval(() => {
      if (!this.client) return;

      const { sendBytes, sendBitrate, sendPackets } = this.client.getLocalAudioStats();

      if (this.lastSendBytes >= 0 && sendBytes > this.lastSendBytes) {
        logger.info('[对话] 音频已上行，声网云端处理中...', { sendBytes, sendBitrate, sendPackets });
        if (this.agentUid !== null) {
          logger.info('[对话] 等待 AI TTS 回话音频下行...', { agentUid: this.agentUid });
          this.startDownlinkMonitor(this.agentUid);
        }
        this.stopUplinkMonitor();
        return;
      }

      this.lastSendBytes = sendBytes;
    }, 2000);
  }

  private stopUplinkMonitor(): void {
    if (this.uplinkMonitorTimer) {
      clearInterval(this.uplinkMonitorTimer);
      this.uplinkMonitorTimer = null;
    }
    this.lastSendBytes = -1;
  }

  private startDownlinkMonitor(uid: UID): void {
    this.stopDownlinkMonitor();
    this.lastReceiveBytes = -1;

    this.downlinkMonitorTimer = setInterval(() => {
      if (!this.client) return;

      const remoteStats = this.client.getRemoteAudioStats();
      const stats = remoteStats[String(uid)];

      if (!stats) return;

      if (this.lastReceiveBytes >= 0 && stats.receiveBytes > this.lastReceiveBytes) {
        logger.info('[对话] 收到 AI 音频下行（TTS 回话播放中）', {
          uid,
          receiveBytes: stats.receiveBytes,
          receiveBitrate: stats.receiveBitrate,
          receiveLevel: stats.receiveLevel,
        });
        this.stopDownlinkMonitor();
        return;
      }

      this.lastReceiveBytes = stats.receiveBytes;
    }, 2000);
  }

  private stopDownlinkMonitor(): void {
    if (this.downlinkMonitorTimer) {
      clearInterval(this.downlinkMonitorTimer);
      this.downlinkMonitorTimer = null;
    }
    this.lastReceiveBytes = -1;
  }

  private startAgentWaitMonitor(): void {
    this.stopAgentWaitMonitor();

    if (this.remoteAudioSubscribed) {
      logger.info('[对话] 远端 Agent 音频通道已就绪，请说话');
      return;
    }

    if (this.remoteUserJoined) {
      logger.info('[对话] 远端 Agent 已在频道内，等待发布音频...');
    } else {
      logger.info('[对话] 频道内暂未检测到远端 Agent，等待进房...');
    }

    this.agentWaitTimer = setTimeout(() => {
      if (!this.remoteUserJoined) {
        logger.warn('[对话] 等待远端 Agent 进房超时（15s），请检查后端是否已在 getConfig 时启动 Agent');
      } else if (!this.remoteAudioSubscribed) {
        logger.warn('[对话] 远端 Agent 已在频道但未发布音频（15s），请检查后端推流配置');
      }
    }, 15000);
  }

  private stopAgentWaitMonitor(): void {
    if (this.agentWaitTimer) {
      clearTimeout(this.agentWaitTimer);
      this.agentWaitTimer = null;
    }
  }

  private setupRemoteListeners(): void {
    if (!this.client) return;

    this.client.on('user-joined', (user) => {
      this.remoteUserJoined = true;
      logger.info('[对话] 远端用户新进房', { uid: user.uid, hasAudio: user.hasAudio, hasVideo: user.hasVideo });

      if (!user.hasAudio) {
        logger.info('[对话] 远端用户已进房，尚未发布音频，等待中...', { uid: user.uid });
      }
    });

    this.client.on('user-left', (user, reason) => {
      logger.info('[对话] 远端用户离房', { uid: user.uid, reason });
    });

    this.client.on('user-published', async (user, mediaType) => {
      logger.info('[对话] 远端用户发布流', { uid: user.uid, mediaType });

      if (mediaType === 'audio') {
        await this.subscribeRemoteAudio(user);
      }
    });

    this.client.on('user-unpublished', (user, mediaType) => {
      logger.info('[对话] 远端用户取消发布', { uid: user.uid, mediaType });
    });

    this.client.on('published-user-list', (users) => {
      logger.info('[对话] 频道内已发布用户列表', users.map((u) => ({
        uid: u.uid,
        hasAudio: u.hasAudio,
        hasVideo: u.hasVideo,
      })));

      for (const user of users) {
        if (user.hasAudio && !user.audioTrack) {
          void this.subscribeRemoteAudio(user);
        }
      }
    });

    AgoraRTC.onAudioAutoplayFailed = () => {
      logger.warn('[对话] 远端音频自动播放被浏览器拦截，请点击页面任意位置后重试');
    };
  }

  private async subscribeRemoteAudio(user: IAgoraRTCRemoteUser): Promise<void> {
    if (!this.client) return;

    const uidKey = String(user.uid);
    if (this.subscribedRemoteUids.has(uidKey)) return;

    logger.info('[对话] 开始订阅远端音频', { uid: user.uid });

    try {
      await this.client.subscribe(user, 'audio');
      this.subscribedRemoteUids.add(uidKey);
      this.remoteUserJoined = true;
      this.remoteAudioSubscribed = true;
      this.agentUid = user.uid;
      this.stopAgentWaitMonitor();
      logger.info('[对话] 订阅远端音频成功，播放通道已就绪', { uid: user.uid });

      const track = user.audioTrack;
      if (!track) {
        logger.warn('[对话] 订阅成功但未拿到 audioTrack', { uid: user.uid });
        return;
      }

      track.play();
      logger.info('[对话] 远端音频自动播放已启动（说话后将接收 AI TTS 回话）', { uid: user.uid });
    } catch (err) {
      logger.error('[对话] 订阅远端音频失败', { uid: user.uid }, err);
    }
  }

  private async syncExistingRemoteUsers(): Promise<void> {
    if (!this.client) return;

    const remoteUsers = this.client.remoteUsers;
    if (remoteUsers.length === 0) {
      logger.info('[对话] 频道内暂无其他远端用户');
      return;
    }

    this.remoteUserJoined = true;
    logger.info('[对话] 检测到频道内已有远端用户（后端 Agent 应已先入房）', remoteUsers.map((u) => ({
      uid: u.uid,
      hasAudio: u.hasAudio,
      hasVideo: u.hasVideo,
    })));

    for (const user of remoteUsers) {
      if (user.hasAudio) {
        await this.subscribeRemoteAudio(user);
      } else {
        logger.info('[对话] 远端用户已在频道，尚未发布音频，等待中...', { uid: user.uid });
      }
    }
  }

  async join(config: { appId: string; channel: string; token: string; uid: number | string }): Promise<void> {
    logger.info('[对话] 正在加入频道...', { channel: config.channel, uid: config.uid });
    this.client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
    this.setupRemoteListeners();
    await this.client.join(config.appId, config.channel, config.token, config.uid);
    logger.info('[对话] SDK join 完成（本端为后进频道）');
    await this.syncExistingRemoteUsers();
  }

  async enableMicrophone(): Promise<void> {
    if (!this.client) throw new Error('RTC client not initialized');
    if (this.localAudioTrack) return;

    logger.info('[对话] 正在创建麦克风轨道...');
    const microphoneId = await pickMicrophoneId();
    this.localAudioTrack = await AgoraRTC.createMicrophoneAudioTrack({ microphoneId });
    logger.info('[对话] 麦克风轨道创建成功，正在 publish...');

    await this.client.publish([this.localAudioTrack]);
    logger.info('[对话] 本地音频 publish 成功，请开始说话');

    this.startUplinkMonitor();
    this.startAgentWaitMonitor();
  }

  async disableMicrophone(): Promise<void> {
    this.stopUplinkMonitor();
    this.stopAgentWaitMonitor();
    if (this.localAudioTrack) {
      logger.info('[对话] 正在取消发布本地音频...');
      await this.client?.unpublish([this.localAudioTrack]);
      this.localAudioTrack.close();
      this.localAudioTrack = null;
      logger.info('[对话] 本地音频已关闭');
    }
  }

  isMicEnabled(): boolean {
    return this.localAudioTrack !== null;
  }

  async leave(): Promise<void> {
    this.stopUplinkMonitor();
    this.stopDownlinkMonitor();
    this.stopAgentWaitMonitor();

    if (this.localAudioTrack) {
      await this.disableMicrophone();
    }

    if (this.client) {
      logger.info('[对话] 正在离开频道...');
      await this.client.leave();
      this.client = null;
      this.agentUid = null;
      this.subscribedRemoteUids.clear();
      this.remoteUserJoined = false;
      this.remoteAudioSubscribed = false;
      logger.info('[对话] 已离开频道');
    }
  }
}
