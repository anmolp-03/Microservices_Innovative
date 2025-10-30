UI (static single-page app)

This folder contains a minimal vanilla-JS SPA used to interact with the microservices in this workspace.

Files
- index.html - main SPA HTML
- app.js - client logic to fetch menu, place orders, compute bills and post reviews
- styles.css - styling for the SPA
- Dockerfile - production container image which serves the files using nginx

Local development (no container)
1. Open `ui/index.html` directly in your browser OR run a small static server such as Python:
   - PowerShell:
     python -m http.server 8082 --directory .; # then open http://localhost:8082

2. If you use the project proxy (recommended for CORS-free local testing), run the proxy and open the proxied port.

Docker (build & run)
1. Build the UI image from the project root:
   docker build -t restaurant-ui:local ./ui

2. Run the UI image, mapping host port 18081 to container port 80 (adjust ports as needed):
   docker run --rm -p 18081:80 restaurant-ui:local

Then open http://localhost:18081 in your browser.

Recommended: use the project's `docker-compose.yml` at the repo root to start the entire stack (DBs, services and UI proxy) in one command. The compose file in the repo may map the UI proxy to a different host port (e.g. 18081). If you prefer the static nginx UI above, change the compose to point to this image or serve the `ui/` directory from a lightweight file server.

Troubleshooting
- If you see CORS errors when the UI tries to call the backend: run the provided UI proxy (test/ui_proxy.py) or use the compose stack which includes a proxy.
- If ports conflict, change the host port when running the container (`-p HOST:80`).

