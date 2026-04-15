export function showAlert(id, message, type = "danger") {
  const el = document.getElementById(id);
  if (!el) {
    return;
  }

  el.textContent = message;
  el.className = `alert alert-${type} show`;
}

export function hideAlert(id) {
  const el = document.getElementById(id);
  if (!el) {
    return;
  }

  el.className = "alert";
}

export function setFieldError(id, message) {
  const el = document.getElementById(id);
  if (el) {
    el.textContent = message;
  }
}

export function clearErrors(...ids) {
  ids.forEach((id) => {
    const el = document.getElementById(id);
    if (el) {
      el.textContent = "";
    }
  });
}

export function markInput(id, valid) {
  const el = document.getElementById(id);
  if (!el) {
    return;
  }

  el.classList.toggle("error", !valid);
  el.classList.toggle("ok", valid);
}

export function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
