import { Matrix4, Vector3 } from 'three'
import { createVrmRig } from './vrm-rig.ts'

/** CPU skin the actual dress vertices; no textures, WebGL or proxy cylinder. */
export function createDressProbe(rig: ReturnType<typeof createVrmRig>) {
  const { gltf, buffer, nodes } = rig
  const binaryOffset = 20 + buffer.readUInt32LE(12) + 8
  const sizes: Record<number, number> = { 5121: 1, 5123: 2, 5125: 4, 5126: 4 }
  const widths: Record<string, number> = { SCALAR: 1, VEC3: 3, VEC4: 4, MAT4: 16 }
  const read = (id: number, index: number) => {
    const accessor = gltf.accessors[id], view = gltf.bufferViews[accessor.bufferView]
    const size = sizes[accessor.componentType], width = widths[accessor.type]
    const offset = binaryOffset + (view.byteOffset ?? 0) + (accessor.byteOffset ?? 0) + index * (view.byteStride ?? size * width)
    return Array.from({ length: width }, (_, i) => {
      const at = offset + i * size
      return accessor.componentType === 5126 ? buffer.readFloatLE(at)
        : accessor.componentType === 5125 ? buffer.readUInt32LE(at)
        : accessor.componentType === 5123 ? buffer.readUInt16LE(at) : buffer.readUInt8(at)
    })
  }
  const entries: { point: Vector3; joints: number[]; weights: number[]; skin: number }[] = []
  const faces: number[][] = []
  for (const node of gltf.nodes) {
    if (node.mesh === undefined || node.skin === undefined) continue
    for (const primitive of gltf.meshes[node.mesh].primitives) {
      if (!gltf.materials[primitive.material]?.name.includes('Onepice')) continue
      const indices = new Set<number>()
      for (let i = 0; i < gltf.accessors[primitive.indices].count; i++) indices.add(read(primitive.indices, i)[0])
      const mapped = new Map<number, number>()
      for (const index of indices) {
        const joints = read(primitive.attributes.JOINTS_0, index), weights = read(primitive.attributes.WEIGHTS_0, index)
        // Exclude moving sleeves when measuring the dress body/skirt envelope.
        if (joints.some((joint, i) => weights[i] > 0.05 && /Arm|Hand|Shoulder/.test(nodes[gltf.skins[node.skin].joints[joint]].name))) continue
        mapped.set(index, entries.length)
        entries.push({ point: new Vector3().fromArray(read(primitive.attributes.POSITION, index)), joints, weights, skin: node.skin })
      }
      for (let i = 0; i < gltf.accessors[primitive.indices].count; i += 3) {
        const face = [0, 1, 2].map(j => mapped.get(read(primitive.indices, i + j)[0]))
        if (face.every(index => index !== undefined)) faces.push(face as number[])
      }
    }
  }
  const inverses = gltf.skins.map((skin: any) => skin.joints.map((_: unknown, i: number) => new Matrix4().fromArray(read(skin.inverseBindMatrices, i)))) as Matrix4[][]
  const vertices = () => {
    rig.humanoid.update(); rig.scene.updateMatrixWorld(true)
    return entries.map(entry => {
      const output = new Vector3()
      entry.joints.forEach((joint, i) => {
        if (!entry.weights[i]) return
        const node = nodes[gltf.skins[entry.skin].joints[joint]]
        output.addScaledVector(entry.point.clone().applyMatrix4(inverses[entry.skin][joint]).applyMatrix4(node.matrixWorld), entry.weights[i])
      })
      return output
    })
  }
  return Object.assign(vertices, { triangles: () => {
    const points = vertices()
    return faces.map(face => face.map(index => points[index]))
  } })
}
