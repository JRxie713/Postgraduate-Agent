import fs from 'node:fs'

const file = process.argv[2]
const r = JSON.parse(fs.readFileSync(file, 'utf8'))
const a = r.audits
const n = (id, key = 'numericValue') => (a[id] && a[id][key] != null ? Math.round(a[id][key]) : null)
const fmt = (v, unit = 'ms') => (v == null ? '-' : `${v} ${unit}`)

console.log(`== ${file}`)
console.log('formFactor      ', r.configSettings.formFactor)
console.log('performance     ', Math.round((r.categories.performance.score || 0) * 100))
console.log('FCP             ', fmt(n('first-contentful-paint')))
console.log('LCP             ', fmt(n('largest-contentful-paint')))
console.log('TBT             ', fmt(n('total-blocking-time')))
console.log('CLS             ', a['cumulative-layout-shift'].displayValue)
console.log('SI              ', fmt(n('speed-index')))
console.log('TTI             ', fmt(n('interactive')))
console.log('serverResponse  ', fmt(n('server-response-time')))
console.log('totalByteWeight ', fmt(n('total-byte-weight', 'numericValue') && Math.round(n('total-byte-weight') / 1024), 'KB'))
console.log('mainthread      ', fmt(n('mainthread-work-breakdown')), ' / script:', fmt(n('bootup-time')))

console.log('-- resources (top by transfer size)')
const items = (a['network-requests'] && a['network-requests'].details.items) || []
items
  .map((i) => ({
    url: i.url.replace(/^https?:\/\/[^/]+/, ''),
    type: i.resourceType,
    transfer: i.transferSize || 0,
    size: i.resourceSize || 0
  }))
  .sort((x, y) => y.transfer - x.transfer)
  .slice(0, 10)
  .forEach((i) => console.log(`  ${i.type.padEnd(10)} ${(i.transfer / 1024).toFixed(1).padStart(7)} KB  ${i.url}`))

console.log('-- opportunities')
for (const [id, audit] of Object.entries(a)) {
  if (audit.details && audit.details.type === 'opportunity' && audit.numericValue > 0) {
    console.log(`  ${id}: ${audit.displayValue || ''}`)
  }
}
