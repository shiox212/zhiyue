// AI-assisted: checks Git's index, so both CI and pre-commit local checks see staged files.
import { execFileSync } from 'node:child_process'

const paths = execFileSync('git', ['ls-files', '-z'], { encoding: 'utf8' })
  .split('\0').filter(Boolean)
const blocked = paths.filter(path => {
  const name = path.split('/').at(-1)
  const secret = (name === '.env' || name.startsWith('.env.')) && name !== '.env.example'
  const generated = /(^|\/)(node_modules|target|dist|coverage|test-results|playwright-report)\//.test(path)
  const databaseBackup = /(^|\/)database\/backups\//.test(path)
  return secret || generated || databaseBackup
})

if (blocked.length) {
  console.error('Repository path check failed. Unstage local secrets, backups or generated files:')
  for (const path of blocked) console.error(`- ${path}`)
  process.exitCode = 1
} else {
  console.log(`Repository path check passed (${paths.length} indexed files).`)
}
