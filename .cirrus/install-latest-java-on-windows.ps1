$javaVersion    = "24.0.1+9"
$sha256_x64     = "2ef7f20c00577747d23999c7b29a0d35d73aeeb9d67aec0aa9825e59084447c3"
$sha256_aarch64 = "aarch64 platform is currently unavailable, see https://adoptium.net/en-GB/news/2025/04/eclipse-temurin-8u452-11027-17015-2107-2401-available/"

Write-Output "Installing Java ${javaVersion}"

$javaMajorVersion = ($javaVersion -split '\.')[0]
Write-Output "Major version: ${javaMajorVersion}"

$javaUriVersion = $javaVersion -replace '\+', '%2B'
$javaFileVersion = $javaVersion -replace '\+', '_'
$arch = (Get-WmiObject -Class Win32_Processor).AddressWidth
if ($arch -eq 64) {
  $arch = "x64"
  $sha256 = $sha256_x64
} else {
  $arch = "aarch64"
  $sha256 = $sha256_aarch64
}
$zipFileName = "OpenJDK${javaMajorVersion}U-jdk_${arch}_windows_hotspot_${javaFileVersion}.zip"
$binaryUrl = "https://github.com/adoptium/temurin${javaMajorVersion}-binaries/releases/download/jdk-${javaUriVersion}/${zipFileName}"
$javaDownloadDirectory = "${env:CIRRUS_WORKING_DIR}/.java_download_cache"
$zipPath = "${javaDownloadDirectory}\${zipFileName}"
$javaHomeParent = "${env:CIRRUS_WORKING_DIR}/.openjdk"
$javaHome = "${javaHomeParent}\jdk-${javaVersion}"

Write-Output "Prepare download directory: ${javaDownloadDirectory}"
if (-not (Test-Path "${javaDownloadDirectory}")) {
  New-Item -ItemType Directory -Path $javaDownloadDirectory -Force
}
$itemsToDelete = Get-ChildItem -Path $javaDownloadDirectory | Where-Object { $_.Name -ne $zipFileName }
foreach ($item in $itemsToDelete) {
  Write-Output "Remove: ${item}"
  Remove-Item -Path $item.FullName -Recurse -Force
}

Write-Output "Prepare installation directory: ${javaHomeParent}"
if (-not (Test-Path "${javaHomeParent}")) {
  New-Item -ItemType Directory -Path $javaHomeParent -Force
}
Write-Output "Remove other jdk in ${javaHomeParent}"
$itemsToDelete = Get-ChildItem -Path $javaHomeParent | Where-Object { $_.Name -ne "jdk-${javaVersion}" }
foreach ($item in $itemsToDelete) {
  Write-Output "Remove: $item"
  Remove-Item -Path $item.FullName -Recurse -Force
}

if (-not (Test-Path "${javaHome}\bin\java.exe")) {
  if (Test-Path $zipPath) {
    Write-Output "Zip '$zipPath' already exists."
  } else {
    Write-Output "Download from '$binaryUrl' into '$zipPath'"
    Invoke-WebRequest -Uri $binaryUrl -OutFile $zipPath -UseBasicParsing > $null

    # Verify the checksum
    Write-Output "Check the sha256 checksum of $zipPath"
    $actualChecksum = Get-FileHash -Path $zipPath -Algorithm SHA256 | Select-Object -ExpandProperty Hash
    if ($actualChecksum -ne $sha256) {
      Write-Error "Checksum verification failed. Expected: $expectedChecksum, Actual: $actualChecksum"
      exit 1
    } else {
      Write-Output "Checksum verification passed."
    }
  }

  # Extract the zip file
  Write-Output "Extract JDK archive"
  $global:ProgressPreference = "SilentlyContinue"
  Expand-Archive -Path $zipPath -DestinationPath $javaHomeParent -Force > $null

  # Check if java is present
  if (-not (Test-Path "${javaHome}\bin\java.exe")) {
    Write-Error "Fail to find ${javaHome}\bin\java.exe in the extracted directory"
    exit 1
  }
} else {
  Write-Output "Java already installed in ${javaHome}"
}

# Set JAVA_HOME
Write-Output "Set JAVA_HOME to $javaHome"
$env:JAVA_HOME = "${javaHome}"

# Set PATH
$javaBinPath = "${env:JAVA_HOME}\bin"

if ($env:Path -split ';' -contains $javaBinPath) {
  Write-Output "The path $javaBinPath is already in the Path environment variable."
} else {
  Write-Output "Adding $javaBinPath to the Path environment variable."
  $env:Path = "$javaBinPath;$env:Path"
}

# Print the version
Write-Output "java.exe --version"
& "${env:JAVA_HOME}\bin\java.exe" --version

Write-Output "-- Java ${javaVersion} Installed Successfully --"

exit 0
