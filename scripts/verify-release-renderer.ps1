param([string]$Apk = "$PSScriptRoot/../app/build/outputs/apk/release/app-release.apk")
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Apk))
$oldBridge = $false
$newRenderer = $false
$reviewNavigation = $false
try {
    foreach ($entry in $archive.Entries) {
        if ($entry.FullName -notmatch '^classes\d*\.dex$') { continue }
        $reader = [IO.StreamReader]::new($entry.Open(), [Text.Encoding]::ASCII)
        try {
            $dex = $reader.ReadToEnd()
            $oldBridge = $oldBridge -or $dex.Contains('onHeightCalculated')
            $newRenderer = $newRenderer -or $dex.Contains('resetForReuse')
            $reviewNavigation = $reviewNavigation -or $dex.Contains('QuizReviewNavigation')
        } finally { $reader.Dispose() }
    }
} finally { $archive.Dispose() }
if ($oldBridge -or !$newRenderer -or !$reviewNavigation) {
    throw "Stale/wrong APK: oldBridge=$oldBridge, newRenderer=$newRenderer, reviewNavigation=$reviewNavigation"
}
Write-Output 'PASS: packaged APK contains the new math renderer and read-only review navigation.'
Get-FileHash -LiteralPath $Apk -Algorithm SHA256
