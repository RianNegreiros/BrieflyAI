const STORAGE_KEYS = {
  API_KEY: "geminiApiKey",
  NOTES: "notes",
};

const API_ENDPOINT = "http://localhost:8080/api/process";

const ELEMENTS = {
  apiKey:        () => document.getElementById("apiKey"),
  saveKeyBtn:    () => document.getElementById("saveKeyBtn"),
  summarizeBtn:  () => document.getElementById("summarizeBtn"),
  suggestBtn:    () => document.getElementById("suggestBtn"),
  results:       () => document.getElementById("results"),
  saveNoteBtn:   () => document.getElementById("saveNoteBtn"),
  myNotes:       () => document.getElementById("myNotes"),
  settingsToggle:() => document.getElementById("settingsToggle"),
  settingsDrawer:() => document.getElementById("settingsDrawer"),
};

let lastProcessedContent = null;

document.addEventListener("DOMContentLoaded", initializeApp);

async function initializeApp() {
  await loadApiKey();
  setupEventListeners();
  displayNotes();
}

async function loadApiKey() {
  const { [STORAGE_KEYS.API_KEY]: apiKey } = await chrome.storage.local.get(STORAGE_KEYS.API_KEY);
  if (apiKey) {
    ELEMENTS.apiKey().value = apiKey;
  }
}

async function saveApiKey() {
  const apiKey = ELEMENTS.apiKey().value.trim();
  if (!apiKey) return;

  await chrome.storage.local.set({ [STORAGE_KEYS.API_KEY]: apiKey });

  const btn = ELEMENTS.saveKeyBtn();
  const prev = btn.textContent;
  btn.textContent = "Saved ✓";
  setTimeout(() => { btn.textContent = prev; }, 1500);
}

function toggleSettings() {
  const drawer  = ELEMENTS.settingsDrawer();
  const toggle  = ELEMENTS.settingsToggle();
  const isOpen  = drawer.classList.contains("is-open");

  drawer.classList.toggle("is-open", !isOpen);
  toggle.classList.toggle("is-active", !isOpen);
}

function setupEventListeners() {
  ELEMENTS.saveKeyBtn().addEventListener("click", saveApiKey);
  ELEMENTS.settingsToggle().addEventListener("click", toggleSettings);
  ELEMENTS.summarizeBtn().addEventListener("click", () => processText("summarize"));
  ELEMENTS.suggestBtn().addEventListener("click", () => processText("suggest"));
  document.getElementById("exportBtn").addEventListener("click", exportNotes);
  document.getElementById("importBtn").addEventListener("click", importNotes);

  chrome.storage.onChanged.addListener((changes, namespace) => {
    if (namespace === "local" && changes[STORAGE_KEYS.NOTES]) {
      displayNotes();
    }
  });
}

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

    const { [STORAGE_KEYS.API_KEY]: apiKey } = await chrome.storage.local.get(STORAGE_KEYS.API_KEY);
    if (!apiKey) {
      showMessage("Enter your Gemini API key first — click the ⚙ icon above.", "error");
      return;
    }

    const selectedText = await getSelectedText();
    if (!selectedText?.trim()) {
      showMessage("Select some text on the page first, then try again.", "warning");
      return;
    }

    showSpinner(operation === "summarize" ? "Summarizing…" : "Generating suggestions…");

    const response = await fetch(API_ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content: selectedText, operation, apiKey }),
    });

    if (!response.ok) {
      throw new Error(`Server returned ${response.status}`);
    }

    const result = await response.text();
    showResult(result, selectedText, operation);
  } catch (error) {
    showMessage(`Something went wrong: ${error.message}`, "error");
  } finally {
    toggleButtons(buttons, false);
  }
}

function toggleButtons(buttons, disabled) {
  buttons.forEach((btn) => (btn.disabled = disabled));
}

function showSpinner(message) {
  ELEMENTS.results().innerHTML = `
    <div class="spinner-wrap">
      <div class="spinner"></div>
      <span class="spinner-label">${message}</span>
    </div>`;
  ELEMENTS.saveNoteBtn().hidden = true;
}

function showMessage(content, type = "success") {
  const cls = { error: "msg--error", warning: "msg--warning", success: "msg--success" }[type] || "msg--success";
  ELEMENTS.results().innerHTML = `<div class="msg ${cls}">${escapeHtml(content)}</div>`;
  ELEMENTS.saveNoteBtn().hidden = true;
}

function showResult(content, originalText, operation) {
  const formatted = formatMarkdown(escapeHtml(content));
  ELEMENTS.results().innerHTML = `<div class="result-card">${formatted}</div>`;

  lastProcessedContent = { content: formatted, original: originalText, operation };

  const saveBtn = ELEMENTS.saveNoteBtn();
  saveBtn.hidden = false;
  saveBtn.onclick = () => saveNote();
}

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
    .replace(/\n\n/g, "</p><p>")
    .replace(/\n/g, " ");
}

function escapeHtml(text) {
  const m = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" };
  return text.replace(/[&<>"']/g, (c) => m[c]);
}

async function saveNote() {
  if (!lastProcessedContent) return;

  try {
    const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(STORAGE_KEYS.NOTES);

    notes.push({
      id: Date.now(),
      timestamp: new Date().toLocaleString(),
      original: lastProcessedContent.original,
      summary: lastProcessedContent.content,
      type: lastProcessedContent.operation,
    });

    await chrome.storage.local.set({ [STORAGE_KEYS.NOTES]: notes });
    showMessage("Note saved.", "success");
    ELEMENTS.saveNoteBtn().hidden = true;
    setTimeout(() => { ELEMENTS.results().innerHTML = ""; }, 1800);
  } catch (error) {
    showMessage("Failed to save note.", "error");
  }
}

async function displayNotes() {
  const container = ELEMENTS.myNotes();
  if (!container) return;

  const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(STORAGE_KEYS.NOTES);

  if (notes.length === 0) {
    container.innerHTML = "";
    return;
  }

  const sorted = [...notes].sort((a, b) => b.id - a.id);
  const fragment = document.createDocumentFragment();
  sorted.forEach((note) => fragment.appendChild(createNoteElement(note)));

  container.innerHTML = "";
  container.appendChild(fragment);

  container.querySelectorAll(".note-delete").forEach((btn) => {
    btn.addEventListener("click", (e) => deleteNote(parseInt(e.currentTarget.dataset.id)));
  });
}

function createNoteElement(note) {
  const label = note.type === "suggest" ? "Suggest" : "Summary";
  const div = document.createElement("div");
  div.className = "note";
  div.dataset.type = note.type;
  div.innerHTML = `
    <div class="note-top">
      <span class="note-badge">${label}</span>
      <span class="note-time">${note.timestamp}</span>
      <button class="note-delete" data-id="${note.id}" aria-label="Delete note">×</button>
    </div>
    <details>
      <summary>View ${label.toLowerCase()}</summary>
      <div class="note-content">${note.summary}</div>
    </details>`;
  return div;
}

async function deleteNote(noteId) {
  try {
    const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(STORAGE_KEYS.NOTES);
    await chrome.storage.local.set({ [STORAGE_KEYS.NOTES]: notes.filter((n) => n.id !== noteId) });
  } catch (error) {
    console.error("Error deleting note:", error);
  }
}

async function exportNotes() {
  try {
    const { [STORAGE_KEYS.NOTES]: notes = [] } = await chrome.storage.local.get(STORAGE_KEYS.NOTES);
    if (notes.length === 0) { showMessage("No notes to export.", "warning"); return; }

    const blob = new Blob([JSON.stringify(notes, null, 2)], { type: "application/json" });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement("a");
    a.href = url;
    a.download = `brieflyai-notes-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  } catch (error) {
    showMessage("Failed to export notes.", "error");
  }
}

function importNotes() {
  const input   = document.createElement("input");
  input.type    = "file";
  input.accept  = "application/json";
  input.onchange = async (e) => {
    try {
      const file = e.target.files[0];
      if (!file) return;

      const imported = JSON.parse(await file.text());
      if (!Array.isArray(imported)) throw new Error("Invalid format");

      const { [STORAGE_KEYS.NOTES]: existing = [] } = await chrome.storage.local.get(STORAGE_KEYS.NOTES);
      await chrome.storage.local.set({ [STORAGE_KEYS.NOTES]: [...existing, ...imported] });
      showMessage(`Imported ${imported.length} note${imported.length !== 1 ? "s" : ""}.`, "success");
    } catch (error) {
      showMessage("Failed to import — check file format.", "error");
    }
  };
  input.click();
}
