param(
  [Parameter(Mandatory = $true)]
  [string]$Passphrase
)

$repo = Split-Path -Parent $PSScriptRoot
$enc = Join-Path $repo 'secrets/encrypted/application.yml.enc'
$out = Join-Path $repo 'services/backend/src/main/resources/application.yml'

if (-not (Test-Path $enc)) {
  Write-Error "Encrypted file not found: $enc"
  exit 1
}

$sha = [System.Security.Cryptography.SHA256]::Create()
$key = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($Passphrase))
$encrypted = Get-Content $enc -Raw
$secure = ConvertTo-SecureString -String $encrypted -Key $key
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
  $plain = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
} finally {
  [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

Set-Content -Path $out -Value $plain -NoNewline
Write-Host "Decrypted: $out"
