# BrieflyAI – Product Design

BrieflyAI is a Chrome Extension with a Spring Boot backend that summarizes user-selected text via the Gemini API and lets users keep local notes per URL in a side panel.

## Core Value

- Provide fast, readable summaries of selected content without leaving the page

## Features

- Summarize selected text using the backend `POST /api/process`
- Local note-taking, stored in `chrome.storage.local`, scoped per browser profile
- Side panel UI with result area and notes input

## Tech Stack

- Frontend: Chrome Extension (Manifest V3), side panel, background service worker
- Backend: Spring Boot (Java 17), WebFlux HTTP client
- AI: Google Gemini `generateContent` endpoint

## User Flow

1. User selects text on a webpage.
2. User opens the extension side panel and clicks “Summarize”.
3. Extension sends `{ content, operation: "summarize" }` to the backend.
4. Backend calls Gemini and returns plain text.
5. User reads the summary and optionally writes notes, then saves them locally.

## Non-Goals

- Multi-user accounts or cloud sync for notes
- Storing notes on the backend

## Security & Privacy

- Selected text is sent to the backend and then to Gemini for processing.
- Notes remain local in the browser and are not transmitted to the backend.
