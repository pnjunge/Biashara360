export type PurchaseIngredient = { unit:string; purchaseUnit?:string|null; purchaseUnitSize?:number }
export function purchaseFactor(ingredient:PurchaseIngredient,unit:string):number|null {
  const base=ingredient.unit.trim().toUpperCase(), purchase=unit.trim().toUpperCase()
  if (base===purchase) return 1
  const units:Record<string,[string,number]>={G:['MASS',1],KG:['MASS',1000],ML:['VOLUME',1],L:['VOLUME',1000]}
  if(units[base] && units[purchase] && units[base][0]===units[purchase][0]) return units[purchase][1]/units[base][1]
  if(['BOTTLE','PACK','CASE'].includes(purchase) && ingredient.purchaseUnit===purchase && Number.isFinite(ingredient.purchaseUnitSize) && ingredient.purchaseUnitSize!>0) return ingredient.purchaseUnitSize!
  return null
}
export function purchaseUnits(ingredient:PurchaseIngredient):string[] {
  const base=ingredient.unit.trim().toUpperCase()
  const units=[base,...(['G','KG'].includes(base)?['G','KG']:['ML','L'].includes(base)?['ML','L']:[])]
  if(ingredient.purchaseUnit) units.push(ingredient.purchaseUnit)
  return [...new Set(units)]
}
