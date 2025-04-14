# Briefly AI – Chrome Extension

A Chrome extension that opens a sidebar to:

- Summarize the content of the current page using the Gemini AI API
- Let the user write and save notes for each page

---

## Features

- One-click “Summarize Page” using Gemini AI (via a Spring Boot backend)
- Local notes saved per URL
- Clean sidebar interface with summary + notes

---

## Tech Stack

- **Frontend**: Chrome Extension
- **Backend**: Spring Boot (Java)
- **AI**: Gemini AI API (for summarization)

---

## User Flow

1. User clicks the extension icon → sidebar opens.
2. Selects the text -> Clicks “Summarize” → content sent to backend → Gemini returns a summary.
3. If user wants to save as a note → clicks “Save Note” → store locally in browser.
