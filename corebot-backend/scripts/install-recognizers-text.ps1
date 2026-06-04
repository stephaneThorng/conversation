$ErrorActionPreference = "Stop"

$installDir = Join-Path $env:TEMP "Recognizers-Text"

if (-not (Test-Path $installDir)) {
    git clone --depth 1 https://github.com/microsoft/Recognizers-Text.git $installDir
} else {
    git -C $installDir pull --ff-only
}

Push-Location (Join-Path $installDir "Java")
try {
    mvn -q -DskipTests install
} finally {
    Pop-Location
}

Write-Output "Recognizers-Text Java artifacts installed to mavenLocal()."
