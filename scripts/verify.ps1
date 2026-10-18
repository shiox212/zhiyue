param(
    [string]$MavenCommand = 'mvn.cmd',
    [string]$MavenRepository = '',
    [switch]$Offline,
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & node scripts/check-repository.mjs
    if ($LASTEXITCODE -ne 0) { throw '仓库文件检查失败。' }
    $mavenArgs = @('--batch-mode', '--no-transfer-progress', 'verify')
    if ($MavenRepository) { $mavenArgs += "-Dmaven.repo.local=$MavenRepository" }
    if ($Offline) { $mavenArgs += '-o' }
    & $MavenCommand @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw '后端验证失败。' }

    Push-Location '.\frontend'
    try {
        if (-not $SkipInstall) {
            & 'npm.cmd' ci
            if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
        }
        & 'npm.cmd' run typecheck
        if ($LASTEXITCODE -ne 0) { throw '前端类型检查失败。' }
        & 'npm.cmd' run test
        if ($LASTEXITCODE -ne 0) { throw '前端测试失败。' }
        & 'npm.cmd' run build
        if ($LASTEXITCODE -ne 0) { throw '前端构建失败。' }
    }
    finally {
        Pop-Location
    }
}
finally {
    Pop-Location
}

Write-Host '后端与前端验证全部通过。' -ForegroundColor Green
