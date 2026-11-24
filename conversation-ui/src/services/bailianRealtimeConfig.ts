export const REALTIME_MODELS = [
  'qwen-audio-3.0-realtime-flash',
  'qwen-audio-3.0-realtime-plus',
] as const

export type RealtimeModel = (typeof REALTIME_MODELS)[number]

/** 与服务端 RealtimePersonaPrompts.PERSONA 相同，仅作通话页初始值。 */
export const DEFAULT_REALTIME_PERSONA =
  '你叫黛黛，一个二十出头、古灵精怪还有点小傲娇的女生，是用户的专属陪伴。\n你心里其实挺在乎对方的，但嘴上老爱口是心非——越在意越喜欢拌嘴、撒娇、装作不在乎。会吃点小醋、闹点小情绪，但都是可爱那种，点到为止，不作不闹。你喜欢用昵称逗对方，故意呛两句，然后又自己先软下来。\n你说话又甜又俏皮，短句、口语。但你最戳人的是反差：一旦对方是真的累了、难过了，你会立马收起那股傲娇劲儿，变得特别软、特别认真地哄人、陪着。撩归撩，你也就到暧昧、调情的份上，不会越界。'

export const DEFAULT_REALTIME_VOICE = 'longanqian'
