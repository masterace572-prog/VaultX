$ErrorActionPreference = "Stop"
$token = "YOUR_GITHUB_TOKEN_HERE"
$repo = "masterace572-prog/VaultX"
$headers = @{ "Authorization" = "token $token"; "Accept" = "application/vnd.github.v3+json" }

# Create Release
$body = @{
    tag_name = "v2.3.0"
    name = "VaultX v2.3.0"
    body = "Release for VaultX v2.3.0 - Premium Stripe-like UI Overhaul, Dynamic Support Email, Game Account Emails"
} | ConvertTo-Json

Write-Host "Creating release..."
$releaseResponse = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases" -Method Post -Headers $headers -Body $body
$uploadUrl = $releaseResponse.upload_url -replace '\{.*\}', ''
$htmlUrl = $releaseResponse.html_url
Write-Host "Release created: $htmlUrl"

# Upload User Asset
$assetPathUser = "vaultx-user\build\outputs\apk\release\vaultx-user-release.apk"
$assetNameUser = "VaultX-User-v2.3.0.apk"
$uploadUriUser = "$uploadUrl?name=$assetNameUser"

Write-Host "Uploading user asset..."
curl.exe -X POST -H "Authorization: token $token" -H "Content-Type: application/vnd.android.package-archive" --data-binary "@$assetPathUser" "$uploadUriUser"

# Upload Admin Asset
$assetPathAdmin = "vaultx-admin\build\outputs\apk\release\vaultx-admin-release.apk"
$assetNameAdmin = "VaultX-Admin-v2.3.0.apk"
$uploadUriAdmin = "$uploadUrl?name=$assetNameAdmin"

Write-Host "Uploading admin asset..."
curl.exe -X POST -H "Authorization: token $token" -H "Content-Type: application/vnd.android.package-archive" --data-binary "@$assetPathAdmin" "$uploadUriAdmin"

Write-Host "Done!"
