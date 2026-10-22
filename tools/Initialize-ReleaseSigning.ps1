$ErrorActionPreference = 'Stop'
# Keep the key and credentials outside Git, protected by the current Windows user's ACL.
$signingDir = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.fulllock/signing'
$propertiesPath = Join-Path $signingDir 'release.properties'
$keyPath = Join-Path $signingDir 'fulllock-release.p12'
if ((Test-Path -LiteralPath $propertiesPath) -or (Test-Path -LiteralPath $keyPath)) {
    throw 'Signing files already exist. They will not be replaced.'
}
New-Item -ItemType Directory -Force $signingDir | Out-Null
$acl = New-Object System.Security.AccessControl.DirectorySecurity
$acl.SetAccessRuleProtection($true, $false)
$sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
$acl.AddAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')))
Set-Acl -LiteralPath $signingDir -AclObject $acl
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$password = [BitConverter]::ToString($bytes).Replace('-', '')
$env:FULLLOCK_TEMP_STOREPASS = $password
try {
    & keytool -genkeypair -storetype PKCS12 -keystore $keyPath -storepass:env FULLLOCK_TEMP_STOREPASS -keypass:env FULLLOCK_TEMP_STOREPASS -alias fulllock -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 -dname 'CN=Full Lock, OU=App Signing' *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Unable to create signing key.' }
    $pathValue = $keyPath.Replace('\', '/')
    [IO.File]::WriteAllText($propertiesPath, "storeFile=$pathValue`nstorePassword=$password`nkeyAlias=fulllock`nkeyPassword=$password`n", [Text.Encoding]::ASCII)
    Write-Output "Created private signing files in $signingDir. Back up this directory securely; do not upload it."
} finally {
    Remove-Item Env:FULLLOCK_TEMP_STOREPASS -ErrorAction SilentlyContinue
    $password = $null
    [Array]::Clear($bytes, 0, $bytes.Length)
}
