#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Crea un nuevo Architecture Decision Record (ADR) para el proyecto 4GUARD WMS.

.DESCRIPTION
    Genera un nuevo archivo ADR con numeracion automatica basandose en los archivos
    existentes en docs/adr/, usando la plantilla docs/adr/template.md.

.PARAMETER Title
    Titulo de la decision (se convierte a kebab-case para el nombre del archivo).

.PARAMETER Status
    Estado inicial del ADR. Valores: Propuesto (default), Aprobado, Rechazado.

.EXAMPLE
    .\scripts\new-adr.ps1 -Title "Uso de Kafka para mensajeria asincrona"
    .\scripts\new-adr.ps1 -Title "Migracion a Java 21" -Status "Propuesto"

.NOTES
    Herramienta interna de 4GUARD WMS. Ver docs/adr/README.md para el proceso completo.
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Title,

    [Parameter(Mandatory = $false)]
    [ValidateSet("Propuesto", "Aprobado", "Rechazado")]
    [string]$Status = "Propuesto"
)

# ── Configuracion ──────────────────────────────────────────────────────────────
$ADR_DIR = Join-Path $PSScriptRoot "..\docs\adr"
$TEMPLATE = Join-Path $ADR_DIR "template.md"
$README   = Join-Path $ADR_DIR "README.md"

# ── Verificar que existe la carpeta y la plantilla ─────────────────────────────
if (-not (Test-Path $ADR_DIR)) {
    Write-Error "No existe el directorio docs/adr. Ejecuta desde la raiz del proyecto."
    exit 1
}

if (-not (Test-Path $TEMPLATE)) {
    Write-Error "No existe docs/adr/template.md. Asegurate de que la plantilla exista."
    exit 1
}

# ── Calcular el siguiente numero ───────────────────────────────────────────────
$existingAdrs = Get-ChildItem -Path $ADR_DIR -Filter "[0-9][0-9][0-9]-*.md" |
    Sort-Object Name

$lastNumber = 0
foreach ($file in $existingAdrs) {
    $num = [int]$file.Name.Substring(0, 3)
    if ($num -gt $lastNumber) { $lastNumber = $num }
}

$nextNumber  = $lastNumber + 1
$paddedNum   = $nextNumber.ToString("000")

# ── Convertir titulo a kebab-case ──────────────────────────────────────────────
$kebabTitle = $Title.ToLower() `
    -replace '[áàâä]', 'a' `
    -replace '[éèêë]', 'e' `
    -replace '[íìîï]', 'i' `
    -replace '[óòôö]', 'o' `
    -replace '[úùûü]', 'u' `
    -replace '[ñ]', 'n' `
    -replace '[^a-z0-9\s-]', '' `
    -replace '\s+', '-' `
    -replace '-{2,}', '-'

$fileName   = "$paddedNum-$kebabTitle.md"
$outputPath = Join-Path $ADR_DIR $fileName
$today      = Get-Date -Format "yyyy-MM-dd"

# ── Verificar que no exista ya ─────────────────────────────────────────────────
if (Test-Path $outputPath) {
    Write-Error "El archivo '$fileName' ya existe."
    exit 1
}

# ── Generar el ADR a partir de la plantilla ────────────────────────────────────
$content = Get-Content $TEMPLATE -Raw

# Reemplazar placeholders
$content = $content `
    -replace '\{NNN\}',    $paddedNum `
    -replace '\{Titulo de la decision\}', $Title `
    -replace 'YYYY-MM-DD', $today `
    -replace '`Propuesto` \| `Aprobado` \| `Rechazado` \| `Superseded por ADR-NNN`', "`$Status`"

Set-Content -Path $outputPath -Value $content -Encoding UTF8

# ── Actualizar el README ───────────────────────────────────────────────────────
if (Test-Path $README) {
    $readmeContent = Get-Content $README -Raw

    # Buscar la tabla del indice y agregar la nueva entrada
    $newRow = "| [$paddedNum]($fileName) | $Title | $Status |"

    # Insertar antes del cierre de la tabla o al final de la misma
    if ($readmeContent -match "\| \[0") {
        # Hay entradas existentes - agregar al final de la tabla
        $readmeContent = $readmeContent -replace '(\| \[\d{3}\].*\| \w+ \|)\r?\n\r?\n', "`$1`n$newRow`n`n"
        $readmeContent = $readmeContent + "`n$newRow"
    }

    # Si la tabla no tiene entradas aun, agregar al final
    Set-Content -Path $README -Value $readmeContent.TrimEnd() -Encoding UTF8
}

# ── Resultado ──────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "✅ ADR creado exitosamente:" -ForegroundColor Green
Write-Host "   Archivo : $outputPath" -ForegroundColor Cyan
Write-Host "   Titulo  : $Title" -ForegroundColor Cyan
Write-Host "   Estado  : $Status" -ForegroundColor Cyan
Write-Host "   Numero  : $paddedNum" -ForegroundColor Cyan
Write-Host ""
Write-Host "Proximos pasos:" -ForegroundColor Yellow
Write-Host "  1. Edita el ADR: notepad `"$outputPath`""
Write-Host "  2. Referencia en codigo: // ADR-$paddedNum"
Write-Host "  3. Incluye el ADR en tu Pull Request"
Write-Host ""
