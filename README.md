## BrieflyAI

BrieflyAI is a Chrome Extension with a Spring Boot backend that summarizes selected text on any webpage using the Gemini API, and lets you save personal notes locally per URL.

### Features

- Summarize selected text via the side panel
- Store personal research notes locally (per URL)

### Architecture

- Extension: Chrome Extension (Manifest V3), side panel UI, background service worker
- Server: Spring Boot (Java 17), WebFlux client to call Gemini API
- AI: Google Gemini `generateContent` endpoint

### Repository Layout

- `apps/extension/`: Chrome extension source
  - `manifest.json`
  - `src/`
    - `sidepanel.html`, `sidepanel.js`, `sidepanel.css`
    - `background.js`
- `apps/server/`: Spring Boot service exposing `/api/process`
- `docs/`: Product design notes

### Prerequisites

- Java 17 and Maven
- Google Gemini API key
- Chrome (or Chromium-based browser) supporting side panel extensions

### Server Setup (Spring Boot)

1. Set your Gemini API key as an environment variable:
   - Linux/macOS: `export GEMINI_KEY=your_key_here`
   - Windows (PowerShell): `$Env:GEMINI_KEY="your_key_here"`
2. From `apps/server/`, run the service:
   - `./mvnw spring-boot:run`
3. The API will be available at `http://localhost:8080/api/process`.

Request format:

```json
{
  "content": "<selected text>",
  "operation": "summarize"
}
```

### Extension Setup (Development)

1. Start the server first (see above).
2. In Chrome, navigate to `chrome://extensions`.
3. Enable Developer Mode.
4. Click "Load unpacked" and select the `apps/extension/` folder.
5. Pin the extension and open the side panel.

Usage:

- Select text on any page, open the side panel, and click "Summarize".
- Write notes in the text area and click "Save Notes". Notes are stored in `chrome.storage.local` per browser profile.

### Configuration

- Server reads `GEMINI_KEY` from the environment; see `apps/server/src/main/resources/application.properties`.
- The extension expects the server at `http://localhost:8080` (configured in `apps/extension/manifest.json` and `apps/extension/src/sidepanel.js`).
- Manifest file paths must match your directory structure. If your files live under `apps/extension/src/`, update `apps/extension/manifest.json` accordingly:
  - `side_panel.default_path`: `src/sidepanel.html`
  - `background.service_worker`: `src/background.js`

### Security & Privacy

- Selected text is sent from the extension to the local server, which forwards the prompt to the Gemini API.
- Notes are stored locally in the browser via `chrome.storage.local` and are not sent to the server.

### API & Postman

- Endpoint: POST /api/process
- Request:
  - Headers: Content-Type: application/json
  - Body:
    ```json
    { "content": "<selected text>", "operation": "summarize" }
    ```
- Response: Plain text summary

Postman:
- Import `docs/BrieflyAI.postman_collection.json`
- (Optional) Import `docs/BrieflyAI.postman_environment.json`
- Select the “BrieflyAI Local” environment and send the request.

Quick curl:
```bash
curl -s -X POST http://localhost:8080/api/process \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello world","operation":"summarize"}'
```
