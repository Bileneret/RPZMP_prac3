"""Mock REST-сервер системи освітлення з IoT (варіант 13).

Емулює зовнішню систему для Практичної роботи №3 (ПЗ9-12):
  GET    /lights        — список світильників            (потрібен Bearer-токен)
  POST   /lights        — додати світильник              (потрібен Bearer-токен)
  PUT    /lights/<id>   — оновити (перемикач живлення)   (потрібен Bearer-токен)
  DELETE /lights/<id>   — видалити світильник            (потрібен Bearer-токен)
  GET    /users         — облікові записи для mocked-автентифікації (відкритий)

Токен перевіряється як «справжній» захищений ресурс: заголовок
`Authorization: Bearer <header>.<payload>.<signature>`, payload містить `exp`.
Підпис не перевіряється (немає секрету — це mock), але термін дії — так.

Стан зберігається у JSON-файлі атомарно (temp + replace) під захистом lock,
тому переживає перезапуск і не псується при паралельних запитах.

Демонстрація помилок (ПЗ12): створити поруч файл `fault_mode.json`, наприклад
    {"GET_LIGHTS": 500}
і сервер повертатиме цей код на відповідну операцію, поки файл існує.
Дозволені ключі: GET_LIGHTS, POST_LIGHTS, PUT_LIGHTS, DELETE_LIGHTS.
Файл прибрати — штатна робота.

Запуск:  python tools/mock_server.py  (порт 8080, стан у tools/server_state.json)

Паролі акаунтів — відкритим текстом у демо-даних (так передбачено завданням).
"""

import argparse
import base64
import json
import os
import pathlib
import re
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

TOOLS_DIR = pathlib.Path(__file__).parent
STATE_FILE = TOOLS_DIR / "server_state.json"
FAULT_FILE = TOOLS_DIR / "fault_mode.json"

SEED = {
    "lights": [
        {"id": 1, "name": "Люстра основна", "zone": "Зал", "powerW": 72, "isPoweredOn": True},
        {"id": 2, "name": "Підсвітка шафи", "zone": "Зал", "powerW": 12, "isPoweredOn": True},
        {"id": 3, "name": "Точкові кухонні", "zone": "Кухня", "powerW": 48, "isPoweredOn": True},
        {"id": 4, "name": "Робоча зона кухні", "zone": "Кухня", "powerW": 36, "isPoweredOn": False},
        {"id": 5, "name": "Нічник", "zone": "Спальня", "powerW": 10, "isPoweredOn": True},
        {"id": 6, "name": "Люстра спальня", "zone": "Спальня", "powerW": 60, "isPoweredOn": False},
        {"id": 7, "name": "Датчик тамбур", "zone": "Коридор", "powerW": 18, "isPoweredOn": True},
        {"id": 8, "name": "Фасадний прожектор", "zone": "Фасад", "powerW": 90, "isPoweredOn": True},
    ],
    "users": [
        {"id": 1, "email": "admin@light.io", "password": "admin123", "name": "Олексій", "role": "ADMIN"},
        {"id": 2, "email": "user@light.io", "password": "user123", "name": "Користувач", "role": "USER"},
    ],
}

LOCK = threading.Lock()


def load_state() -> dict:
    if STATE_FILE.exists():
        return json.loads(STATE_FILE.read_text(encoding="utf-8"))
    return json.loads(json.dumps(SEED))  # глибока копія seed


def save_state(state: dict) -> None:
    """Атомарний запис: temp-файл + os.replace, щоб збій не пошкодив стан."""
    tmp = STATE_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding="utf-8")
    os.replace(tmp, STATE_FILE)


STATE = load_state()


def token_is_valid(header_value: str | None) -> bool:
    """Bearer-токен формату JWT з непротермінованим exp (підпис не перевіряємо)."""
    if not header_value or not header_value.startswith("Bearer "):
        return False
    parts = header_value[len("Bearer "):].strip().split(".")
    if len(parts) != 3:
        return False
    try:
        payload_b64 = parts[1]
        padded = payload_b64 + "=" * (-len(payload_b64) % 4)
        payload = json.loads(base64.urlsafe_b64decode(padded))
        return int(payload.get("exp", 0)) > time.time()
    except Exception:
        return False


def fault_code(operation: str) -> int | None:
    """Симуляція помилки з fault_mode.json (лише для навчальної демонстрації)."""
    if not FAULT_FILE.exists():
        return None
    try:
        data = json.loads(FAULT_FILE.read_text(encoding="utf-8"))
        code = data.get(operation)
        return int(code) if code else None
    except Exception:
        return None


class LightingHandler(BaseHTTPRequestHandler):
    """Обробник запитів: JSON in/out, роутинг за методом+шляхом."""

    def _send(self, code: int, payload) -> None:
        body = b"" if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self) -> dict:
        length = int(self.headers.get("Content-Length", 0))
        return json.loads(self.rfile.read(length) or b"{}")

    def _require_token(self) -> bool:
        """Захищений ресурс: без валідного Bearer-токена — 401."""
        if token_is_valid(self.headers.get("Authorization")):
            return True
        self._send(401, {"error": "unauthorized: Bearer token required"})
        return False

    def _send_fault(self, operation: str) -> bool:
        code = fault_code(operation)
        if code is None:
            return False
        self._send(code, {"error": f"simulated failure {code}", "operation": operation})
        return True

    def do_GET(self) -> None:
        if self.path == "/lights":
            if self._send_fault("GET_LIGHTS"):
                return
            if not self._require_token():
                return
            with LOCK:
                snapshot = json.loads(json.dumps(STATE["lights"]))
            self._send(200, snapshot)
        elif self.path == "/users":
            self._send(200, STATE["users"])
        else:
            self._send(404, {"error": f"unknown path {self.path}"})

    def do_POST(self) -> None:
        if self.path != "/lights":
            self._send(404, {"error": "unknown path"})
            return
        if self._send_fault("POST_LIGHTS"):
            return
        if not self._require_token():
            return
        item = self._read_body()
        with LOCK:
            new_id = max((l["id"] for l in STATE["lights"]), default=0) + 1
            record = {
                "id": new_id,
                "name": item.get("name", ""),
                "zone": item.get("zone", ""),
                "powerW": int(item.get("powerW", 0)),
                "isPoweredOn": bool(item.get("isPoweredOn", False)),
            }
            STATE["lights"].append(record)
            save_state(STATE)
        self._send(201, record)

    def do_PUT(self) -> None:
        match = re.fullmatch(r"/lights/(\d+)", self.path)
        if not match:
            self._send(404, {"error": "unknown path"})
            return
        if self._send_fault("PUT_LIGHTS"):
            return
        if not self._require_token():
            return
        light_id = int(match.group(1))
        with LOCK:
            for index, light in enumerate(STATE["lights"]):
                if light["id"] == light_id:
                    patch = self._read_body()
                    updated = {**light, **patch, "id": light_id}
                    STATE["lights"][index] = updated
                    save_state(STATE)
                    self._send(200, updated)
                    return
        self._send(404, {"error": f"light {light_id} not found"})

    def do_DELETE(self) -> None:
        match = re.fullmatch(r"/lights/(\d+)", self.path)
        if not match:
            self._send(404, {"error": "unknown path"})
            return
        if self._send_fault("DELETE_LIGHTS"):
            return
        if not self._require_token():
            return
        light_id = int(match.group(1))
        with LOCK:
            before = len(STATE["lights"])
            STATE["lights"] = [l for l in STATE["lights"] if l["id"] != light_id]
            save_state(STATE)
        if before == len(STATE["lights"]):
            self._send(404, {"error": f"light {light_id} not found"})
        else:
            self._send(204, None)

    def log_message(self, fmt: str, *args) -> None:
        sys.stdout.write(f"[{self.command}] {fmt % args}\n")
        sys.stdout.flush()


def main() -> None:
    parser = argparse.ArgumentParser(description="Mock REST server для IoT Lighting")
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()
    server = ThreadingHTTPServer(("0.0.0.0", args.port), LightingHandler)
    print(f"Mock-сервер освітлення: http://0.0.0.0:{args.port} (стан: {STATE_FILE.name})")
    print("Акаунти: admin@light.io/admin123 (ADMIN), user@light.io/user123 (USER)")
    print(f"Демонстрація помилок: створити {FAULT_FILE.name} (напр. {{\"GET_LIGHTS\": 500}})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Сервер зупинено")


if __name__ == "__main__":
    main()
