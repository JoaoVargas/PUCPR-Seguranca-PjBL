const strengthSteps = [
  { width: "0%", color: "#ccc", label: "Digite uma senha" },
  { width: "25%", color: "#e74c3c", label: "Fraca" },
  { width: "50%", color: "#e67e22", label: "Razoavel" },
  { width: "75%", color: "#f1c40f", label: "Boa" },
  { width: "100%", color: "var(--success)", label: "Forte" }
];

export function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

export function validatePassword(password) {
  return password.length >= 8 && /[A-Z]/.test(password) && /[0-9]/.test(password);
}

export function passwordStrength(password) {
  let score = 0;
  if (password.length >= 8) score += 1;
  if (/[A-Z]/.test(password)) score += 1;
  if (/[0-9]/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;

  return strengthSteps[score];
}
