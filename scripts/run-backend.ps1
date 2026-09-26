$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    if (-not (Test-Path -LiteralPath '.env')) {
        throw '未找到 .env。请复制 .env.example 为 .env，并填写本机数据库配置。'
    }

    if (Test-Path -LiteralPath '.\mvnw.cmd') {
        & '.\mvnw.cmd' spring-boot:run
    }
    else {
        $mavenCommand = Get-Command 'mvn.cmd' -ErrorAction SilentlyContinue
        if ($null -eq $mavenCommand) {
            throw '未找到 Maven。请先安装 Maven 并把 bin 目录加入 PATH，或在 IntelliJ IDEA 中运行启动类。'
        }
        & $mavenCommand.Source spring-boot:run
    }

    if ($LASTEXITCODE -ne 0) {
        throw '后端启动失败，请检查控制台中的首个错误。'
    }
}
finally {
    Pop-Location
}
