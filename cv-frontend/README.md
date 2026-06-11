# CV Frontend

React + TypeScript frontend for the educational CV Manager starter application.

## Run Locally

Start the backend first, then run:

```bash
npm install
npm run dev
```

The frontend runs at:

```text
http://localhost:5173
```

The API base URL defaults to:

```text
http://localhost:8080
```

Override it with:

```text
VITE_API_BASE_URL=http://localhost:8080
```

The GitHub Pages production build reads `VITE_API_BASE_URL` from
`.env.production`.
