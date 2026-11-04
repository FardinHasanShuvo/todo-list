$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$sources = @(Get-ChildItem -Path "$root\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) {
    throw "No Java sources found."
}
New-Item -ItemType Directory -Force -Path "$root\out" | Out-Null
& javac --release 17 -encoding UTF-8 -d "$root\out" @sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
& java -cp "$root\out" todo.Main
exit $LASTEXITCODE
