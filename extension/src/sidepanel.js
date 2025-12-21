// Constants
const STORAGE_KEYS = {
  API_KEY: "geminiApiKey",
  NOTES: "notes",
};

const API_ENDPOINT = "http://localhost:8080/api/process";

const ELEMENTS = {
  apiKey: () => document.getElementById("apiKey"),
  saveKeyBtn: () => document.getElementById("saveKeyBtn"),
  summarizeBtn: () => document.getElementById("summarizeBtn"),
  suggestBtn: () => document.getElementById("suggestBtn"),
  results: () => document.getElementById("results"),
  saveNoteBtn: () => document.getElementById("saveNoteBtn"),
  myNotes: () => document.getElementById("myNotes"),
};

// State
let lastProcessedContent = null;

// Initialize
document.addEventListener("DOMContentLoaded", initializeApp);

async function initializeApp() {
  await loadApiKey();
  setupEventListeners();
  displayNotes();
}

// API Key Management
async function loadApiKey() {
  const { [STORAGE_KEYS.API_KEY]: apiKey } = await chrome.storage.local.get(
    STORAGE_KEYS.API_KEY,
  );
  if (apiKey) {
    ELEMENTS.apiKey().value = apiKey;
  }
}

async function saveApiKey() {
  const apiKey = ELEMENTS.apiKey().value.trim();
  if (!apiKey) return;

  await chrome.storage.local.set({ [STORAGE_KEYS.API_KEY]: apiKey });
  console.log("API key saved successfully");
}

// Event Listeners
function setupEventListeners() {
  ELEMENTS.saveKeyBtn().addEventListener("click", saveApiKey);
  ELEMENTS.summarizeBtn().addEventListener("click", () =>
    processText("summarize"),
  );
  ELEMENTS.suggestBtn().addEventListener("click", () => processText("suggest"));

  chrome.storage.onChanged.addListener((changes, namespace) => {
    if (namespace === "local" && changes[STORAGE_KEYS.NOTES]) {
      displayNotes();
    }
  });
}

// Text Processing
async function getSelectedText() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  const [{ result }] = await chrome.scripting.executeScript({
    target: { tabId: tab.id },
    function: () => window.getSelection().toString(),
  });
  return result;
}

async function processText(operation) {
  const buttons = [ELEMENTS.summarizeBtn(), ELEMENTS.suggestBtn()];

  try {
    toggleButtons(buttons, true);

    const { [STORAGE_KEYS.API_KEY]: apiKey } = await chrome.storage.local.get(
      STORAGE_KEYS.API_KEY,
    );
    if (!apiKey) {
      showMessage("Please enter your Gemini API key first", "error");
      return;
    }

    const selectedText = await getSelectedText();
    if (!selectedText?.trim()) {
      showMessage("Select some text first", "warning");
      return;
    }

    showSpinner(
      `${operation === "summarize" ? "Summarizing" : "Generating suggestions"}...`,
    );

    const response = await fetch(API_ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: selectedText, operation, apiKey }),
    });

    if (!response.ok) {
      throw new Error(`Failed to ${operation} text: ${response.status}`);
    }

    const result = await response.text();
    showResult(result, selectedText, operation);
  } catch (error) {
    showMessage(`Error ${operation}ing text: ${error.message}`, "error");
  } finally {
    toggleButtons(buttons, false);
  }
}

// UI Helpers
function toggleButtons(buttons, disabled) {
  buttons.forEach((btn) => (btn.disabled = disabled));
}

function showSpinner(message) {
  ELEMENTS.results().innerHTML = `
    <div class="spinner-container">
      <div class="spinner"></div>
      <p class="spinner-text">${message}</p>
    </div>`;
  ELEMENTS.saveNoteBtn().hidden = true;
}

function showMessage(content, type = "success") {
  const messageClass =
    {
      error: "error-message",
      warning: "warning-message",
      success: "success-message",
    }[type] || "result-item";

  ELEMENTS.results().innerHTML = `
    <div class="${messageClass}">
      <div class="result-content">${escapeHtml(content)}</div>
    </div>`;
  ELEMENTS.saveNoteBtn().hidden = true;
}

function showResult(content, originalText, operation) {
  const formatted = formatMarkdown(escapeHtml(content));
  ELEMENTS.results().innerHTML = `
    <div class="result-item">
      <div class="result-content">${formatted}</div>
    </div>`;

  lastProcessedContent = {
    content: formatted,
    original: originalText,
    operation,
  };

  const saveBtn = ELEMENTS.saveNoteBtn();
  saveBtn.hidden = false;
  saveBtn.onclick = () => saveNote();
}

// Formatting
function formatMarkdown(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.*?)\*/g, "<em>$1</em>")
    .replace(/`(.*?)`/g, "<code>$1</code>")
    .replace(/^### (.*$)/gm, "<h3>$1</h3>")
    .replace(/^## (.*$)/gm, "<h2>$1</h2>")
    .replace(/^# (.*$)/gm, "<h1>$1</h1>")
    .replace(/^- (.*$)/gm, "<li>$1</li>")
    .replace(/(<li>.*<\/li>)/s, "<ul>$1</ul>")
    .replace(/\n\n/g, "</p><p>") // Double newlines = paragraphs
    .replace(/\n/g, " "); // Single newlines = spaces
}

function escapeHtml(text) {
  const escapeMap = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
  };
  return text.replace(/[&<>"']/g, (m) => escapeMap[m]);
}

// Notes Management
async function saveNote() {
  if (!lastProcessedContent) return;

  try {
    const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(
      STORAGE_KEYS.NOTES,
    );

    const newNote = {
      id: Date.now(),
      timestamp: new Date().toLocaleString(),
      original: lastProcessedContent.original,
      summary: lastProcessedContent.content,
      type: lastProcessedContent.operation,
    };

    notes.push(newNote);
    await chrome.storage.local.set({ [STORAGE_KEYS.NOTES]: notes });

    showMessage("Note saved successfully!", "success");
    ELEMENTS.saveNoteBtn().hidden = true;

    setTimeout(() => (ELEMENTS.results().innerHTML = ""), 2000);
  } catch (error) {
    console.error("Error saving note:", error);
    showMessage("Failed to save note", "error");
  }
}

async function displayNotes() {
  const notesContainer = ELEMENTS.myNotes();
  if (!notesContainer) return;

  const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(
    STORAGE_KEYS.NOTES,
  );

  if (notes.length === 0) {
    notesContainer.innerHTML = "<p>No notes saved yet.</p>";
    return;
  }

  const sortedNotes = [...notes].sort((a, b) => b.id - a.id);
  const fragment = document.createDocumentFragment();

  sortedNotes.forEach((note) => {
    const noteElement = createNoteElement(note);
    fragment.appendChild(noteElement);
  });

  notesContainer.innerHTML = "";
  notesContainer.appendChild(fragment);

  // Attach delete handlers
  notesContainer.querySelectorAll(".note-delete").forEach((btn) => {
    btn.addEventListener("click", (e) =>
      deleteNote(parseInt(e.target.dataset.id)),
    );
  });
}

function createNoteElement(note) {
  const typeLabel = note.type === "suggest" ? "Suggestion" : "Summary";

  const div = document.createElement("div");
  div.className = "note";
  div.dataset.type = note.type;
  div.innerHTML = `
    <div class="note-header">
      <div class="note-meta">
        <span class="note-badge">${typeLabel}</span>
        <span class="note-time">${note.timestamp}</span>
      </div>
      <button class="note-delete" data-id="${note.id}">Delete</button>
    </div>
    <details>
      <summary>${typeLabel}</summary>
      <div class="note-content">${note.summary}</div>
    </details>`;
  return div;
}

async function deleteNote(noteId) {
  try {
    const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(
      STORAGE_KEYS.NOTES,
    );
    const filteredNotes = notes.filter((note) => note.id !== noteId);
    await chrome.storage.local.set({ [STORAGE_KEYS.NOTES]: filteredNotes });
  } catch (error) {
    console.error("Error deleting note:", error);
  }
}
