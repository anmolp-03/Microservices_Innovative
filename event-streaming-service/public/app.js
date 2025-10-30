// Connect to WebSocket server
const socket = io();

// DOM Elements
const logContainer = document.getElementById('logContainer');
const clearLogsButton = document.getElementById('clearLogs');
const serviceFilters = document.querySelectorAll('.service-filter');

// Store filter states
const filters = {
    menu: true,
    order: true,
    bill: true,
    review: true
};

// Handle incoming log messages
socket.on('log_message', (log) => {
    if (filters[log.service]) {
        const logEntry = createLogEntry(log);
        logContainer.appendChild(logEntry);
        
        // Auto-scroll to bottom if already at bottom
        const isAtBottom = logContainer.scrollHeight - logContainer.scrollTop === logContainer.clientHeight;
        if (isAtBottom) {
            logContainer.scrollTop = logContainer.scrollHeight;
        }
    }
});

// Create log entry element
function createLogEntry(log) {
    const entry = document.createElement('div');
    entry.className = `log-entry ${log.service}`;

    const timestamp = document.createElement('span');
    timestamp.className = 'log-timestamp';
    timestamp.textContent = new Date(log.timestamp).toLocaleString();

    const service = document.createElement('span');
    service.className = `log-service ${log.service}`;
    service.textContent = log.service.toUpperCase();

    const message = document.createElement('span');
    message.className = 'log-message';
    message.textContent = log.message;

    entry.appendChild(timestamp);
    entry.appendChild(service);
    entry.appendChild(message);

    return entry;
}

// Handle filter changes
serviceFilters.forEach(filter => {
    filter.addEventListener('change', (e) => {
        const service = e.target.dataset.service;
        filters[service] = e.target.checked;
        
        // Update visibility of existing logs
        document.querySelectorAll(`.log-entry.${service}`).forEach(entry => {
            entry.style.display = e.target.checked ? 'flex' : 'none';
        });
    });
});

// Clear logs
clearLogsButton.addEventListener('click', () => {
    logContainer.innerHTML = '';
});