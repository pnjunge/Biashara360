export const unitsOfMeasure = [
  {value:'PCS',label:'Pieces (pcs)'}, {value:'KG',label:'Kilograms (kg)'},
  {value:'G',label:'Grams (g)'}, {value:'L',label:'Litres (L)'},
  {value:'ML',label:'Millilitres (ml)'}, {value:'BOTTLE',label:'Bottles'},
  {value:'PACK',label:'Packs'}, {value:'BOX',label:'Boxes'}, {value:'CASE',label:'Cases'},
  {value:'PORTION',label:'Portions'}, {value:'TOT',label:'Tots'},
  {value:'PLATE',label:'Plates'}, {value:'CUP',label:'Cups'},
]
export function unitOptions(current:string) {
  return unitsOfMeasure.some(unit=>unit.value===current)?unitsOfMeasure:[...unitsOfMeasure,{value:current,label:current}]
}
