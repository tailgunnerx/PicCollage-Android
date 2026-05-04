Add-Type -AssemblyName System.Drawing

$srcImg = "C:\Users\CREED-gaming\.gemini\antigravity\playground\ruby-ride\PicCollage\app_icon.png"
$resDir = "C:\Users\CREED-gaming\.gemini\antigravity\playground\ruby-ride\PicCollage\app\src\main\res"

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

if (Test-Path "C:\Users\CREED-gaming\.gemini\antigravity\playground\ruby-ride\PicCollage\app\src\main\res\mipmap-anydpi-v26") {
    Remove-Item -Recurse -Force "C:\Users\CREED-gaming\.gemini\antigravity\playground\ruby-ride\PicCollage\app\src\main\res\mipmap-anydpi-v26"
}
