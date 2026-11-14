# RainOps Frontend

Arabic, responsive incident management interface built with React, TypeScript, and Vite.

## Development

Requires Node.js 22.12+ (Node.js 24 recommended) and the RainOps API.

```sh
npm ci
npm run dev
```

The development server listens on http://127.0.0.1:5173. Requests under /api are proxied to http://127.0.0.1:8080. Set API_PROXY_TARGET in .env.local to use another backend.

## Verification

```sh
npm run lint
npm run build
npx playwright install chromium
npm test
```

Browser tests cover desktop and mobile flows with isolated API fixtures. The optional live test requires RAINOPS_LIVE=1 and a backend connected to a disposable database; never run it against production data.

## Deployment

`npm run build` produces dist/. Serve it using a static web server with SPA fallback to index.html and reverse-proxy /api to the backend. Vite's development/preview proxy is not a production server. PostgreSQL credentials must remain on the backend.

## Structure

- src/pages: routed dashboard, incident list, form, and details
- src/components: shared UI components
- src/lib: API client and display formatting
- src/hooks: cancellable data loading
- src/types.ts: API contracts
- tests: browser workflows and optional live integration test
