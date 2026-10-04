const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const Module = require('node:module')
const ts = require('typescript')
const filename = path.resolve(__dirname, '../src/utils/categoryImage.ts')
const mod = new Module(filename, module)
mod._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, filename)
const { prepareCategoryImage } = mod.exports

test('rejects unsupported files and empty or oversized uploads before decoding', async () => {
  await assert.rejects(prepareCategoryImage({type:'image/svg+xml',size:100}), /JPEG, PNG or WebP/)
  for (const size of [0, 5 * 1024 * 1024 + 1]) await assert.rejects(prepareCategoryImage({type:'image/png',size}), /5 MB/)
})

test('resizes proportionally and releases temporary image URLs on success and failure', async t => {
  const released = []
  t.mock.method(URL, 'createObjectURL', () => 'blob:category')
  t.mock.method(URL, 'revokeObjectURL', url => released.push(url))
  const previousImage = global.Image
  const previousDocument = global.document
  t.after(() => { global.Image = previousImage; global.document = previousDocument })
  const canvas = { width:0, height:0, getContext:() => ({ fillRect(){}, drawImage(){} }), toDataURL:() => 'data:image/jpeg;base64,YQ==' }
  global.document = { createElement: () => canvas }
  global.Image = class { naturalWidth = 1600; naturalHeight = 800; set src(value) { this.onload() } }
  assert.equal(await prepareCategoryImage({type:'image/png',size:100}), 'data:image/jpeg;base64,YQ==')
  assert.equal(canvas.width, 512)
  assert.equal(canvas.height, 256)
  global.Image = class { set src(value) { this.onerror() } }
  await assert.rejects(prepareCategoryImage({type:'image/jpeg',size:100}), /could not be read/)
  assert.deepEqual(released, ['blob:category', 'blob:category'])
})
