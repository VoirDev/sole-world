const clientsBody = document.querySelector("[data-clients]");
const dialog = document.querySelector("[data-client-dialog]");
const form = document.querySelector("[data-client-form]");
const title = document.querySelector("[data-dialog-title]");
const secretPanel = document.querySelector("[data-secret-panel]");
const secretValue = document.querySelector("[data-secret]");
const copySecretButton = document.querySelector("[data-copy-secret]");
const openCreateButton = document.querySelector("[data-open-create]");
const saveClientButton = document.querySelector("[data-save-client]");

let clients = [];
let csrfToken = null;

openCreateButton.disabled = true;
saveClientButton.disabled = true;

openCreateButton.addEventListener("click", () => openForm());
saveClientButton.addEventListener("click", saveClient);
document.querySelectorAll("[data-close-dialog]").forEach((button) => {
    button.addEventListener("click", () => dialog.close());
});
copySecretButton.addEventListener("click", async () => {
    await navigator.clipboard.writeText(secretValue.textContent);
    copySecretButton.textContent = "Copied";
});

clientsBody.addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const id = Number(button.dataset.id);
    const client = clients.find((item) => item.id === id);
    if (!client) return;

    if (button.dataset.action === "edit") {
        openForm(client);
    }

    if (button.dataset.action === "rotate") {
        if (!confirm(`Rotate access key for ${client.name}?`)) return;
        const result = await request(`/admin/api/clients/${id}/rotate-key`, { method: "POST" });
        showSecret(result.accessKey);
        await loadClients();
    }

    if (button.dataset.action === "delete") {
        if (!confirm(`Delete ${client.name}?`)) return;
        await request(`/admin/api/clients/${id}`, { method: "DELETE" });
        hideSecret();
        await loadClients();
    }
});

async function loadClients() {
    clientsBody.innerHTML = `<tr><td colspan="5" class="empty-state">Loading clients...</td></tr>`;
    clients = await request("/admin/api/clients");
    renderClients();
}

function renderClients() {
    if (clients.length === 0) {
        clientsBody.innerHTML = `<tr><td colspan="5" class="empty-state">No clients yet.</td></tr>`;
        return;
    }

    clientsBody.innerHTML = clients.map((client) => `
        <tr>
            <td><strong>${escapeHtml(client.name)}</strong></td>
            <td>${escapeHtml(client.description || "")}</td>
            <td><code>${escapeHtml(client.accessKeyPrefix)}</code></td>
            <td><span class="status">${client.active ? "Active" : "Inactive"}</span></td>
            <td class="actions">
                <button class="secondary-button" type="button" data-action="edit" data-id="${client.id}">Edit</button>
                <button class="secondary-button" type="button" data-action="rotate" data-id="${client.id}">Rotate key</button>
                <button class="danger-button" type="button" data-action="delete" data-id="${client.id}">Delete</button>
            </td>
        </tr>
    `).join("");
}

function openForm(client = null) {
    form.reset();
    form.elements.id.value = client?.id || "";
    form.elements.name.value = client?.name || "";
    form.elements.description.value = client?.description || "";
    title.textContent = client ? "Edit client" : "Create client";
    dialog.showModal();
}

async function saveClient() {
    if (!form.reportValidity()) return;

    const id = form.elements.id.value;
    const payload = {
        name: form.elements.name.value,
        description: form.elements.description.value || null,
    };

    if (id) {
        await request(`/admin/api/clients/${id}`, {
            method: "PUT",
            body: JSON.stringify(payload),
        });
    } else {
        const result = await request("/admin/api/clients", {
            method: "POST",
            body: JSON.stringify(payload),
        });
        showSecret(result.accessKey);
    }

    dialog.close();
    await loadClients();
}

async function request(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const safeMethod = method === "GET" || method === "HEAD" || method === "OPTIONS";
    if (!safeMethod && !csrfToken) {
        throw new Error("Security token unavailable. Refresh the page.");
    }
    const csrfHeaders = safeMethod ? {} : { [csrfToken.headerName]: csrfToken.token };

    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            ...csrfHeaders,
            ...(options.headers || {}),
        },
        ...options,
    });

    if (response.status === 401 || response.status === 403) {
        window.location.href = "/admin/login";
        return;
    }

    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `Request failed with ${response.status}`);
    }

    if (response.status === 204) return null;
    return response.json();
}

function showSecret(secret) {
    secretValue.textContent = secret;
    copySecretButton.textContent = "Copy";
    secretPanel.hidden = false;
}

function hideSecret() {
    secretValue.textContent = "";
    copySecretButton.textContent = "Copy";
    secretPanel.hidden = true;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function loadCsrfToken() {
    const response = await fetch("/admin/api/csrf", {
        headers: { "Accept": "application/json" },
    });

    if (!response.ok) {
        throw new Error(`Unable to load CSRF token (${response.status})`);
    }

    csrfToken = await response.json();
    openCreateButton.disabled = false;
    saveClientButton.disabled = false;

    document.querySelectorAll("form[method='post']").forEach((formElement) => {
        let input = formElement.querySelector(`input[name="${csrfToken.parameterName}"]`);
        if (!input) {
            input = document.createElement("input");
            input.type = "hidden";
            input.name = csrfToken.parameterName;
            formElement.append(input);
        }
        input.value = csrfToken.token;
    });
}

loadCsrfToken().then(loadClients).catch((error) => {
    clientsBody.innerHTML = `<tr><td colspan="5" class="empty-state">${escapeHtml(error.message)}</td></tr>`;
});
