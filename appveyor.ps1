$ErrorActionPreference = "Stop"

function FetchAndUnzip
{
	param ([string]$Url, [string]$Out)

	$tmp = [System.IO.Path]::GetTempFileName()
	[System.Reflection.Assembly]::LoadWithPartialName('System.Net.Http') | Out-Null
	$client = (New-Object System.Net.Http.HttpClient)
	try
	{
		if (-not([string]::IsNullOrEmpty($env:GITHUB_TOKEN)))
		{
			$credentials = [string]::Format([System.Globalization.CultureInfo]::InvariantCulture, "{0}:", $env:GITHUB_TOKEN);
			$credentials = [Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($credentials));
			$client.DefaultRequestHeaders.Authorization = (New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Basic", $credentials));
		}
		$contents = $client.GetByteArrayAsync($url).Result;
		[System.IO.File]::WriteAllBytes($tmp, $contents);
	}
	finally
	{
		$client.Dispose()
	}

	if (-not(Test-Path $Out))
	{
		mkdir $Out | Out-Null
	}
	[System.Reflection.Assembly]::LoadWithPartialName('System.IO.Compression.FileSystem') | Out-Null
	[System.IO.Compression.ZipFile]::ExtractToDirectory($tmp, $Out)
}

function InstallAppveyorTools
{
	$travisUtilsVersion = "16"
	$localPath = "$env:USERPROFILE\.local"
	$travisUtilsPath = "$localPath\travis-utils-$travisUtilsVersion"
	if (Test-Path $travisUtilsPath)
	{
		echo "Reusing the Travis Utils version $travisUtilsVersion already downloaded under $travisUtilsPath"
	}
	else
	{
		$url = "https://github.com/SonarSource/travis-utils/archive/v$travisUtilsVersion.zip"
		echo "Downloading Travis Utils version $travisUtilsVersion from $url into $localPath"
		FetchAndUnzip $url $localPath
	}

	$mavenLocalRepository = "$env:USERPROFILE\.m2\repository"
	if (-not(Test-Path $mavenLocalRepository))
	{
		mkdir $mavenLocalRepository | Out-Null
	}
	echo "Installating Travis Utils closed source Maven projects into $mavenLocalRepository"
	Copy-Item "$travisUtilsPath\m2repo\*" $mavenLocalRepository -Force -Recurse

	$env:ORCHESTRATOR_CONFIG_URL = ""
	$env:TRAVIS = "ORCH-332"
}

function Build
{
	param ([string]$Project, [string]$Sha1)

	$msg = "Fetch [" + $Project + ":" + $Sha1 + "]"
	echo $msg

	$url = "https://github.com/$Project/archive/$Sha1.zip"
	$tmp = "c:\snapshot"
	if (Test-Path $tmp)
	{
		Cmd /C "rmdir /S /Q $tmp"
	}

	FetchAndUnzip $url $tmp

	$msg = "Build [" + $Project + ":" + $Sha1 + "]"
	echo $msg

	pushd $tmp\*
	try
	{
		mvn install "--batch-mode" "-DskipTests" "-Pdev"
		CheckLastExitCode
	}
	finally
	{
		popd
	}
}

function BuildSnapshot
{
	param ([string]$Project)

	echo "Fetch and build latest green snapshot of [$Project]"

	$lastGreenSha1 = (new-object Net.WebClient).DownloadString("http://sonarsource-979.appspot.com/$Project/latestGreen")

	Build $Project $lastGreenSha1
}

function CheckLastExitCode
{
    param ([int[]]$SuccessCodes = @(0))

    if ($SuccessCodes -notcontains $LastExitCode)
	{
        $msg = @"
EXE RETURNED EXIT CODE $LastExitCode
CALLSTACK:$(Get-PSCallStack | Out-String)
"@
        throw $msg
    }
}

switch ($env:RUN)
{
	"ci"
	{
		InstallAppveyorTools

		mvn verify "--batch-mode" "-B" "-e" "-V"
		CheckLastExitCode
	}

	{($_ -eq "its") -or ($_ -eq "ruling")}
	{
		InstallAppveyorTools

		mvn install "--batch-mode" "-Dsource.skip=true" "-Denforcer.skip=true" "-Danimal.sniffer.skip=true" "-Dmaven.test.skip=true"
		CheckLastExitCode

		if ($env:SQ_VERSION -eq "DEV")
		{
			BuildSnapshot "SonarSource/sonarqube"
		}

		if ($env:RUN -eq "ruling")
		{
			BuildSnapshot "SonarSource/sonar-lits"
			$currentDir=$pwd.Path
			$env:SONAR_IT_SOURCES="${currentDir}/its/sources"
			pushd its/ruling
		}
		else
		{
			pushd its/plugin
		}
		try
		{
			mvn install "--batch-mode" "-DjavaVersion=DEV" "-Dsonar.runtimeVersion=$env:SQ_VERSION" "-Dmaven.test.redirectTestOutputToFile=false" "-Dsonar.jdbc.dialect=embedded" "-Dorchestrator.updateCenterUrl=http://update.sonarsource.org/update-center-dev.properties" "-Dmaven.localRepository=$env:USERPROFILE\.m2\repository"
			CheckLastExitCode
		}
		finally
		{
			popd
		}
	}

	default
	{
		throw "RUN should be set to either ""ci"", ""its"" or ""ruling"", not: ""$env:RUN"""
	}
}
