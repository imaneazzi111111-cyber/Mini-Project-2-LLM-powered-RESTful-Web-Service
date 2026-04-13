# Mini project 2

Course project clone for an LLM-powered RESTful translator service, paired with a redesigned web app and Chrome extension that share the same warm, cinematic conversation UI.

## What is included

- `translator-service/` for the Jakarta REST backend
- `web-app/` for the browser client
- `chrome-extension/` for the Manifest V3 side panel
- placeholder folders for PHP, Python, and mobile clients
- docs and shared workspace files from the original project scaffold

## Current experience

This clone keeps the English-to-Darija translation backend, but rebrands the web app and extension around a centered welcome screen and minimal dark composer inspired by the provided reference.

## Quick start

### Translator service

```bash
cd translator-service
export GEMINI_API_KEY=your_gemini_key_here
mvn exec:java
```

Default API endpoint:

- `POST http://127.0.0.1:8081/api/translator/translate`
- Basic auth demo credentials: `student / translator`

### Web app

```bash
cd web-app
python3 -m http.server 4173
```

Then open `http://127.0.0.1:4173`.

### Chrome extension

Load `chrome-extension/` as an unpacked extension in Chrome, then use the side panel or the context-menu shortcut to send selected text into the translator flow.

## Repository structure

```text
translator-service/  Jakarta REST translator backend
web-app/             Warm dark browser client
chrome-extension/    Matching side-panel extension
php-client/          Placeholder client
python-client/       Placeholder client
mobile-client/       Placeholder client
docs/                Project notes
shared/              Shared workspace files
```
