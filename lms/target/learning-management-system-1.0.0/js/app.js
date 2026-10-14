/* ================================================================
   LMS – Global API + Utility JavaScript
   ================================================================ */

const API = (() => {
    const BASE = '';  // same origin

    async function request(method, url, body, isMultipart = false) {
        const opts = { method, credentials: 'include' };
        if (body && !isMultipart) {
            opts.headers = { 'Content-Type': 'application/json' };
            opts.body = JSON.stringify(body);
        } else if (body && isMultipart) {
            opts.body = body; // FormData
        }
        const res = await fetch(BASE + url, opts);
        const text = await res.text();
        let data;
        try { data = text ? JSON.parse(text) : {}; }
        catch { data = { message: text }; }
        if (!res.ok) throw { status: res.status, message: data.error || data.message || 'Request failed' };
        return data;
    }

    return {
        get:    (url)          => request('GET',    url),
        post:   (url, body)    => request('POST',   url, body),
        put:    (url, body)    => request('PUT',    url, body),
        delete: (url)          => request('DELETE', url),
        upload: (url, formData) => request('POST',  url, formData, true),
    };
})();

/* ---------- Auth helpers ---------- */
const Auth = {
    getUser() {
        try { return JSON.parse(sessionStorage.getItem('lmsUser') || 'null'); }
        catch { return null; }
    },
    setUser(u) { sessionStorage.setItem('lmsUser', JSON.stringify(u)); },
    clear()    { sessionStorage.removeItem('lmsUser'); },
    isStudent()    { return this.getUser()?.role === 'STUDENT'; },
    isInstructor() { return this.getUser()?.role === 'INSTRUCTOR'; },
    requireAuth(role) {
        const u = this.getUser();
        if (!u) { location.href = '/lms/login.html'; return false; }
        if (role && u.role !== role) {
            location.href = u.role === 'STUDENT'
                ? '/lms/student/dashboard.html'
                : '/lms/instructor/dashboard.html';
            return false;
        }
        return true;
    }
};

/* ---------- UI helpers ---------- */
const UI = {
    toast(message, type = 'success', duration = 3500) {
        const colours = {
            success: '#22c55e', danger: '#ef4444',
            info: '#38bdf8', warning: '#f59e0b'
        };
        const icons = { success: '✓', danger: '✕', info: 'ℹ', warning: '⚠' };
        const el = document.createElement('div');
        el.style.cssText = `
            position:fixed; bottom:1.5rem; right:1.5rem; z-index:9999;
            background:#1e2235; border:1px solid ${colours[type]}44;
            color:#f1f5f9; padding:.85rem 1.25rem; border-radius:12px;
            font-size:.875rem; display:flex; align-items:center; gap:.6rem;
            box-shadow:0 8px 32px rgba(0,0,0,.5); max-width:360px;
            animation:slideUp .3s cubic-bezier(.34,1.56,.64,1);
        `;
        el.innerHTML = `<span style="color:${colours[type]};font-weight:700">${icons[type]}</span>${message}`;
        document.body.appendChild(el);
        setTimeout(() => { el.style.animation = 'fadeOut .3s ease forwards'; setTimeout(() => el.remove(), 300); }, duration);
    },

    showAlert(containerId, message, type = 'danger') {
        const el = document.getElementById(containerId);
        if (!el) return;
        el.innerHTML = `<div class="alert alert-${type}">${message}</div>`;
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        setTimeout(() => { el.innerHTML = ''; }, 5000);
    },

    showLoading(show = true) {
        let overlay = document.getElementById('loading-overlay');
        if (show) {
            if (!overlay) {
                overlay = document.createElement('div');
                overlay.id = 'loading-overlay';
                overlay.className = 'loading-overlay';
                overlay.innerHTML = `<div class="spinner"></div><p style="color:#94a3b8;font-size:.85rem">Loading…</p>`;
                document.body.appendChild(overlay);
            }
        } else if (overlay) {
            overlay.remove();
        }
    },

    setButtonLoading(btn, loading) {
        if (!btn) return;
        if (loading) {
            btn.dataset.originalText = btn.innerHTML;
            btn.innerHTML = `<span class="spinner" style="width:16px;height:16px;border-width:2px"></span> Loading…`;
            btn.disabled = true;
        } else {
            btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
            btn.disabled = false;
        }
    },

    badge(text, type = 'muted') {
        return `<span class="badge badge-${type}">${text}</span>`;
    },

    formatDate(isoString) {
        if (!isoString) return '—';
        return new Date(isoString).toLocaleDateString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric'
        });
    },

    formatDateTime(isoString) {
        if (!isoString) return '—';
        return new Date(isoString).toLocaleString('en-IN', {
            day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    },

    isPastDeadline(deadline) {
        return deadline && new Date(deadline) < new Date();
    },

    progressBar(pct) {
        const colour = pct >= 80 ? '#22c55e' : pct >= 40 ? '#6c63ff' : '#f59e0b';
        return `
            <div class="progress-track">
                <div class="progress-bar" style="width:${pct}%;background:${colour}"></div>
            </div>
            <span class="text-xs text-muted mt-1">${pct.toFixed(0)}%</span>
        `;
    },

    emptyState(icon, title, subtitle = '') {
        return `
            <div style="text-align:center;padding:3rem;color:var(--text-muted)">
                <div style="font-size:3rem;margin-bottom:1rem">${icon}</div>
                <h3 style="color:var(--text-secondary);margin-bottom:.5rem">${title}</h3>
                <p style="font-size:.875rem">${subtitle}</p>
            </div>
        `;
    }
};

/* ---------- Notification badge ---------- */
async function loadNotificationCount() {
    try {
        const notifications = await API.get('/api/notifications');
        const unread = notifications.filter(n => !n.read).length;
        const badge = document.getElementById('notif-badge');
        if (badge) {
            badge.textContent = unread;
            badge.style.display = unread > 0 ? 'inline-flex' : 'none';
        }
    } catch (_) {}
}

/* ---------- Logout ---------- */
async function logout() {
    try { await API.post('/api/auth/logout'); } catch (_) {}
    Auth.clear();
    location.href = '/lms/login.html';
}

/* ---------- Add slide-up and fadeOut keyframes ---------- */
(function addKeyframes() {
    const style = document.createElement('style');
    style.textContent = `
        @keyframes slideUp { from{opacity:0;transform:translateY(20px)} to{opacity:1;transform:none} }
        @keyframes fadeOut { to{opacity:0;transform:translateY(10px)} }
    `;
    document.head.appendChild(style);
})();
