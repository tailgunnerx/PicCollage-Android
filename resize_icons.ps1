Add-Type -AssemblyName System.Drawing

$srcImg = Join-Path $PSScriptRoot "app_icon.png"
$resDir = Join-Path $PSScriptRoot "app\src\main\res"

$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$bmp = [System.Drawing.Image]::FromFile($srcImg)

foreach ($key in $sizes.Keys) {
    $size = $sizes[$key]
    $destDir = Join-Path $resDir $key
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir | Out-Null }

    $newBmp = New-Object System.Drawing.Bitmap $size, $size
    $graphics = [System.Drawing.Graphics]::FromImage($newBmp)
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($bmp, 0, 0, $size, $size)
    $graphics.Dispose()

    $outPath1 = Join-Path $destDir "ic_launcher.png"
    $outPath2 = Join-Path $destDir "ic_launcher_round.png"

    $newBmp.Save($outPath1, [System.Drawing.Imaging.ImageFormat]::Png)
    $newBmp.Save($outPath2, [System.Drawing.Imaging.ImageFormat]::Png)

    $newBmp.Dispose()
}

$bmp.Dispose()

$anydpiDir = Join-Path $resDir "mipmap-anydpi-v26"
if (Test-Path $anydpiDir) {
    Remove-Item -Recurse -Force $anydpiDir
}
