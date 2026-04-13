const experience = document.getElementById("experience");
const promptInput = document.getElementById("prompt-input");
const chatLog = document.getElementById("chat-log");
const form = document.getElementById("translator-form");
const composerShell = document.getElementById("composer-shell");
const sendButton = document.getElementById("send-button");
const statusText = document.getElementById("status-text");

const API_BASE_URL = "http://127.0.0.1:8081/api";
const BASIC_USER = "student";
const BASIC_PASSWORD = "translator";
const SPARK_ICON = `
<svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M12 1.5v21M4.6 4.6l14.8 14.8M1.5 12h21M4.6 19.4 19.4 4.6M7.2 2.9l9.6 18.2M2.9 7.2l18.2 9.6M16.8 2.9 7.2 21.1M2.9 16.8l18.2-9.6"></path>
</svg>`;
const REFRESH_ICON = `
<svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M20 11a8 8 0 1 0 2.1 5.4"></path>
    <path d="M20 4v7h-7"></path>
</svg>`;
const COPY_ICON = `
<svg viewBox="0 0 24 24" aria-hidden="true">
    <rect x="9" y="9" width="11" height="11" rx="2"></rect>
    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
</svg>`;

function setStatus(message) {
    statusText.textContent = message;
}

function autoResizeTextarea() {
    promptInput.style.height = "0";
    promptInput.style.height = `${Math.min(promptInput.scrollHeight, 112)}px`;
}

function updateComposerState() {
    const hasValue = promptInput.value.trim().length > 0;
    composerShell.classList.toggle("has-value", hasValue);
    sendButton.disabled = !hasValue;
}

function activateConversation() {
    experience.classList.add("has-messages");
}

function createUserMessage(text) {
    const article = document.createElement("article");
    article.className = "message user-message fade-up";

    const body = document.createElement("div");
    body.className = "message-body";
    body.textContent = text;

    article.append(body);
    return article;
}

function createAssistantMessage(sourceText, initialText) {
    const article = document.createElement("article");
    article.className = "message assistant-message fade-up";

    const icon = document.createElement("div");
    icon.className = "message-icon";
    icon.innerHTML = SPARK_ICON;

    const content = document.createElement("div");
    content.className = "message-content";

    const body = document.createElement("div");
    body.className = "message-body";
    body.textContent = initialText;

    const actions = document.createElement("div");
    actions.className = "message-actions";

    const refreshButton = document.createElement("button");
    refreshButton.type = "button";
    refreshButton.className = "action-button";
    refreshButton.setAttribute("aria-label", "Translate again");
    refreshButton.innerHTML = REFRESH_ICON;

    const copyButton = document.createElement("button");
    copyButton.type = "button";
    copyButton.className = "action-button";
    copyButton.setAttribute("aria-label", "Copy answer");
    copyButton.innerHTML = COPY_ICON;

    refreshButton.addEventListener("click", async () => {
        refreshButton.disabled = true;
        copyButton.disabled = true;
        body.textContent = "Working on that...";
        setStatus("Sending request to the translator...");

        try {
            const payload = await translateText(sourceText);
            body.textContent = payload.translatedText;
            setStatus(`Model: ${payload.provider}`);
            await chrome.storage.local.set({ draftResponse: payload.translatedText });
        } catch (error) {
            body.textContent = error.message;
            setStatus("Backend error. Make sure the translator service is running.");
        } finally {
            refreshButton.disabled = false;
            copyButton.disabled = false;
        }
    });

    copyButton.addEventListener("click", async () => {
        try {
            await navigator.clipboard.writeText(body.textContent);
            setStatus("Copied answer.");
        } catch {
            setStatus("Could not copy the answer.");
        }
    });

    actions.append(refreshButton, copyButton);
    content.append(body, actions);
    article.append(icon, content);
    return article;
}

async function translateText(text) {
    const response = await fetch(`${API_BASE_URL}/translator/translate`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Basic ${btoa(`${BASIC_USER}:${BASIC_PASSWORD}`)}`
        },
        body: JSON.stringify({
            text,
            sourceLanguage: "English",
            targetLanguage: "Darija",
            style: "natural"
        })
    });

    const payload = await response.json().catch(() => null);

    if (!response.ok) {
        const message = payload?.message || payload?.error || `Request failed with status ${response.status}.`;
        throw new Error(message);
    }

    return payload;
}

function getSourceStatus(sourceUrl, hasSelection) {
    if (!sourceUrl) {
        return "";
    }

    try {
        const hostname = new URL(sourceUrl).hostname;
        return hasSelection ? `Selection loaded from ${hostname}.` : `Ready from ${hostname}.`;
    } catch {
        return hasSelection ? "Selection loaded." : "Ready.";
    }
}

async function loadSelectionState() {
    const { selectedText = "", sourceUrl = "", lastAction = "" } = await chrome.storage.local.get([
        "selectedText",
        "sourceUrl",
        "lastAction"
    ]);

    const currentSelection = selectedText.trim();
    if (currentSelection && lastAction === "context-menu") {
        promptInput.value = currentSelection;
        activateConversation();
    }

    autoResizeTextarea();
    updateComposerState();
    setStatus(getSourceStatus(sourceUrl, Boolean(currentSelection)));
    promptInput.focus();
}

promptInput.addEventListener("input", () => {
    autoResizeTextarea();
    updateComposerState();
});

promptInput.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" || event.shiftKey) {
        return;
    }

    event.preventDefault();
    form.requestSubmit();
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const text = promptInput.value.trim();
    if (!text) {
        promptInput.focus();
        return;
    }

    activateConversation();

    const userMessage = createUserMessage(text);
    const assistantMessage = createAssistantMessage(text, "Working on that...");
    chatLog.append(userMessage, assistantMessage);
    assistantMessage.scrollIntoView({ behavior: "smooth", block: "end" });

    setStatus("Sending request to the translator...");
    promptInput.value = "";
    autoResizeTextarea();
    updateComposerState();

    try {
        const payload = await translateText(text);
        assistantMessage.querySelector(".message-body").textContent = payload.translatedText;
        setStatus(`Model: ${payload.provider}`);
        await chrome.storage.local.set({ draftResponse: payload.translatedText });
    } catch (error) {
        assistantMessage.querySelector(".message-body").textContent = error.message;
        setStatus("Backend error. Make sure the translator service is running.");
    } finally {
        promptInput.focus();
    }
});

chrome.storage.onChanged.addListener((changes, areaName) => {
    if (areaName !== "local") {
        return;
    }

    if (changes.selectedText || changes.sourceUrl || changes.lastAction) {
        loadSelectionState();
    }
});

loadSelectionState();
