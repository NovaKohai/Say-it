@echo off
:: Batch script to set Quad9 DNS with Administrator privileges
net session >nul 2>&1
if %errorLevel% neq 0 (
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo ===================================================
echo Configuring Quad9 DNS on active Ethernet connection...
echo ===================================================

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Set-DnsClientServerAddress -InterfaceAlias 'Ethernet' -ServerAddresses @('9.9.9.9', '149.112.112.112', '2620:fe::fe', '2620:fe::9'); " ^
  "Set-DnsClientDohServerAddress -ServerAddress '9.9.9.9' -AutoUpgrade `$true -AllowFallbackToUdp `$true -ErrorAction SilentlyContinue; " ^
  "Set-DnsClientDohServerAddress -ServerAddress '149.112.112.112' -AutoUpgrade `$true -AllowFallbackToUdp `$true -ErrorAction SilentlyContinue; " ^
  "Clear-DnsClientCache"

echo.
echo DNS Cache Flushed:
ipconfig /flushdns

echo.
echo ===================================================
echo Current DNS Settings:
echo ===================================================
powershell -NoProfile -Command "Get-DnsClientServerAddress -InterfaceAlias 'Ethernet' | Select-Object InterfaceAlias, AddressFamily, ServerAddresses | Format-Table -AutoSize"

echo Done! Press any key to exit.
pause >nul
