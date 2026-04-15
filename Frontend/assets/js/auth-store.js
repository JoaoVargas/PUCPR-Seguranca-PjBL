import { AUTH_BRIDGE_KEY } from "./config.js";

let token = null;

export const AuthStore = {
  set(value) {
    token = value || null;
  },
  get() {
    return token;
  },
  clear() {
    token = null;
  },
  isAuth() {
    return Boolean(token);
  },
  persistBridge() {
    if (token) {
      sessionStorage.setItem(AUTH_BRIDGE_KEY, token);
    }
  },
  hydrateFromBridge() {
    const bridgeToken = sessionStorage.getItem(AUTH_BRIDGE_KEY);
    if (!bridgeToken) {
      return null;
    }

    token = bridgeToken;
    sessionStorage.removeItem(AUTH_BRIDGE_KEY);
    return bridgeToken;
  }
};
