document.addEventListener("DOMContentLoaded", () => {
  chrome.storage.local.get(["geminiApiKey"], (result) => {
    if (result.geminiApiKey) {
      document.getElementById("apiKey").value = result.geminiApiKey;
    }
  });

  document
    .getElementById("summarizeBtn")
    .addEventListener("click", summarizeText);
  document.getElementById("saveKeyBtn").addEventListener("click", saveApiKey);
});

async function saveApiKey() {
  const apiKey = document.getElementById("apiKey").value;
  chrome.storage.local.set({ geminiApiKey: apiKey }, () => {
    if (chrome.runtime.lastError) {
      alert("Error saving API key: " + chrome.runtime.lastError.message);
    } else {
      alert("API key saved successfully");
    }
  });
}

async function summarizeText() {
  try {
    const { geminiApiKey } = await chrome.storage.local.get(["geminiApiKey"]);

    if (!geminiApiKey) {
      showResult("Please enter your Gemini API key first");
      return;
    }

    const [tab] = await chrome.tabs.query({
      active: true,
      currentWindow: true,
    });
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      function: () => window.getSelection().toString(),
    });

    if (!result) {
      showResult("Select some text first");
      return;
    }

    showSpinner();

    const response = await fetch("http://localhost:8080/api/process", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        content: result,
        operation: "summarize",
        apiKey: geminiApiKey,
      }),
    });

    if (!response.ok) {
      throw new Error(`Failed to summarize text: ${response.status}`);
    }

    const text = await response.text();
    showResult(formatMarkdown(escapeHtml(text)));
  } catch (error) {
    showResult("Error summarizing text: " + escapeHtml(error.message));
  }
}

function showResult(content) {
  document.getElementById("results").innerHTML =
    `<div class='result-item'><div class='result-content'>${content}</div></div>`;
}

function showSpinner() {
  document.getElementById("results").innerHTML = '<div class="spinner"></div>';
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
    .replace(/\n/g, "<br>");
}

// Simple HTML escape utility
function escapeHtml(text) {
  const map = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
  };
  return text.replace(/[&<>"']/g, function (m) {
    return map[m];
  });
}
