@echo off
rem Запуск mock-сервера освітлення для Практичної роботи №3.
rem Зупинка: закрити вікно (Ctrl+C теж працює).
rem Стан зберігається у tools\server_state.json — переживає перезапуск.
title IoT Lighting mock server (port 8080)
python "%~dp0tools\mock_server.py" --port 8080
pause
