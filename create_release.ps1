$ErrorActionPreference = "Stop"
$token = "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"
$repo = "masterace572-prog/VaultX"
$headers = @{ "Authorization" = "token $token"; "Accept" = "application/vnd.github.v3+json" }

# Create Release
$body = @{
    tag_name = "v1.2.1"
    name = "VaultX v1.2.1"
    body = "Release for VaultX v1.2.1 - Colorful UI Update"
} | ConvertTo-Json

Write-Host "Creating release..."
$releaseResponse = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases" -Method Post -Headers $headers -Body $body
$uploadUrl = $releaseResponse.upload_url -replace '\{.*\}', ''
$htmlUrl = $releaseResponse.html_url
Write-Host "Release created: $htmlUrl"

# Upload Asset
$assetPath = "vaultx-user\build\outputs\apk\debug\vaultx-user-debug.apk"
$assetName = "vaultx-user-v1.2.1.apk"
$uploadUri = "$uploadUrl?name=$assetName"

Write-Host "Uploading asset..."
$uploadHeaders = @{ 
    "Authorization" = "token $token"
    "Accept" = "application/vnd.github.v3+json"
    "Content-Type" = "application/vnd.android.package-archive"
}
# We use curl.exe for the actual upload since it's much more stable than Invoke-RestMethod for large binary files
curl.exe -X POST -H "Authorization: token $token" -H "Content-Type: application/vnd.android.package-archive" --data-binary "@$assetPath" "$uploadUri"

# Update JSON file
$jsonPath = "update_v1.2.0.json"
$jsonContent = Get-Content $jsonPath | ConvertFrom-Json
$jsonContent.versionCode = 4
$jsonContent.versionName = "1.2.1"
$jsonContent.downloadUrl = "https://github.com/$repo/releases/download/v1.2.1/$assetName"
$jsonContent | ConvertTo-Json -Depth 10 | Set-Content $jsonPath
Write-Host "Updated update_v1.2.0.json with URL: $($jsonContent.downloadUrl)"
