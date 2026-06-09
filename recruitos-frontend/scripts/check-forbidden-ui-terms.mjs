#!/usr/bin/env node
/**
 * 扫描 <template> 内用户可见文本中的禁用工程词
 * 用法: npm run check:terms
 */
import { readFileSync, readdirSync, statSync } from 'fs'
import { join, relative } from 'path'
import { fileURLToPath } from 'url'

const ROOT = join(fileURLToPath(new URL('.', import.meta.url)), '..')
const SCAN_DIRS = ['src/views', 'src/components', 'src/config']

const FORBIDDEN = [
  'OpsPack', 'CARD_GREET', 'SCREEN_THEN_GREET', 'COLLECT_ONLY', 'FULL_AUTO',
  'CommunicationProfile', '策略进化', '运营包', 'Agent任务', '决策面板',
  'ROLLBACK', '获客', '寻源', '入库',
]

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) {
      if (name === 'node_modules') continue
      walk(p, out)
    } else if (/\.vue$/.test(name)) {
      out.push(p)
    }
  }
  return out
}

function extractTemplate(vueContent) {
  const m = vueContent.match(/<template[^>]*>([\s\S]*?)<\/template>/i)
  return m ? m[1] : ''
}

function visibleSnippets(template) {
  const snippets = []
  // 标签间纯文本（排除绑定与组件）
  const textRe = />([^<]+)</g
  let m
  while ((m = textRe.exec(template)) !== null) {
    const t = m[1].trim()
    if (t && !t.startsWith('{{') && !/^\s*$/.test(t)) {
      snippets.push(t)
    }
  }
  // 静态 label/title/description（非 : 绑定，且非枚举 value）
  const attrRe = /(?:^|\s)(label|title|placeholder|description)=["']([^"']+)["']/gm
  while ((m = attrRe.exec(template)) !== null) {
    const val = m[2]
    if (/^[A-Z][A-Z0-9_]+$/.test(val)) continue // radio value，展示用人话函数
    snippets.push(val)
  }
  return snippets
}

function containsForbidden(text) {
  const hits = []
  for (const term of FORBIDDEN) {
    if (/^[A-Z_]+$/.test(term)) {
      if (text.includes(term)) hits.push(term)
    } else if (text.includes(term)) {
      hits.push(term)
    }
  }
  return hits
}

const files = SCAN_DIRS.flatMap((d) => walk(join(ROOT, d)))
const violations = []

for (const file of files) {
  const rel = relative(ROOT, file).replace(/\\/g, '/')
  const tpl = extractTemplate(readFileSync(file, 'utf8'))
  for (const snippet of visibleSnippets(tpl)) {
    const hits = containsForbidden(snippet)
    for (const term of hits) {
      violations.push({ file: rel, term, snippet })
    }
  }
}

if (violations.length) {
  console.error(`\n❌ 发现 ${violations.length} 处用户可见禁用词:\n`)
  for (const v of violations) {
    console.error(`  ${v.file}  [${v.term}]  ${v.snippet}`)
  }
  process.exit(1)
}

console.log('✅ 用户可见文案未发现禁用工程词')
