# AI Banking Operations Platform — Frontend

Author: **Sharath Kumar Suram**

React 18 + TypeScript + Vite console for the AI Banking Operations Platform.
Talks to a Spring Cloud Gateway at `http://localhost:8080` (JWT Bearer auth).

## Run locally

```bash
npm install
npm run dev        # http://localhost:5173 — Vite proxies /api and /ws to localhost:8080
npm run build      # tsc && vite build -> dist/
```

Demo login: `admin` / `password123` (see the hint card on the login page).

## Docker

```bash
docker build -t aibop-frontend .
docker run -p 5173:80 aibop-frontend   # nginx serves dist/ and proxies /api + /ws to gateway:8080
```

## Pages

| Route | Page |
|---|---|
| `/` | Executive Overview — KPIs, Recharts case/severity charts, recent audit activity |
| `/kyc` | KYC Center — customers, create, agent verify + reasoning, compliance report |
| `/fraud` | Fraud Center — HIGH/CRITICAL alerts, ingest + analyze transactions, risk gauge |
| `/loans` | Loan Center — applications, underwriting assessment (DTI, risk band, factors) |
| `/support` | Support Center — ask the support agent, tickets, escalations |
| `/orchestration` | Orchestration Center — agent fleet status, run decision cases |
| `/review` | Decision Review — human approve/reject queue with case detail |
| `/reasoning` | Reasoning & Audit — filterable audit trail with expandable reasoning |

Real-time notifications arrive over STOMP WebSocket (`/ws`, topic `/topic/notifications`)
and surface as toasts plus a bell badge/popover in the top bar.
