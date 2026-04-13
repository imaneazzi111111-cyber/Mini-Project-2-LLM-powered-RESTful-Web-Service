const CONTEXT_MENU_ID = "translate-selection-darija";

chrome.runtime.onInstalled.addListener(async () => {
  chrome.contextMenus.create({
    id: CONTEXT_MENU_ID,
    title: "Translate in For The Love Of The Game",
    contexts: ["selection"]
  });

  await chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== CONTEXT_MENU_ID || !tab?.id) {
    return;
  }

  const selectedText = info.selectionText?.trim() || "";

  await chrome.storage.local.set({
    selectedText,
    sourceUrl: tab.url || "",
    lastAction: "context-menu"
  });

  await chrome.sidePanel.open({ tabId: tab.id });
});

chrome.action.onClicked.addListener(async (tab) => {
  if (!tab?.id) {
    return;
  }

  await chrome.storage.local.set({
    lastAction: "toolbar"
  });

  await chrome.sidePanel.open({ tabId: tab.id });
});
