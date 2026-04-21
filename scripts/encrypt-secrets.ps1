param(
  [Parameter(Mandatory = $true)]
  [string]$Passphrase,
  [string]$SourcePath
)

$repo = Split-Path -Parent $PSScriptRoot
$src = if ($SourcePath) { $SourcePath } else { Join-Path $repo 'services/backend/src/main/resources/application.yml' }
$enc = Join-Path $repo 'secrets/encrypted/application.yml.enc'

if (-not (Test-Path $src)) {
  Write-Error "Source file not found: $src"
  exit 1
}

New-Item -ItemType Directory -Force -Path (Split-Path $enc) | Out-Null

$sha = [System.Security.Cryptography.SHA256]::Create()
$key = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Passphrase))
$plain = Get-Content $src -Raw
$secure = ConvertTo-SecureString $plain -AsPlainText -Force
$encrypted = ConvertFrom-SecureString -SecureString $secure -Key $key
Set-Content -Path $enc -Value $encrypted -NoNewline

Write-Host "Encrypted: $enc"
