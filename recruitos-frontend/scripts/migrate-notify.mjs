#!/usr/bin/env node
/**
 * Mechanical migration: ElMessage / ElMessageBox imports → toast / confirm
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'src')

function walk(dir, acc = []) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name)
    const st = fs.statSync(p)
    if (st.isDirectory()) walk(p, acc)
    else if (/\.(vue|ts)$/.test(name)) acc.push(p)
  }
  return acc
}

const files = walk(root)
let changed = 0

for (const file of files) {
  let s = fs.readFileSync(file, 'utf8')
  const orig = s

  s = s.replace(/\bElMessage\.success\(/g, 'toast.success(')
  s = s.replace(/\bElMessage\.error\(/g, 'toast.error(')
  s = s.replace(/\bElMessage\.warning\(/g, 'toast.error(')
  s = s.replace(/\bElMessage\.info\(/g, 'toast.info(')

  if (/ElMessage|ElMessageBox/.test(s)) {
    s = s.replace(
      /import\s*\{([^}]*)\}\s*from\s*['"]element-plus['"]/g,
      (m, imports) => {
        const parts = imports.split(',').map((x) => x.trim()).filter(Boolean)
        const rest = parts.filter((p) => !/^ElMessage(Box)?$/.test(p))
        const needsToast = parts.some((p) => /^ElMessage$/.test(p))
        const needsConfirm = parts.some((p) => /^ElMessageBox$/.test(p))
        const lines = []
        if (needsToast) lines.push("import { toast } from '@/lib/notify'")
        if (needsConfirm) lines.push("import { confirm } from '@/lib/confirm'")
        if (rest.length) lines.push(`import { ${rest.join(', ')} } from 'element-plus'`)
        return lines.join('\n')
      }
    )
  }

  if (s !== orig) {
    fs.writeFileSync(file, s)
    changed++
    console.log('updated', path.relative(root, file))
  }
}

console.log(`Done. ${changed} files updated.`)
