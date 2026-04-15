import { API } from "./config.js";
import { AuthStore } from "./auth-store.js";
import { showAlert, hideAlert, setFieldError, clearErrors, markInput } from "./ui.js";
import { validateEmail, validatePassword, passwordStrength } from "./validators.js";

function switchTab(tab) {
  document.querySelectorAll(".tab-panel").forEach((panel) => panel.classList.remove("active"));
  document.querySelectorAll(".tab-btn").forEach((button) => button.classList.remove("active"));

  const panel = document.getElementById(`panel-${tab}`);
  const tabButton = document.querySelector(`[data-tab="${tab}"]`);
  if (panel) {
    panel.classList.add("active");
  }
  if (tabButton) {
    tabButton.classList.add("active");
  }
}

function updateStrength(password) {
  const fill = document.getElementById("strength-fill");
  const label = document.getElementById("strength-label");
  const state = passwordStrength(password);

  fill.style.width = state.width;
  fill.style.background = state.color;
  label.textContent = state.label;
  label.style.color = state.color;
}

async function handleLogin() {
  hideAlert("login-alert");
  clearErrors("login-email-err", "login-pass-err");

  const email = document.getElementById("login-email").value.trim();
  const password = document.getElementById("login-password").value;

  let valid = true;

  if (!validateEmail(email)) {
    setFieldError("login-email-err", "Informe um e-mail valido.");
    markInput("login-email", false);
    valid = false;
  } else {
    markInput("login-email", true);
  }

  if (!password) {
    setFieldError("login-pass-err", "A senha e obrigatoria.");
    markInput("login-password", false);
    valid = false;
  } else {
    markInput("login-password", true);
  }

  if (!valid) {
    return;
  }

  const button = document.getElementById("login-btn");
  button.disabled = true;
  button.textContent = "Autenticando...";

  try {
    const response = await fetch(`${API}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, senha: password })
    });

    const data = await response.json();

    if (response.ok) {
      AuthStore.set(data.token);
      AuthStore.persistBridge();
      window.location.href = "dashboard.html";
      return;
    }

    showAlert("login-alert", data.message || "Credenciais invalidas. Tente novamente.");
  } catch {
    showAlert("login-alert", "Nao foi possivel conectar ao servidor. Verifique se a API esta rodando.");
  } finally {
    button.disabled = false;
    button.textContent = "Entrar";
  }
}

async function handleRegister() {
  hideAlert("reg-alert");
  clearErrors("reg-name-err", "reg-email-err", "reg-pass-err", "reg-confirm-err");

  const name = document.getElementById("reg-name").value.trim();
  const email = document.getElementById("reg-email").value.trim();
  const password = document.getElementById("reg-password").value;
  const confirm = document.getElementById("reg-confirm").value;

  let valid = true;

  if (name.length < 3) {
    setFieldError("reg-name-err", "Nome deve ter ao menos 3 caracteres.");
    markInput("reg-name", false);
    valid = false;
  } else {
    markInput("reg-name", true);
  }

  if (!validateEmail(email)) {
    setFieldError("reg-email-err", "Informe um e-mail valido.");
    markInput("reg-email", false);
    valid = false;
  } else {
    markInput("reg-email", true);
  }

  if (!validatePassword(password)) {
    setFieldError("reg-pass-err", "Minimo 8 caracteres, com maiuscula e numero.");
    markInput("reg-password", false);
    valid = false;
  } else {
    markInput("reg-password", true);
  }

  if (password !== confirm) {
    setFieldError("reg-confirm-err", "As senhas nao coincidem.");
    markInput("reg-confirm", false);
    valid = false;
  } else if (confirm) {
    markInput("reg-confirm", true);
  }

  if (!valid) {
    return;
  }

  const button = document.getElementById("reg-btn");
  button.disabled = true;
  button.textContent = "Criando conta...";

  try {
    const response = await fetch(`${API}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nome: name, email, senha: password })
    });

    const data = await response.json();

    if (response.ok || response.status === 201) {
      showAlert("reg-alert", "Conta criada! Faca login para continuar.", "success");
      setTimeout(() => switchTab("login"), 2000);
      return;
    }

    showAlert("reg-alert", data.message || "Erro ao criar conta. Tente novamente.");
  } catch {
    showAlert("reg-alert", "Nao foi possivel conectar ao servidor. Verifique se a API esta rodando.");
  } finally {
    button.disabled = false;
    button.textContent = "Criar conta";
  }
}

function bindEvents() {
  document.querySelectorAll(".tab-btn").forEach((button) => {
    button.addEventListener("click", () => switchTab(button.dataset.tab));
  });

  document.getElementById("reg-password").addEventListener("input", (event) => {
    updateStrength(event.target.value);
  });

  document.getElementById("login-btn").addEventListener("click", handleLogin);
  document.getElementById("reg-btn").addEventListener("click", handleRegister);

  document.addEventListener("keydown", (event) => {
    if (event.key !== "Enter") {
      return;
    }

    const activePanel = document.querySelector(".tab-panel.active");
    if (!activePanel) {
      return;
    }

    if (activePanel.id === "panel-login") {
      handleLogin();
    }

    if (activePanel.id === "panel-register") {
      handleRegister();
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  updateStrength("");
});
