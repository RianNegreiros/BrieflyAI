document.addEventListener("DOMContentLoaded", () => {
  chrome.storage.local.get(['researchNotes'], (result) => {
    if (result.researchNotes) {
      document.getElementById('notes').value = result.researchNotes;
    }
  });

  document.getElementById('summarizeBtn').addEventListener('click', summarizeText);
  document.getElementById('saveNotesBtn').addEventListener('click', saveNotes);
})

async function summarizeText() {
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      function: () => window.getSelection().toString()
    });

    if (!result) {
      showResult("Select some text first");
      return;
    }

    const response = await fetch("http://localhost:8080/api/process", {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: result, operation: 'summarize' })
    });

    if (!response.ok) {
      throw new Error(`Failed to summarize text: ${response.status}`);
    }

    const text = await response.text();
    showResult(escapeHtml(text).replace(/\n/g, '<br>'));
  } catch (error) {
    showResult('Error summarizing text: ' + escapeHtml(error.message));
  }
}

async function saveNotes() {
  const notes = document.getElementById('notes').value;
  chrome.storage.local.set({ researchNotes: notes }, () => {
    if (chrome.runtime.lastError) {
      alert("Error saving notes: " + chrome.runtime.lastError.message);
    } else {
      alert("Notes saved successfully");
    }
  });
}

function showResult(content) {
  document.getElementById('results').innerHTML = 
    `<div class='result-item'><div class='result-content'>${content}</div></div>`;
}

// Simple HTML escape utility
function escapeHtml(text) {
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  };
  return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}
