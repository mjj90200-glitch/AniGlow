#!/usr/bin/env node
import { execFileSync } from 'child_process'

const DB = process.env.MYSQL_DATABASE || 'aniglow'
const DB_USER = process.env.MYSQL_USER || 'root'
const DB_PASS = process.env.MYSQL_PASSWORD
const ARK_BASE = `${process.env.AI_BASE_URL || 'https://ark.cn-beijing.volces.com/api/coding/v3'}/chat/completions`
const MODEL = process.env.AI_MODEL || 'doubao-seed-2.0-lite'
const API_KEY = process.env.AI_API_KEY

if (!DB_PASS || !API_KEY) {
  throw new Error('MYSQL_PASSWORD and AI_API_KEY are required')
}

function mysql(cmd) {
  return execFileSync('mysql', ['-u', DB_USER, '-N', '-s', '-e', cmd, DB], {
    encoding: 'utf-8', maxBuffer: 50 * 1024 * 1024,
    env: { ...process.env, MYSQL_PWD: DB_PASS },
  }).trim()
}

async function translate(titleJapanese, title, retries = 2) {
  const source = (titleJapanese && titleJapanese.trim()) || (title && title.trim()) || ''
  if (!source) return null

  for (let r = 0; r <= retries; r++) {
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 60000)
    try {
      const res = await fetch(ARK_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${API_KEY}` },
        body: JSON.stringify({
          model: MODEL,
          messages: [
            { role: 'system', content: '你是专业动漫翻译。将动漫标题翻译成中文。直接输出中文译名，不加解释、前缀或标点。如果标题有公认中文译名，使用该译名。只输出译名本身。' },
            { role: 'user', content: source }
          ],
          max_tokens: 30, temperature: 0.1,
        }),
        signal: ctrl.signal,
      })
      const json = await res.json()
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const content = json?.choices?.[0]?.message?.content?.trim()
      if (content) return content.replace(/["""'']/g, '').replace(/^[：:。【】\s]+|[：:。【】\s]+$/g, '').trim()
      return null
    } catch (e) {
      if (r < retries) { console.log(`    重试 ${r+1}/${retries}...`); await new Promise(r => setTimeout(r, 3000)) }
      else throw e
    } finally { clearTimeout(timer) }
  }
}

async function main() {
  const rows = mysql("SELECT id, mal_id, title, title_japanese FROM anime WHERE title_cn IS NULL OR title_cn = ''")
  if (!rows) { console.log('全部完成！'); return }

  const lines = rows.split('\n').filter(Boolean)
  console.log(`剩余 ${lines.length} 部番剧需要翻译\n`)

  let success = 0, fail = 0
  const start = Date.now()

  for (let i = 0; i < lines.length; i++) {
    const [id, malId, title, titleJapanese] = lines[i].split('\t')
    const src = (titleJapanese || title || '').substring(0, 45)

    try {
      const cn = await translate(titleJapanese, title)
      if (cn && cn.length > 0 && cn.length < 100) {
        const escaped = cn.replace(/\\/g, '\\\\').replace(/'/g, "\\'")
        mysql(`UPDATE anime SET title_cn = '${escaped}' WHERE id = ${id}`)
        success++
        const elapsed = Math.round((Date.now() - start) / 1000)
        const eta = success > 0 ? Math.round(elapsed / success * (lines.length - i - 1)) : 0
        console.log(`[${success}/${lines.length}] ✓ MAL#${malId} → "${cn}"  (${src})  ${eta}s剩`)
      } else {
        fail++
        console.log(`[${i+1}/${lines.length}] ✗ MAL#${malId} 翻译为空`)
      }
    } catch (e) {
      fail++
      console.log(`[${i+1}/${lines.length}] ✗ MAL#${malId} 失败: ${e.message}`)
    }
  }

  const t = Math.round((Date.now() - start) / 1000)
  console.log(`\n完成！成功${success} 失败${fail} 耗时${t}s`)
}

main().catch(console.error)
