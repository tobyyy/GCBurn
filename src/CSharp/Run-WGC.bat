@echo off
pushd "%~dp0"

set COMPlus_TieredCompilation=1
rem set COMPlus_gcServer=1
set COMPlus_gcConcurrent=1
set COMPlus_GCLatencyLevel=0
rem set COMPlus_GCLatencyMode=1
set COMPlus_GCCpuGroup=1
set COMPlus_Thread_UseAllCpuGroups=1
set COMPlus_GCRetainVM=1

goto :skipLogging
set COMPlus_GCLogEnabled=1
set COMPlus_GCLogFile=C:\Temp\GC.log
set COMPlus_GCLogFileSize=1f0
set COMPlus_GCConfigLogEnabled=1
set COMPlus_GCConfigLogFile=C:\Temp\GCConfig.log
set COMPlus_GCMixLog=C:\Temp\GCMix.log
:skipLogging

dotnet run -p App\GCBurn.App.csproj -c Release -- %*
popd
