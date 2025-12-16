# BrieflyAI

BrieflyAI is a Chrome Extension with a Spring Boot backend that summarizes selected text on any webpage using the Google Gemini, and lets you save personal notes locally per URL.

## Features

- Summarize selected text via the side panel
- Store personal research notes locally (per URL)

## Architecture

- Extension: Chrome Extension (Manifest V3), side panel UI
- Server: Spring Boot (Java 21)

## Repository Layout

- `extension/`: Chrome extension source
  - `manifest.json`
  - `src/`
    - `sidepanel.html`, `sidepanel.js`, `sidepanel.css`
    - `background.js`
- `server/`: Spring Boot service exposing `/api/process`
- `docs/`: Product design notes

## Prerequisites

- Java 21 and Maven
- Google Gemini API key
- Chrome (or Chromium-based browser) supporting side panel extensions

## Server Setup (Spring Boot)

1. Server reads `GEMINI_MODEL` from the environment; see `server/src/main/resources/application.properties`.
2. From `server/`, run the service:
   - `./mvnw spring-boot:run`
3. The API will be available at `http://localhost:8080/api/process`.

Request format:

```json
{
  "content": "<selected text>",
  "operation": "summarize",
  "apiKey": "<your-gemini-api-key-here>"
}
```

## Extension Setup (Development)

1. Start the server first (see above).
2. In Chrome, navigate to `chrome://extensions`.
3. Enable Developer Mode.
4. Click "Load unpacked" and select the `extension/` folder.
5. Pin the extension and open the side panel.

Usage:

- Select text on any page, open the side panel, and click "Summarize".
- Write notes in the text area and click "Save Notes". Notes are stored in `chrome.storage.local` per browser profile.

## Configuration

- The extension expects the server at `http://localhost:8080` (configured in `extension/manifest.json` and `extension/src/sidepanel.js`).

- Manifest file paths must match your directory structure. If your files live under `extension/src/`, update `extension/manifest.json` accordingly:
  - `side_panel.default_path`: `src/sidepanel.html`
  - `background.service_worker`: `src/background.js`

## Security & Privacy

- Selected text is sent from the extension to the local server, which forwards the prompt to the Gemini API.
- Notes are stored locally in the browser via `chrome.storage.local` and are not sent to the server.

## Testing

### Manual API Testing

To manually test the API endpoint, you can use `curl` or any HTTP client like Postman or Insomnia. Below are examples for testing the endpoint:

#### Using cURL

```bash
# Test summarization
curl -X POST http://localhost:8080/api/process \
  -H "Content-Type: application/json" \
  -d '{"content": "Your text to summarize here", "operation": "summarize", "apiKey": "<your-gemini-api-key-here>"}'
```

#### Using the `.http` File

You can use the provided `test-api.http` file in the root of the project to test the API. This file contains pre-configured HTTP requests that you can run directly in VS Code using the **REST Client** extension.

1. Open the `test-api.http` file in VS Code.
2. Install the **REST Client** extension if you haven't already.
3. Click the **Send Request** link above the request to test the endpoint.

Example content of `test-api.http`:

```http
POST http://localhost:8080/api/process
Content-Type: application/json

{
  "content": "Your text to summarize here",
  "operation": "summarize",
  "apiKey": "your-gemini-api-key-here"
}
```

#### Expected Responses

- **Success**: Returns a plain text summary of the provided content.
- **Error**: Returns an error message with details about the issue (e.g., invalid request format).
