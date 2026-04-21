import { API } from "./config.js";
import { AuthStore } from "./auth-store.js";
import { escapeHtml } from "./ui.js";

let adminUsersCache = [];
let currentAdminEmail = "";

function base64UrlDecode(value) {
  let output = value.replace(/-/g, "+").replace(/_/g, "/");
  while (output.length % 4) {
    output += "=";
  }

  try {
    return JSON.parse(atob(output));
  } catch {
    return null;
  }
}

function getRoleLabel(payload) {
  const role = payload?.role ?? payload?.roles ?? "";
  return Array.isArray(role) ? role[0] : role;
}

function isAdminPayload(payload) {
  return String(getRoleLabel(payload) || "").toUpperCase() === "ADMIN";
}

function renderToken(token) {
  const parts = token.split(".");
  if (parts.length !== 3) {
    return null;
  }

  const [headerSegment, payloadSegment, signatureSegment] = parts;
  const payload = base64UrlDecode(payloadSegment);

  document.getElementById("raw-header").textContent = headerSegment;
  document.getElementById("raw-payload").textContent = payloadSegment;
  document.getElementById("raw-sig").textContent = signatureSegment;

  if (!payload) {
    return null;
  }

  document.getElementById("info-email").textContent = payload.sub || "-";
  document.getElementById("info-role").textContent = getRoleLabel(payload) || "-";
  document.getElementById("nav-email").textContent = payload.sub || "-";
  document.getElementById("welcome-msg").textContent = `Bem-vindo, ${payload.name || payload.sub}!`;

  const formatDate = (timestamp) => (timestamp ? new Date(timestamp * 1000).toLocaleString("pt-BR") : "-");
  document.getElementById("info-iat").textContent = formatDate(payload.iat);
  document.getElementById("info-exp").textContent = formatDate(payload.exp);

  document.getElementById("payload-decoded").textContent = JSON.stringify(payload, null, 2);
  return payload;
}

function shortenId(id) {
  if (!id) {
    return "-";
  }

  const value = String(id);
  return value.length > 12 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function renderUsers(users, currentUserEmail) {
  const body = document.getElementById("users-table-body");
  const emptyState = document.getElementById("users-empty");
  const countLabel = document.getElementById("admin-count");

  countLabel.textContent = `${users.length} usuario${users.length === 1 ? "" : "s"}`;
  body.innerHTML = "";

  if (!users.length) {
    emptyState.textContent = "Nenhum usuario cadastrado.";
    emptyState.hidden = false;
    body.innerHTML = '<tr><td colspan="5">Sem registros para exibir.</td></tr>';
    return;
  }

  emptyState.hidden = true;
  emptyState.classList.remove("admin-error");

  for (const user of users) {
    const row = document.createElement("tr");

    const idCell = document.createElement("td");
    idCell.innerHTML = `<code>${escapeHtml(shortenId(user.id))}</code>`;

    const nameCell = document.createElement("td");
    nameCell.textContent = user.nome || "-";

    const emailCell = document.createElement("td");
    emailCell.textContent = user.email || "-";

    const roleCell = document.createElement("td");
    roleCell.innerHTML = `<span class="tag">${escapeHtml(user.tipo || "-")}</span>`;

    const actionsCell = document.createElement("td");
    actionsCell.className = "actions-cell";

    const canDelete = normalizeEmail(user.email) !== normalizeEmail(currentUserEmail);
    if (canDelete) {
      const deleteBtn = document.createElement("button");
      deleteBtn.type = "button";
      deleteBtn.className = "delete-user-btn";
      deleteBtn.setAttribute("aria-label", `Excluir usuario ${user.email || user.nome || ""}`);
      deleteBtn.title = "Excluir usuario";
      deleteBtn.textContent = "🗑";
      deleteBtn.addEventListener("click", () => handleDeleteUser(user));
      actionsCell.appendChild(deleteBtn);
    } else {
      actionsCell.textContent = "-";
    }

    row.append(idCell, nameCell, emailCell, roleCell, actionsCell);
    body.appendChild(row);
  }
}

function renderUsersError(message) {
  const body = document.getElementById("users-table-body");
  const emptyState = document.getElementById("users-empty");
  const countLabel = document.getElementById("admin-count");

  countLabel.textContent = "0 usuarios";
  emptyState.textContent = message;
  emptyState.hidden = false;
  emptyState.classList.add("admin-error");
  body.innerHTML = `<tr><td colspan="5">${escapeHtml(message)}</td></tr>`;
}

async function fetchAuthenticated(url, options = {}) {
  const token = AuthStore.get();
  if (!token) {
    window.location.href = "index.html";
    return null;
  }

  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${token}`
    }
  });

  if (response.status === 401) {
    AuthStore.clear();
    window.location.href = "index.html";
    return null;
  }

  return response;
}

async function loadAdminUsers() {
  const response = await fetchAuthenticated(`${API}/users`);
  if (!response) {
    return;
  }

  if (response.status === 403) {
    renderUsersError("Acesso restrito a administradores.");
    return;
  }

  if (!response.ok) {
    renderUsersError("Nao foi possivel carregar a lista de usuarios.");
    return;
  }

  const data = await response.json();
  adminUsersCache = Array.isArray(data.users) ? data.users : [];
  renderUsers(adminUsersCache, currentAdminEmail);
}

async function handleDeleteUser(user) {
  if (normalizeEmail(user?.email) === normalizeEmail(currentAdminEmail)) {
    renderUsersError("Voce nao pode excluir seu proprio usuario.");
    return;
  }

  const identifier = user?.id;
  if (!identifier) {
    renderUsersError("Usuario invalido para exclusao.");
    return;
  }

  const confirmed = window.confirm(`Deseja excluir o usuario ${user.email || user.nome || "selecionado"}?`);
  if (!confirmed) {
    return;
  }

  const response = await fetchAuthenticated(`${API}/users/${encodeURIComponent(identifier)}`, {
    method: "DELETE"
  });

  if (!response) {
    return;
  }

  if (response.status === 403) {
    renderUsersError("Acesso restrito a administradores.");
    return;
  }

  if (!response.ok) {
    let message = "Nao foi possivel excluir o usuario.";
    try {
      const payload = await response.json();
      if (payload?.error) {
        message = payload.error;
      }
    } catch {
      // keep default message when response has no json
    }
    renderUsersError(message);
    return;
  }

  adminUsersCache = adminUsersCache.filter((item) => item?.id !== identifier);
  renderUsers(adminUsersCache, currentAdminEmail);
}

async function logout() {
  const token = AuthStore.get();

  try {
    await fetch(`${API}/auth/logout`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` }
    });
  } catch {
    // Ignore logout transport errors and clear local state.
  }

  AuthStore.clear();
  window.location.href = "index.html";
}

function initializePage() {
  AuthStore.hydrateFromBridge();

  if (!AuthStore.isAuth()) {
    window.location.href = "index.html";
    return;
  }

  document.getElementById("logout-btn").addEventListener("click", logout);

  document.getElementById("loading").style.display = "none";
  document.getElementById("main-content").style.display = "block";

  const payload = renderToken(AuthStore.get());
  if (isAdminPayload(payload)) {
    currentAdminEmail = payload?.sub || "";
    document.getElementById("admin-panel").hidden = false;
    loadAdminUsers();
  }
}

document.addEventListener("DOMContentLoaded", initializePage);
