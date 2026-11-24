import { Component, useEffect, useRef, useState, type ReactNode } from 'react'
import { Canvas, useFrame, useThree } from '@react-three/fiber'
import { Box3, Color, Group, Object3D, Vector3 } from 'three'
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { VRMLoaderPlugin, VRMUtils, type VRM } from '@pixiv/three-vrm'
import { AvatarRuntime, mouthTarget, smooth, type AvatarEmotion } from './AvatarRuntime'
import { DRESSED_IDLE_FORWARD_PITCH, DRESSED_IDLE_LATERAL_SLOPE, relaxedArmPose, applyRelaxedHands } from './armPose'
import { GazeController } from './GazeController'
import { MotionAnimation, parseMotion } from './WaveAnimation'
import { MOTION_CATALOG } from './motionCatalog'
import { MotionAssetLoader } from './MotionAssetLoader'
import { expressionSpeed, expressionTarget } from './expressionProfile'
import './AvatarStage.css'

const SCENES = ['#ededed', '#f4e6ed', '#dcefe9']
const EMOTIONS: AvatarEmotion[] = ['neutral', 'happy', 'sad', 'angry', 'surprised']
const LABELS = ['自然', '开心', '难过', '生气', '惊讶']
const DEFAULT_MODEL = {
  url: import.meta.env.BASE_URL + 'models/avatar-sample-2.vrm',
  name: 'AvatarSample_2 · ビビ',
}

class StageBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false }
  static getDerivedStateFromError() { return { failed: true } }
  render() {
    return this.state.failed
      ? <div className="avatar-notice">3D 场景不可用，请检查浏览器硬件加速。语音对话仍可使用。</div>
      : this.props.children
  }
}

function CameraControls({ framing }: { framing: 'portrait' | 'full' }) {
  const { camera, gl } = useThree()
  const controls = useRef<OrbitControls | null>(null)
  useEffect(() => {
    const orbit = new OrbitControls(camera, gl.domElement)
    camera.position.set(0, framing === 'portrait' ? 1.45 : 1.05, framing === 'portrait' ? 1.65 : 3.2)
    orbit.target.set(0, framing === 'portrait' ? 1.35 : 0.95, 0)
    orbit.enableDamping = true
    orbit.enableZoom = true
    orbit.minDistance = 1.0
    orbit.maxDistance = 4.5
    orbit.enablePan = false
    orbit.minPolarAngle = Math.PI * 0.25
    orbit.maxPolarAngle = Math.PI * 0.6
    orbit.update()
    controls.current = orbit
    return () => { controls.current = null; orbit.dispose() }
  }, [camera, gl, framing])
  useFrame(() => controls.current?.update())
  return null
}

function Character({ url, runtime, onStatus, onMotionStatus, idleMotion, maxFps }: {
  url: string | null
  runtime: AvatarRuntime
  onStatus: (status: string) => void
  onMotionStatus: (status: string) => void
  idleMotion: boolean
  maxFps: number
}) {
  const root = useRef<Group>(null)
  const vrmRef = useRef<VRM | null>(null)
  const wavePlayer = useRef<MotionAnimation | null>(null)
  const armRest = useRef({ left: { x: 0, z: 0, side: 1 }, right: { x: 0, z: 0, side: -1 } })
  const mouth = useRef(0)
  const blink = useRef(0)
  const nextBlink = useRef(2)
  const blinkStarted = useRef(-Infinity)
  const emotionWeights = useRef(EMOTIONS.map(() => 0))
  const mouthMesh = useRef<Group>(null)
  const eyeGroup = useRef<Group>(null)
  const heart = useRef<Group>(null)
  const renderTime = useRef(0)
  const background = useRef(new Color())
  const gaze = useRef(new GazeController())
  const gazePoint = useRef(new Object3D())
  const gazeDesired = useRef(new Vector3())
  const gazeReady = useRef(false)

  useEffect(() => {
    if (!url) return
    onMotionStatus('正在加载高频动作…')
    runtime.setProceduralReady([])
    runtime.setWaveDuration(2.5)
    let cancelled = false
    let model: VRM | undefined
    let motionLoader: MotionAssetLoader | undefined
    const downloads = new AbortController()
    const loader = new GLTFLoader()
    loader.register((parser) => new VRMLoaderPlugin(parser))
    void loader.loadAsync(url).then((gltf) => {
      model = gltf.userData.vrm as VRM | undefined
      if (!model) {
        VRMUtils.deepDispose(gltf.scene)
        throw new Error('请选择有效的 VRM 0.x / 1.0 模型')
      }
      if (cancelled) { VRMUtils.deepDispose(model.scene); return }
      VRMUtils.rotateVRM0(model)
      VRMUtils.removeUnnecessaryVertices(model.scene)
      model.humanoid.resetNormalizedPose()
      // Derive the lowering rotation from each rig instead of assuming VRM 1.x arm axes.
      for (const side of ['left', 'right'] as const) {
        const upper = model.humanoid.getNormalizedBoneNode(side === 'left' ? 'leftUpperArm' : 'rightUpperArm')
        const lower = model.humanoid.getNormalizedBoneNode(side === 'left' ? 'leftLowerArm' : 'rightLowerArm')
        if (upper && lower) {
          armRest.current[side] = url === DEFAULT_MODEL.url
            ? relaxedArmPose(lower.position, DRESSED_IDLE_FORWARD_PITCH, DRESSED_IDLE_LATERAL_SLOPE)
            : relaxedArmPose(lower.position)
          upper.rotation.set(armRest.current[side].x, 0, armRest.current[side].z)
        }
      }
      if (url === DEFAULT_MODEL.url) applyRelaxedHands(model.humanoid)
      model.update(0)
      const bounds = new Box3().setFromObject(model.scene)
      const size = bounds.getSize(new Vector3())
      const scale = 1.8 / Math.max(size.y, 0.1)
      model.scene.scale.multiplyScalar(scale)
      const center = bounds.getCenter(new Vector3())
      model.scene.position.set(-center.x * scale, -bounds.min.y * scale, -center.z * scale)
      root.current?.add(model.scene)
      vrmRef.current = model
      const expressions = model.expressionManager
      onStatus(expressions?.getExpression('aa')
        ? '角色已就绪 · 拖动旋转 · 口型随 AI 音频变化'
        : '角色已加载，但缺少 aa 嘴型绑定，无法展示语音口型')
      const activeModel = model
      wavePlayer.current = new MotionAnimation([], activeModel.humanoid)
      const supports = (asset: typeof MOTION_CATALOG[number]) =>
        asset.requiredBones.every(bone => !!activeModel.humanoid.getNormalizedBoneNode(bone))
      runtime.setProceduralReady(MOTION_CATALOG.filter(asset => asset.source.kind === 'procedural' && supports(asset)).map(asset => asset.id))
      motionLoader = new MotionAssetLoader({
        supports,
        fetchMotion: async asset => {
          if (asset.source.kind !== 'json') throw new Error('不是动画素材')
          const response = await fetch(import.meta.env.BASE_URL + asset.source.path, { signal: downloads.signal })
          if (!response.ok) throw new Error('动画素材读取失败')
          return response.json()
        },
        install: (asset, value) => {
          const motion = parseMotion(value)
          if (motion.name !== asset.id) throw new Error('动画名称与目录不一致')
          const strategy = url === DEFAULT_MODEL.url && asset.id === 'wave' ? 'right-arm' : asset.retargetStrategy
          wavePlayer.current!.registerMotion(motion, activeModel.humanoid, strategy)
          runtime.registerMotion(asset.id, motion.duration)
        },
      })
      const activeLoader = motionLoader
      runtime.setMotionLoader(async id => {
        const asset = MOTION_CATALOG.find(item => item.id === id)
        if (!asset || cancelled) return false
        onMotionStatus('正在加载' + asset.label + '…')
        const loaded = await activeLoader.load(id)
        if (!cancelled) onMotionStatus(loaded ? asset.label + '已就绪' : asset.label + '加载失败或骨骼不兼容，可重试')
        return loaded
      })
      void activeLoader.preload().then(results => {
        if (!cancelled) onMotionStatus(results.every(Boolean)
          ? '高频动作已就绪 · 试播动作点击后加载'
          : '部分高频素材加载失败 · 自动动作将跳过未就绪素材')
      })

    }).catch((error: unknown) => {
      if (!cancelled) onStatus(`加载失败：${error instanceof Error ? error.message : '模型不可用'}，请重新导入`)
    })
    return () => {
      cancelled = true
      downloads.abort()
      motionLoader?.dispose()
      runtime.setMotionLoader(null)
      runtime.setProceduralReady([])
      wavePlayer.current?.dispose()
      wavePlayer.current = null
      runtime.clearImportedMotions()
      runtime.stopMotion()
      vrmRef.current = null
      if (model) {
        model.scene.removeFromParent()
        VRMUtils.deepDispose(model.scene)
      }
    }
  }, [url, runtime, onStatus, onMotionStatus])

  // One owner blends mouth, expression and skeleton; no React state per frame.
  useFrame(({ clock, camera, scene, gl }, rawDt) => {
    const dt = Math.min(rawDt, 0.1)
    const t = clock.elapsedTime
    const now = performance.now() / 1000
    const level = runtime.readLevel()
    runtime.update(now, level)
    const gazeOffset = gaze.current.update(runtime.state, now)
    mouth.current = smooth(mouth.current, runtime.listening ? 0 : mouthTarget(level), 18, dt)
    if (idleMotion && t > nextBlink.current) {
      blinkStarted.current = t
      nextBlink.current = t + 2.5 + Math.random() * 3
    }
    const blinkAge = t - blinkStarted.current
    blink.current = !idleMotion ? 0 : blinkAge < 0.06 ? blinkAge / 0.06 : Math.max(0, 1 - (blinkAge - 0.06) / 0.12)
    const elapsed = now - runtime.motionStarted
    const duration = runtime.getMotionDuration()
    runtime.finishMotion(now)
    const active = runtime.motion !== 'idle'
    const envelope = active ? Math.min(1, elapsed / 0.3, Math.max(0, (duration - elapsed) / 0.4)) : 0
    const dance = runtime.motion === 'dance' ? envelope : 0
    const wave = runtime.motion === 'wave' && !wavePlayer.current?.getDuration('wave') ? envelope : 0
    const nod = runtime.motion === 'nod' ? envelope : 0
    const vrm = vrmRef.current
    if (root.current) {
      root.current.scale.setScalar(smooth(root.current.scale.x, 1 / runtime.zoom, 6, dt))
      root.current.rotation.z = smooth(root.current.rotation.z, Math.sin(t * 4) * 0.07 * dance, 10, dt)
      root.current.position.y = smooth(root.current.position.y, Math.abs(Math.sin(t * 4)) * 0.025 * dance, 10, dt)
    }
    if (vrm) {
      wavePlayer.current?.restoreBase()
      const manager = vrm.expressionManager
      manager?.setValue('aa', mouth.current)
      manager?.setValue('blink', blink.current)
      EMOTIONS.forEach((emotion, index) => {
        // The model combines mouth and eyes in each preset; moderate attenuation
        // keeps expressions visible while leaving room for speech articulation.
        const target = emotion !== 'neutral' && runtime.emotion === emotion && now < runtime.emotionUntil
          ? expressionTarget(emotion, runtime.emotionWeight, mouth.current) : 0
        const weight = smooth(emotionWeights.current[index], target, expressionSpeed(emotion, target > emotionWeights.current[index]), dt)
        emotionWeights.current[index] = weight
        const expression = emotion === 'surprised' && !manager?.getExpression(emotion)
          ? 'Surprised' : emotion
        manager?.setValue(expression, weight)
      })
      const bone = (name: 'head' | 'neck' | 'chest' | 'leftUpperArm' | 'rightUpperArm' | 'rightLowerArm') => vrm.humanoid.getNormalizedBoneNode(name)
      const head = bone('head')
      if (head) {
        head.rotation.x = smooth(head.rotation.x, (idleMotion ? (runtime.state === 'listening' ? 0.035 : runtime.state === 'thinking' ? -0.02 : 0) : 0) + nod * Math.sin(elapsed * 8) * 0.15, 8, dt)
        head.rotation.y = smooth(head.rotation.y, idleMotion ? gazeOffset.x * 0.12 : 0, 8, dt)
      }
      const chest = bone('chest')
      if (chest) chest.rotation.x = smooth(chest.rotation.x, idleMotion ? Math.sin(t * 1.7) * 0.012 : 0, 8, dt)
      const left = bone('leftUpperArm')
      const right = bone('rightUpperArm')
      const forearm = bone('rightLowerArm')
      if (left) left.rotation.z = smooth(left.rotation.z, armRest.current.left.z + dance * Math.sin(t * 4) * 0.35, 9, dt)
      if (right) right.rotation.z = smooth(right.rotation.z, armRest.current.right.z + armRest.current.right.side * wave * 2.1 - dance * Math.sin(t * 4) * 0.35, 9, dt)
      if (forearm) forearm.rotation.y = smooth(forearm.rotation.y, wave * (0.8 + Math.sin(elapsed * 12) * 0.35), 10, dt)
      wavePlayer.current?.apply(runtime, now)
      if (vrm.lookAt) {
        vrm.lookAt.autoUpdate = idleMotion
        if (idleMotion) {
          camera.getWorldPosition(gazeDesired.current)
          gazeDesired.current.x += gazeOffset.x
          gazeDesired.current.y += gazeOffset.y
          if (!gazeReady.current) {
            gazePoint.current.position.copy(gazeDesired.current)
            gazeReady.current = true
          }
          gazePoint.current.position.lerp(gazeDesired.current, 1 - Math.exp(-5 * dt))
          gazePoint.current.updateMatrixWorld()
          vrm.lookAt.target = gazePoint.current
        } else {
          vrm.lookAt.target = null
          vrm.lookAt.reset()
        }
      }
      vrm.update(dt)
    }
    if (mouthMesh.current) mouthMesh.current.scale.y = 0.12 + mouth.current * 1.5
    if (eyeGroup.current) eyeGroup.current.scale.y = Math.max(0.08, 1 - blink.current)
    if (heart.current) {
      heart.current.visible = runtime.motion === 'heartbeat'
      heart.current.scale.setScalar(0.8 + Math.sin(elapsed * 8) * 0.1)
    }
    background.current.set(SCENES[runtime.scene])
    scene.background = background.current
    // Match the selected quality budget; animation weights remain time based.
    renderTime.current += rawDt
    if (renderTime.current >= 1 / maxFps) {
      gl.render(scene, camera)
      renderTime.current %= 1 / maxFps
    }
  }, 1)

  return (
    <group ref={root}>
      {!url && <group position={[0, 1.1, 0]}>
        <mesh position={[0, -0.38, 0]}><capsuleGeometry args={[0.22, 0.4, 4, 12]} /><meshStandardMaterial color="#8196cd" /></mesh>
        <mesh><sphereGeometry args={[0.29, 24, 16]} /><meshStandardMaterial color="#e7ecff" /></mesh>
        <group ref={eyeGroup}>
          {[-0.1, 0.1].map((x) => <mesh key={x} position={[x, 0.04, 0.26]}><sphereGeometry args={[0.035, 12, 8]} /><meshStandardMaterial color="#273456" /></mesh>)}
        </group>
        <group ref={mouthMesh} position={[0, -0.1, 0.275]}><mesh><sphereGeometry args={[0.055, 12, 8]} /><meshStandardMaterial color="#624363" /></mesh></group>
      </group>}
      <group ref={heart} position={[0.45, 1.45, 0.2]} visible={false}>
        {[-1, 1].map((side) => <mesh key={side} position={[side * 0.045, 0, 0]} rotation={[0, 0, side * -0.6]}><capsuleGeometry args={[0.065, 0.09, 4, 8]} /><meshStandardMaterial color="#f471a1" /></mesh>)}
      </group>
    </group>
  )
}

export default function AvatarStage({ runtime }: { runtime: AvatarRuntime }) {
  const [loadingMotion, setLoadingMotion] = useState<string | null>(null)
  const preview = async (id: string) => {
    setLoadingMotion(id)
    try { await runtime.previewMotion(id) } finally { setLoadingMotion(null) }
  }
  const [model, setModel] = useState<{ url: string; name: string } | null>(DEFAULT_MODEL)
  const [status, setStatus] = useState('正在加载默认角色 AvatarSample_2…')
  const [visible, setVisible] = useState(true)
  const [waveStatus, setWaveStatus] = useState('正在加载挥手动画…')
  const [quality, setQuality] = useState<'sharp' | 'smooth'>('sharp')
  const [idleMotion, setIdleMotion] = useState(true)
  const [framing, setFraming] = useState<'portrait' | 'full'>('portrait')
  useEffect(() => () => {
    if (model?.url.startsWith('blob:')) URL.revokeObjectURL(model.url)
  }, [model])

  return <section className="avatar-stage" aria-label="3D 数字人">
    <div className="avatar-toolbar">
      <label className="avatar-upload">导入 VRM<input type="file" accept=".vrm" onChange={(event) => {
        const file = event.target.files?.[0]
        if (!file) return
        if (file.size > 50 * 1024 * 1024) { setStatus('请使用小于 50 MB 的优化模型'); return }
        setStatus('正在加载角色…')
        setModel({ url: URL.createObjectURL(file), name: file.name })
        event.target.value = ''
      }} /></label>
      <span className="avatar-model-name">{model?.name ?? '预览角色'}</span>
      <label className="avatar-quality">画质
        <select aria-label="角色画质" value={quality} onChange={(event) => setQuality(event.target.value as 'sharp' | 'smooth')}>
          <option value="sharp">清晰</option>
          <option value="smooth">流畅</option>
        </select>
      </label>
      <label className="avatar-quality">视角
        <select aria-label="角色视角" value={framing} onChange={(event) => setFraming(event.target.value as 'portrait' | 'full')}>
          <option value="portrait">半身</option><option value="full">全身</option>
        </select>
      </label>
      <button type="button" aria-pressed={idleMotion} onClick={() => setIdleMotion(!idleMotion)}>{idleMotion ? '自然待机：开' : '自然待机：关'}</button>
      <button type="button" onClick={() => {
        setStatus('正在加载默认角色 AvatarSample_2…')
        setModel(DEFAULT_MODEL)
      }} disabled={model === DEFAULT_MODEL}>默认角色</button>
      <button type="button" onClick={() => setVisible(!visible)}>{visible ? '隐藏角色' : '显示角色'}</button>
    </div>
    {visible && <StageBoundary><div className="avatar-canvas">
      <Canvas flat dpr={quality === 'sharp' ? [1.5, 2] : 1} camera={{ position: [0, 1.45, 1.65], fov: 35 }} gl={{ antialias: true }} fallback={<div>浏览器不支持 WebGL，语音仍可使用。</div>}>
        <ambientLight intensity={0.65} />
        <directionalLight position={[-2, 3, 4]} intensity={1.1} />
        <directionalLight position={[3, 1.5, 2]} intensity={0.25} />
        <CameraControls framing={framing} />
        <Character key={model?.url ?? 'placeholder'} url={model?.url ?? null} runtime={runtime} onStatus={setStatus} onMotionStatus={setWaveStatus} idleMotion={idleMotion} maxFps={quality === 'sharp' ? 60 : 30} />
      </Canvas>
    </div></StageBoundary>}
    <p className="avatar-status" role="status">{status}</p>
    <p className="avatar-status" role="status">{waveStatus}</p>
    <p className="avatar-status">拖动旋转，滚轮缩放；切换半身／全身可恢复正面视角。自然待机按聆听、思考和说话状态变化，可关闭对比静态效果。</p>
    <div className="avatar-controls" aria-label="表情与动作预览">
      {EMOTIONS.map((emotion, i) => <button key={emotion} type="button" disabled={!model} onClick={() => runtime.express(emotion)}>{LABELS[i]}</button>)}
      {MOTION_CATALOG.map(({ id, label }) => <button key={id} type="button"
        disabled={loadingMotion !== null || (!model && id !== 'heartbeat')}
        onClick={() => { void preview(id) }}>{loadingMotion === id ? '加载中…' : label}</button>)}
      <button type="button" onClick={() => runtime.stopMotion()}>停止动作</button>
      <button type="button" onClick={() => runtime.act('zoom_in')}>拉近</button>
      <button type="button" onClick={() => runtime.act('zoom_out')}>拉远</button>
      <button type="button" onClick={() => runtime.act('reset_default')}>复位</button>
    </div>
  </section>
}
