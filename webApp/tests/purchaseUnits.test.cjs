const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const Module = require('node:module')
const ts = require('typescript')
const filename = path.resolve(__dirname, '../src/utils/purchaseUnits.ts')
const mod = new Module(filename, module)
mod._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText, filename)
const { purchaseFactor, purchaseUnits } = mod.exports

test('standard mass and volume purchases preserve their physical dimensions', () => {
 assert.equal(purchaseFactor({unit:'G'},'KG'),1000)
 assert.equal(purchaseFactor({unit:'ML'},'L'),1000)
 assert.equal(purchaseFactor({unit:'KG'},'G'),.001)
 assert.equal(purchaseFactor({unit:'G'},'L'),null)
 assert.equal(purchaseFactor({unit:'ML'},'KG'),null)
})
test('bottles and packs require the configured ingredient pack size', () => {
 const spirit={unit:'ML',purchaseUnit:'BOTTLE',purchaseUnitSize:750}
 assert.equal(purchaseFactor(spirit,'BOTTLE'),750)
 assert.equal(purchaseFactor(spirit,'CASE'),null)
 assert.equal(purchaseFactor({unit:'ML'},'BOTTLE'),null)
 assert.equal(purchaseFactor({...spirit,purchaseUnitSize:0},'BOTTLE'),null)
 assert.deepEqual(purchaseUnits(spirit),['ML','L','BOTTLE'])
 assert.deepEqual(purchaseUnits({unit:'G'}),['G','KG'])
})
