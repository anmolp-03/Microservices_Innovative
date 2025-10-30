#!/usr/bin/env python3
"""
Simple static server + reverse-proxy for local UI testing.
Serves files from ../ui on port 8081 and proxies API calls:
  /menu   -> http://localhost:8080
  /orders -> http://localhost:8000
  /bills  -> http://localhost:3000
  /reviews-> http://localhost:4000

Run: python test/ui_proxy.py
Open: http://localhost:8081
"""
import http.server
import socketserver
import urllib.request
import urllib.error
import urllib.parse
import sys
import os
import io

import os

# Allow overriding port with environment variable UI_PORT or first CLI arg
PORT = int(os.environ.get('UI_PORT', sys.argv[1] if len(sys.argv) > 1 else 8082))
UI_DIR = os.path.join(os.path.dirname(__file__), '..', 'ui')
API_MAP = {
    '/menu': 'http://localhost:8080',
    '/orders': 'http://localhost:8000',
    '/bills': 'http://localhost:3000',
    '/reviews': 'http://localhost:4000',
}

class ProxyHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=UI_DIR, **kwargs)

    def is_api(self, path):
        for prefix in API_MAP:
            if path == prefix or path.startswith(prefix + '/'):
                return prefix
        return None

    def forward_request(self, upstream_base):
        # Build upstream URL
        upstream = upstream_base + self.path
        # Prepare data for POST/PUT
        data = None
        if self.command in ('POST', 'PUT', 'PATCH'):
            length = int(self.headers.get('Content-Length', 0))
            data = self.rfile.read(length) if length > 0 else None
        # Prepare headers (pass Content-Type and Authorization if present)
        headers = {}
        for h in ('Content-Type', 'Authorization'):
            v = self.headers.get(h)
            if v:
                headers[h] = v
        req = urllib.request.Request(upstream, data=data, headers=headers, method=self.command)
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                self.send_response(resp.getcode())
                for k, v in resp.getheaders():
                    # skip transfer-encoding and certain headers
                    if k.lower() in ('transfer-encoding', 'content-encoding', 'connection'):
                        continue
                    self.send_header(k, v)
                self.end_headers()
                body = resp.read()
                if body:
                    self.wfile.write(body)
        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            self.end_headers()
            try:
                self.wfile.write(e.read())
            except Exception:
                pass
        except Exception as e:
            self.send_response(502)
            self.end_headers()
            self.wfile.write(str(e).encode('utf-8'))

    def do_GET(self):
        api = self.is_api(self.path)
        if api:
            upstream = API_MAP[api]
            # Trim the leading api prefix if upstream expects different base; here we forward full path
            # e.g. /menu -> http://localhost:8080/menu
            self.forward_request(upstream)
        else:
            return super().do_GET()

    def do_POST(self):
        api = self.is_api(self.path)
        if api:
            upstream = API_MAP[api]
            self.forward_request(upstream)
        else:
            # Allow posting to static server (unlikely), but return 404
            self.send_response(404)
            self.end_headers()

    do_PUT = do_POST
    do_PATCH = do_POST
    do_DELETE = do_POST

if __name__ == '__main__':
    print(f"Serving UI from: {UI_DIR}")
    print(f"Proxying APIs: {API_MAP}")
    with socketserver.ThreadingTCPServer(("", PORT), ProxyHandler) as httpd:
        print(f"Serving at http://localhost:{PORT}")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print('\nShutting down')
            httpd.shutdown()
