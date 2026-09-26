$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    if (Test-Path -LiteralPath '.\mvnw.cmd') {
        & '.\mvnw.cmd' verify
    }
    else {
        & 'mvn' verify
    }
    if ($LASTEXITCODE -ne 0) { throw '后端验证失败。' }

    Push-Location '.\frontend'
    try {
        & 'npm' ci
        if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
        & 'npm' run lint
        if ($LASTEXITCODE -ne 0) { throw '前端 lint 失败。' }
        & 'npm' run typecheck
        if ($LASTEXITCODE -ne 0) { throw '前端类型检查失败。' }
        & 'npm' run test
        if ($LASTEXITCODE -ne 0) { throw '前端测试失败。' }
        & 'npm' run build
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

