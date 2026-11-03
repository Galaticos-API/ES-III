# Empacota somente fontes e documentacao; nao inclui credenciais, .git ou target.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$output = Join-Path $root 'target\entrega'
$staging = Join-Path $output ('preparo-' + [guid]::NewGuid().ToString('N'))
$project = Join-Path $staging 'ES-III'
New-Item -ItemType Directory -Force -Path $project | Out-Null
foreach ($name in @('pom.xml', 'mvnw', 'mvnw.cmd', 'README.md', 'LICENSE',
                    'verificar-interface.ps1', 'preparar-entrega.ps1')) {
    Copy-Item -LiteralPath (Join-Path $root $name) -Destination $project
}
Copy-Item -LiteralPath (Join-Path $root 'src') -Destination $project -Recurse
$wrapper = Join-Path $project '.mvn\wrapper'
New-Item -ItemType Directory -Force -Path $wrapper | Out-Null
Copy-Item -LiteralPath (Join-Path $root '.mvn\wrapper\maven-wrapper.properties') -Destination $wrapper
$docs = Join-Path $project 'docs'
New-Item -ItemType Directory -Force -Path $docs | Out-Null
Get-ChildItem -LiteralPath (Join-Path $root 'docs') -File -Filter '*.md' |
    Copy-Item -Destination $docs
Copy-Item -LiteralPath (Join-Path $root 'docs\uml') -Destination $docs -Recurse
Copy-Item -LiteralPath (Join-Path $root 'docs\gerar-uml.py') -Destination $docs

$archive = Join-Path $output ('ES-III-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.zip')
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($staging, $archive)
Write-Host "Pacote de fontes criado: $archive"
Write-Host 'O pacote nao representa validacao com Ollama. Confira as pendencias em docs/tasks.md.'
