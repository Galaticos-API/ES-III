param(
    [string]$JdkPath = $env:JAVA_HOME
)

# Execute no PowerShell normal. Usa analista fixo: nao instala nem chama Ollama.
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JdkPath)) {
    $JdkPath = Join-Path $env:USERPROFILE '.jdks\ms-25.0.0'
}
if (!(Test-Path -LiteralPath (Join-Path $JdkPath 'bin\java.exe'))) {
    throw 'Informe um JDK 21 ou superior: .\verificar-interface.ps1 -JdkPath "C:\caminho\jdk"'
}
$env:JAVA_HOME = $JdkPath
$env:Path = (Join-Path $JdkPath 'bin') + ';' + $env:Path

& .\mvnw.cmd test
if ($LASTEXITCODE -ne 0) { throw 'Os testes falharam. Confira a saida antes de gerar as capturas.' }

$reportPath = Join-Path $PSScriptRoot 'target\surefire-reports\TEST-br.fatec.esiii.socagent.service.IncidentTriageServiceTest.xml'
[xml]$report = Get-Content -LiteralPath $reportPath -Raw
$classpath = ($report.testsuite.properties.property | Where-Object name -eq 'java.class.path').value
if ([string]::IsNullOrWhiteSpace($classpath)) { throw 'Classpath dos testes nao encontrado no relatorio.' }

$outputDirectory = Join-Path $PSScriptRoot 'target\ui-snapshots'
foreach ($theme in @('claro', 'escuro')) {
    foreach ($state in @('vazio', 'aguardando_aprovacao', 'encerrado', 'revisao', 'negado', 'falso_positivo')) {
        & (Join-Path $JdkPath 'bin\java.exe') -cp $classpath `
            br.fatec.esiii.socagent.gui.UiSnapshot $outputDirectory $theme $state
        if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar captura: $theme / $state" }
    }
}
Write-Host "Testes concluidos. Capturas geradas em: $outputDirectory"
Write-Host 'Abra as imagens para conferir layout, detalhes e etapas nao realizadas. Elas usam dados simulados.'
