document.addEventListener('DOMContentLoaded', () => {
    const launcher = document.getElementById('bisChatLauncher');
    const panel = document.getElementById('bisChatPanel');
    const backdrop = document.getElementById('bisChatBackdrop');
    const closeButton = document.getElementById('bisChatClose');
    const clearButton = document.getElementById('bisChatClear');
    const form = document.getElementById('bisChatForm');
    const input = document.getElementById('bisChatInput');
    const sendButton = document.getElementById('bisChatSend');
    const messages = document.getElementById('bisChatMessages');
    const status = document.getElementById('bisChatStatus');
    const promptHistory = document.getElementById('bisChatPromptHistory');
    const filterContext = document.getElementById(
        'bisChatFilterContext'
    );

    if (!launcher || !panel || !form || !input) {
        return;
    }

    const clearFiltersButton = document.querySelector(
        '.filter-card .filter-action-row .filter-btn.clear'
    );

    if (clearFiltersButton) {
        clearFiltersButton.after(launcher);
    }

    const MAX_HISTORY_MESSAGES = 6;
    const MAX_HISTORY_MESSAGE_CHARACTERS = 2000;
    const MAX_HISTORY_TOTAL_CHARACTERS = 8000;
    const MAX_SAVED_PROMPTS = 4;
    const SAVED_PROMPTS_KEY = 'evtl.bisIsiAssistant.savedPrompts';
    const conversationHistory = [];
    let notificationTimer;

    const showDeleteNotification = message => {
        window.clearTimeout(notificationTimer);
        document.querySelector('.bis-chat-notification')?.remove();

        const notification = document.createElement('div');
        notification.className = 'bis-chat-notification';
        notification.setAttribute('role', 'status');
        notification.setAttribute('aria-live', 'polite');
        notification.innerHTML = '<span aria-hidden="true">&#10003;</span><p></p>';
        notification.querySelector('p').textContent = message;
        document.body.appendChild(notification);

        requestAnimationFrame(() => notification.classList.add('visible'));
        notificationTimer = window.setTimeout(() => {
            notification.classList.remove('visible');
            window.setTimeout(() => notification.remove(), 180);
        }, 2000);
    };

    const loadSavedPrompts = () => {
        try {
            const stored = JSON.parse(
                localStorage.getItem(SAVED_PROMPTS_KEY) || '[]'
            );

            if (!Array.isArray(stored)) {
                return [];
            }

            return stored
                .filter(prompt => typeof prompt === 'string')
                .map(prompt => prompt.trim().slice(
                    0,
                    MAX_HISTORY_MESSAGE_CHARACTERS
                ))
                .filter(Boolean)
                .slice(0, MAX_SAVED_PROMPTS);
        } catch (error) {
            return [];
        }
    };

    let savedPrompts = loadSavedPrompts();

    const persistSavedPrompts = () => {
        try {
            localStorage.setItem(
                SAVED_PROMPTS_KEY,
                JSON.stringify(savedPrompts)
            );
        } catch (error) {
            // The assistant remains usable when browser storage is blocked.
        }
    };

    const renderSavedPrompts = () => {
        if (!promptHistory) {
            return;
        }

        promptHistory.replaceChildren();

        if (!savedPrompts.length) {
            const empty = document.createElement('p');
            empty.className = 'bis-chat-history-empty';
            empty.textContent = 'Your last four prompts will appear here.';
            promptHistory.appendChild(empty);
            return;
        }

        savedPrompts.forEach((prompt, index) => {
            const row = document.createElement('div');
            row.className = 'bis-chat-history-item';

            const reuseButton = document.createElement('button');
            reuseButton.className = 'bis-chat-history-prompt';
            reuseButton.type = 'button';
            reuseButton.textContent = prompt;
            reuseButton.title = prompt;
            reuseButton.addEventListener('click', () => {
                input.value = prompt;
                input.focus();
            });

            const deleteButton = document.createElement('button');
            deleteButton.className = 'bis-chat-history-delete';
            deleteButton.type = 'button';
            deleteButton.setAttribute(
                'aria-label',
                `Delete saved prompt: ${prompt}`
            );
            deleteButton.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5"/></svg>';
            deleteButton.addEventListener('click', () => {
                savedPrompts.splice(index, 1);
                persistSavedPrompts();
                renderSavedPrompts();
                showDeleteNotification('Prompt deleted successfully.');
            });

            row.append(reuseButton, deleteButton);
            promptHistory.appendChild(row);
        });
    };

    const savePrompt = prompt => {
        savedPrompts.unshift(
            prompt.slice(0, MAX_HISTORY_MESSAGE_CHARACTERS)
        );
        savedPrompts = savedPrompts.slice(0, MAX_SAVED_PROMPTS);
        persistSavedPrompts();
        renderSavedPrompts();
    };

    const rememberMessage = (role, text) => {
        conversationHistory.push({
            role,
            content: text.slice(0, MAX_HISTORY_MESSAGE_CHARACTERS)
        });

        while (conversationHistory.length > MAX_HISTORY_MESSAGES) {
            conversationHistory.shift();
        }

        while (conversationHistory.reduce(
            (total, entry) => total + entry.content.length,
            0
        ) > MAX_HISTORY_TOTAL_CHARACTERS) {
            conversationHistory.shift();
        }
    };

    const getSelectedUserName = (parameterName) => {
        const select = document.querySelector(
            `.filter-card select[name="${parameterName}"]`
        );

        if (!select || !select.value) {
            return null;
        }

        return select.selectedOptions[0]?.textContent?.trim() || null;
    };

    const getFilterPayload = () => {
        const params = new URLSearchParams(location.search);
        const monthItems = Array.from(
            document.querySelectorAll('.monthly-item')
        );
        const activeMonthIndex = monthItems.findIndex(
            item => item.classList.contains('active')
        );
        const urlMonth = Number(params.get('breakdownMonth'));
        const selectedMonth = activeMonthIndex >= 0
            ? activeMonthIndex + 1
            : Number.isInteger(urlMonth) && urlMonth >= 1 && urlMonth <= 12
                ? urlMonth
                : new Date().getMonth() + 1;
        const analyticsProcessSelect = document.querySelector(
            '.monthly-process-select'
        );

        return {
            startDate: params.get('startDate') || null,
            endDate: params.get('endDate') || null,
            procedure: params.get('procedure') || null,
            engineer: params.get('engineer')
                ? Number(params.get('engineer'))
                : null,
            creator: params.get('creator')
                ? Number(params.get('creator'))
                : null,
            status: params.get('status') || null,
            processes: params.getAll('process'),
            analyticsYear: params.get('analyticsYear')
                ? Number(params.get('analyticsYear'))
                : new Date().getFullYear(),
            selectedMonth,
            analyticsProcess: analyticsProcessSelect?.value
                || params.get('analyticsProcess')
                || 'License Granted'
        };
    };

    const updateFilterContext = () => {
        const filters = getFilterPayload();
        const parts = [];

        const creatorName = getSelectedUserName('creator');
        const engineerName = getSelectedUserName('engineer');

        if (creatorName) {
            parts.push(`User: ${creatorName}`);
        }

        if (engineerName) {
            parts.push(`Assigned: ${engineerName}`);
        }

        if (filters.startDate || filters.endDate) {
            parts.push(
                `Dates: ${filters.startDate || 'Any'} to `
                + `${filters.endDate || 'Any'}`
            );
        }

        if (filters.procedure) {
            parts.push(`Procedure: ${filters.procedure}`);
        }

        if (filters.status) {
            parts.push(`Status: ${filters.status}`);
        }

        if (filters.processes.length) {
            parts.push(`Processes: ${filters.processes.join(', ')}`);
        }

        parts.push(`Year: ${filters.analyticsYear}`);
        parts.push(
            `Chart month: ${new Intl.DateTimeFormat('en', {
                month: 'long'
            }).format(new Date(2000, filters.selectedMonth - 1, 1))}`
        );
        parts.push(`Analytics process: ${filters.analyticsProcess}`);

        filterContext.textContent = parts.length
            ? parts.join(' · ')
            : 'All BIS-ISI operations';
    };

    const openChat = () => {
        panel.hidden = false;
        if (backdrop) {
            backdrop.hidden = false;
        }
        document.body.classList.add('bis-chat-open');
        launcher.setAttribute('aria-expanded', 'true');
        updateFilterContext();
        input.focus();
    };

    const closeChat = () => {
        panel.hidden = true;
        if (backdrop) {
            backdrop.hidden = true;
        }
        document.body.classList.remove('bis-chat-open');
        launcher.setAttribute('aria-expanded', 'false');
        launcher.focus();
    };

    const setStatus = (message, type = 'info') => {
        status.textContent = message || '';
        status.classList.toggle('error', type === 'error');
        status.hidden = !message;
    };

    const addMessage = (role, text) => {
        const row = document.createElement('div');
        row.className = `bis-chat-message ${role}`;

        if (role === 'assistant') {
            const avatar = document.createElement('div');
            avatar.className = 'bis-chat-avatar';
            avatar.textContent = 'AI';
            row.appendChild(avatar);
        }

        const bubble = document.createElement('div');
        bubble.className = 'bis-chat-bubble';
        bubble.textContent = text;

        row.appendChild(bubble);
        messages.appendChild(row);
        messages.scrollTop = messages.scrollHeight;
    };

    const clearConversation = () => {
        conversationHistory.length = 0;
        messages.replaceChildren();

        addMessage(
            'assistant',
            'Hi, I am your BIS-ISI Assistant. I can help with operation totals, processes, procedures, assignments, deadlines and company questions.'
        );

        setStatus('');
        input.value = '';
        input.focus();
        showDeleteNotification('Chat cleared successfully.');
    };

    const parseError = async (response) => {
        try {
            const body = await response.json();

            return body.detail
                || body.message
                || `Request failed with status ${response.status}.`;
        } catch (error) {
            return `Request failed with status ${response.status}.`;
        }
    };

    const sendQuestion = async () => {
        const message = input.value.trim();

        if (!message) {
            input.focus();
            return;
        }

        const csrf = document.querySelector('input[name="_csrf"]');

        if (!csrf) {
            setStatus(
                'The security token is unavailable. Refresh the page.',
                'error'
            );
            return;
        }

        const payload = {
            message,
            history: conversationHistory.map(entry => ({ ...entry })),
            ...getFilterPayload()
        };

        savePrompt(message);
        addMessage('user', message);

        input.value = '';
        input.disabled = true;
        sendButton.disabled = true;
        setStatus('Analyzing the filtered BIS-ISI data...');

        try {
            const response = await fetch(
                '/api/operation/bis-isi/assistant',
                {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        'X-CSRF-TOKEN': csrf.value
                    },
                    body: JSON.stringify(payload)
                }
            );

            if (!response.ok) {
                throw new Error(await parseError(response));
            }

            const result = await response.json();

            if (!result.answer) {
                throw new Error(
                    'The assistant returned an empty response.'
                );
            }

            addMessage('assistant', result.answer);
            rememberMessage('user', message);
            rememberMessage('assistant', result.answer);
            setStatus('');

        } catch (error) {
            const fallback =
                'Unable to contact the BIS-ISI assistant.';

            setStatus(error.message || fallback, 'error');

        } finally {
            input.disabled = false;
            sendButton.disabled = false;
            input.focus();
        }
    };

    launcher.addEventListener('click', () => {
        if (panel.hidden) {
            openChat();
        } else {
            closeChat();
        }
    });

    closeButton.addEventListener('click', closeChat);
    backdrop?.addEventListener('click', closeChat);
    clearButton.addEventListener('click', clearConversation);

    form.addEventListener('submit', event => {
        event.preventDefault();
        sendQuestion();
    });

    input.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && !panel.hidden) {
            closeChat();
        }
    });

    renderSavedPrompts();
    updateFilterContext();
});
