#!/usr/bin/env python3
"""고정 지연시간을 주는 Discord 웹훅 모의 서버.

부하테스트에서 실제 Discord 대신 사용해, 두 백엔드 버전을 동일한 외부 API
응답 지연 조건 아래 공정하게 비교하기 위한 용도.
"""
import os
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DELAY_SECONDS = int(os.environ.get("MOCK_DELAY_MS", "300")) / 1000
PORT = int(os.environ.get("MOCK_PORT", "9000"))


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        self.rfile.read(length)
        time.sleep(DELAY_SECONDS)
        self.send_response(204)
        self.end_headers()

    def log_message(self, format, *args):
        pass


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"mock discord webhook listening on :{PORT}, delay={DELAY_SECONDS * 1000:.0f}ms")
    server.serve_forever()
