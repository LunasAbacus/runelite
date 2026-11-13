$exe = 'D:\Projects\RuneHandy\UWU3.ahk'
$processName = 'AutoHotkey64.exe'

if (-not (Get-Process -Name $processName -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $exe -WorkingDirectory (Split-Path -Parent $exe) -ErrorAction Stop
}