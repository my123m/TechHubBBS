# TechHub Frontend Dev Launcher
# 强制使用 Node 24，绕过系统 PATH 中的旧版 Node 18
$NODE24 = "C:\Users\29640\AppData\Local\Microsoft\WinGet\Packages\OpenJS.NodeJS.LTS_Microsoft.Winget.Source_8wekyb3d8bbwe\node-v24.16.0-win-x64\node.exe"
& $NODE24 node_modules/vite/bin/vite.js $args
