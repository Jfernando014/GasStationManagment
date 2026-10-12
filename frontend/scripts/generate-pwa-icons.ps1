# Reuses the fuel-pump paths from the approved login and dashboard brand.
# Windows-only asset generation; no image libraries or npm dependencies required.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$iconDirectory = Join-Path $PSScriptRoot '../public/icons'

function Write-PwaIcon([int]$Size, [string]$Name, [bool]$Maskable) {
    $bitmap = [System.Drawing.Bitmap]::new($Size * 4, $Size * 4)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $background = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#FF6B00'))
    $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::White, 2.2)
    $tile = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $pump = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $icon = $null
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.ScaleTransform(($Size * 4 / 512), ($Size * 4 / 512))
        if ($Maskable) {
            $graphics.FillRectangle($background, 0, 0, 512, 512)
            # All essential strokes stay inside the central 80% safe circle.
            $graphics.TranslateTransform(112, 106)
            $graphics.ScaleTransform(12, 12)
        } else {
            $tile.AddArc(0, 0, 256, 256, 180, 90)
            $tile.AddArc(256, 0, 256, 256, 270, 90)
            $tile.AddArc(256, 256, 256, 256, 0, 90)
            $tile.AddArc(0, 256, 256, 256, 90, 90)
            $tile.CloseFigure()
            $graphics.FillPath($background, $tile)
            $graphics.TranslateTransform(88, 74)
            $graphics.ScaleTransform(14, 14)
        }
        # M3 22V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v17
        $pump.AddLine(3, 22, 3, 5)
        $pump.AddArc(3, 3, 4, 4, 180, 90)
        $pump.AddLine(5, 3, 15, 3)
        $pump.AddArc(13, 3, 4, 4, 270, 90)
        $pump.AddLine(17, 5, 17, 22)
        $pump.StartFigure()
        # M17 10h2a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2
        $pump.AddLine(17, 10, 19, 10)
        $pump.AddArc(17, 10, 4, 4, 270, 90)
        $pump.AddLine(21, 12, 21, 17)
        $pump.AddArc(17, 15, 4, 4, 0, 90)
        $pump.AddLine(19, 19, 17, 19)
        $graphics.DrawPath($pen, $pump)
        $graphics.DrawEllipse($pen, 8, 7, 4, 4)
        $icon = [System.Drawing.Bitmap]::new($Size, $Size)
        $resizer = [System.Drawing.Graphics]::FromImage($icon)
        try {
            $resizer.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $resizer.DrawImage($bitmap, 0, 0, $Size, $Size)
        } finally {
            $resizer.Dispose()
        }
        $icon.Save((Join-Path $iconDirectory $Name), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        if ($null -ne $icon) { $icon.Dispose() }
        $pump.Dispose()
        $tile.Dispose()
        $pen.Dispose()
        $background.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

Write-PwaIcon 192 'icon-192.png' $false
Write-PwaIcon 512 'icon-512.png' $false
Write-PwaIcon 192 'icon-maskable-192.png' $true
Write-PwaIcon 512 'icon-maskable-512.png' $true
Write-PwaIcon 180 'apple-touch-icon.png' $true
