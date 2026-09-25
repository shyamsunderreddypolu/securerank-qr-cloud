// State Management
const API_BASE = '/api';
let currentUser = null;

// On Page Load
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    checkSession();
});

function initTheme() {
    const savedTheme = localStorage.getItem('securerank_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateThemeIcon(savedTheme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme') || 'dark';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('securerank_theme', newTheme);
    updateThemeIcon(newTheme);
}

function updateThemeIcon(theme) {
    const icon = document.getElementById('themeIcon');
    if (icon) {
        if (theme === 'dark') {
            icon.className = 'fa-solid fa-sun text-warning';
        } else {
            icon.className = 'fa-solid fa-moon text-primary';
        }
    }
}

function checkSession() {
    const token = sessionStorage.getItem('jwt_token');
    const userJson = sessionStorage.getItem('user_data');

    if (token && userJson) {
        currentUser = JSON.parse(userJson);
        renderAuthenticatedUI();
    } else {
        renderAuthView();
    }
}

function showAlert(message, type = 'success') {
    const alertPlaceholder = document.getElementById('alertPlaceholder');
    alertPlaceholder.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show card-custom border-${type}" role="alert">
            <div class="d-flex align-items-center gap-2">
                <i class="fa-solid ${type === 'success' ? 'fa-circle-check text-success' : 'fa-triangle-exclamation text-danger'}"></i>
                <div>${message}</div>
            </div>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
    setTimeout(() => {
        const bsAlert = bootstrap.Alert.getOrCreateInstance(alertPlaceholder.querySelector('.alert'));
        if (bsAlert) bsAlert.close();
    }, 5000);
}

function fillDemo(email, pass) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = pass;
}

// ----------------- AUTHENTICATION -----------------

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;
    const btn = document.getElementById('loginBtn');

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Signing in...';

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();

        if (res.ok) {
            sessionStorage.setItem('jwt_token', data.token);
            sessionStorage.setItem('user_data', JSON.stringify(data));
            currentUser = data;
            showAlert(`Welcome back, ${data.name}!`, 'success');
            renderAuthenticatedUI();
        } else {
            showAlert(data.message || 'Login failed. Please check your credentials.', 'danger');
        }
    } catch (err) {
        showAlert('Network error: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Sign In';
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('regName').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const role = document.getElementById('regRole').value;
    const mobile = document.getElementById('regMobile').value;
    const btn = document.getElementById('regBtn');

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Registering...';

    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password, role, mobile })
        });

        const data = await res.json();

        if (res.ok) {
            showAlert(data.message || 'Registration successful! You can now login.', 'success');
            document.getElementById('registerForm').reset();
            const loginTab = new bootstrap.Tab(document.getElementById('login-tab'));
            loginTab.show();
        } else {
            showAlert(data.message || 'Registration failed.', 'danger');
        }
    } catch (err) {
        showAlert('Network error: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Create Account';
    }
}

function logout() {
    sessionStorage.removeItem('jwt_token');
    sessionStorage.removeItem('user_data');
    currentUser = null;
    renderAuthView();
    showAlert('You have been logged out safely.', 'info');
}

// ----------------- UI VIEW ROUTING -----------------

function renderAuthView() {
    document.getElementById('authView').classList.remove('d-none');
    document.getElementById('ownerDashboardView').classList.add('d-none');
    document.getElementById('consumerDashboardView').classList.add('d-none');
    document.getElementById('adminDashboardView').classList.add('d-none');
    document.getElementById('userBadgeContainer').classList.add('d-none');
    document.getElementById('logoutNavItem').classList.add('d-none');
}

function renderAuthenticatedUI() {
    document.getElementById('authView').classList.add('d-none');
    document.getElementById('userBadgeContainer').classList.remove('d-none');
    document.getElementById('logoutNavItem').classList.remove('d-none');

    document.getElementById('userNameLabel').innerText = currentUser.name;
    document.getElementById('userRoleBadge').innerText = currentUser.role.replace('ROLE_', '');

    if (currentUser.role === 'ROLE_OWNER') {
        document.getElementById('ownerDashboardView').classList.remove('d-none');
        document.getElementById('consumerDashboardView').classList.add('d-none');
        document.getElementById('adminDashboardView').classList.add('d-none');
        loadMyFiles();
    } else if (currentUser.role === 'ROLE_CONSUMER') {
        document.getElementById('consumerDashboardView').classList.remove('d-none');
        document.getElementById('ownerDashboardView').classList.add('d-none');
        document.getElementById('adminDashboardView').classList.add('d-none');
        loadMyKeyRequests();
    } else if (currentUser.role === 'ROLE_ADMIN' || currentUser.role === 'ROLE_PKG') {
        document.getElementById('adminDashboardView').classList.remove('d-none');
        document.getElementById('ownerDashboardView').classList.add('d-none');
        document.getElementById('consumerDashboardView').classList.add('d-none');
        loadAdminData();
    }
}

function getAuthHeaders() {
    return {
        'Authorization': `Bearer ${sessionStorage.getItem('jwt_token')}`
    };
}

// ----------------- DATA OWNER FUNCTIONS -----------------

async function handleFileUpload(e) {
    e.preventDefault();
    const fileInput = document.getElementById('uploadFileInput');
    const label = document.getElementById('uploadLabelInput').value;
    const keywords = document.getElementById('uploadKeywordsInput').value;
    const btn = document.getElementById('uploadBtn');

    if (fileInput.files.length === 0) return;

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('label', label);
    formData.append('keywords', keywords);

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Encrypting & Uploading...';

    try {
        const res = await fetch(`${API_BASE}/files/upload`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: formData
        });

        const data = await res.json();

        if (res.ok) {
            showAlert(data.message || 'File encrypted and stored successfully!', 'success');
            document.getElementById('uploadForm').reset();
            loadMyFiles();
        } else {
            showAlert(data.message || 'Upload failed.', 'danger');
        }
    } catch (err) {
        showAlert('Upload network error: ' + err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Encrypt & Upload';
    }
}

async function loadMyFiles() {
    const tbody = document.getElementById('myFilesTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3"><span class="spinner-border spinner-border-sm"></span> Loading...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/files/my-files`, {
            headers: getAuthHeaders()
        });
        const files = await res.json();

        if (files.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No files uploaded yet.</td></tr>';
            return;
        }

        tbody.innerHTML = files.map(f => `
            <tr>
                <td class="fw-semibold text-white"><i class="fa-solid fa-file-shield text-primary me-2"></i>${escapeHtml(f.filename)}</td>
                <td class="text-light">${escapeHtml(f.label || '-')}</td>
                <td class="text-light">${(f.fileSize / 1024).toFixed(1)} KB</td>
                <td class="text-secondary small">${new Date(f.uploadedAt).toLocaleString()}</td>
                <td>
                    <button class="btn btn-sm btn-outline-info" onclick="viewQRAndVC(${f.id}, '${escapeHtml(f.filename)}')">
                        <i class="fa-solid fa-qrcode me-1"></i> QR & VC Shares
                    </button>
                </td>
                <td>
                    <button class="btn btn-sm btn-outline-warning" onclick="viewBenchmark(${f.id}, '${escapeHtml(f.filename)}')">
                        <i class="fa-solid fa-chart-line me-1"></i> Benchmark
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-danger text-center py-3">Error loading files: ${err.message}</td></tr>`;
    }
}

// ----------------- DATA CONSUMER FUNCTIONS -----------------

async function handleSearch(e) {
    e.preventDefault();
    const query = document.getElementById('searchQueryInput').value.trim();
    const btn = document.getElementById('searchBtn');
    const resultsCard = document.getElementById('searchResultsCard');
    const tbody = document.getElementById('searchResultsTableBody');

    if (!query) return;

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    resultsCard.style.display = 'block';
    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-3"><span class="spinner-border spinner-border-sm"></span> Matching encrypted indexes...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/files/search?query=${encodeURIComponent(query)}`, {
            headers: getAuthHeaders()
        });
        const results = await res.json();

        if (results.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No matching documents found.</td></tr>';
            return;
        }

        tbody.innerHTML = results.map(r => {
            let actionBtn = '';
            if (r.keyRequestStatus === 'APPROVED') {
                actionBtn = `<button class="btn btn-sm btn-success" onclick="downloadFile(${r.id}, '${escapeHtml(r.filename)}')"><i class="fa-solid fa-download me-1"></i> Download</button>`;
            } else if (r.keyRequestStatus === 'PENDING') {
                actionBtn = `<span class="badge bg-warning text-dark">Pending Key</span>`;
            } else {
                actionBtn = `<button class="btn btn-sm btn-outline-primary" onclick="openRequestKeyModal(${r.id}, '${escapeHtml(r.filename)}')"><i class="fa-solid fa-key me-1"></i> Request Key</button>`;
            }

            return `
                <tr>
                    <td><span class="badge bg-secondary">#${r.rank}</span></td>
                    <td class="fw-semibold text-white">${escapeHtml(r.filename)}</td>
                    <td class="text-light">${escapeHtml(r.label || '-')}</td>
                    <td class="text-secondary small">${escapeHtml(r.ownerEmail)}</td>
                    <td><span class="score-pill">${r.score.toFixed(4)}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-info" onclick="viewQRAndVC(${r.id}, '${escapeHtml(r.filename)}')">
                            <i class="fa-solid fa-qrcode me-1"></i> View QR
                        </button>
                    </td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-danger text-center py-3">Error searching files: ${err.message}</td></tr>`;
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Search';
    }
}

function openRequestKeyModal(fileId, filename) {
    document.getElementById('modalRequestFileId').value = fileId;
    document.getElementById('modalRequestFileName').value = filename;
    document.getElementById('modalRequestReason').value = '';
    const modalEl = document.getElementById('requestKeyModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

async function submitKeyRequestWithReason() {
    const fileId = document.getElementById('modalRequestFileId').value;
    const reason = document.getElementById('modalRequestReason').value.trim();

    if (!reason) {
        showAlert('Please provide an access reason or purpose.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/files/request-key/${fileId}`, {
            method: 'POST',
            headers: {
                ...getAuthHeaders(),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ accessReason: reason })
        });
        const data = await res.json();
        showAlert(data.message, data.success ? 'success' : 'warning');
        
        const modalEl = document.getElementById('requestKeyModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();

        loadMyKeyRequests();
        const searchForm = document.getElementById('searchForm');
        if (searchForm && document.getElementById('searchQueryInput').value.trim()) {
            searchForm.dispatchEvent(new Event('submit'));
        }
    } catch (err) {
        showAlert('Request error: ' + err.message, 'danger');
    }
}

async function loadMyKeyRequests() {
    const tbody = document.getElementById('myRequestsTableBody');

    try {
        const res = await fetch(`${API_BASE}/files/my-requests`, {
            headers: getAuthHeaders()
        });
        const requests = await res.json();

        if (requests.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No key requests found.</td></tr>';
            return;
        }

        tbody.innerHTML = requests.map(req => {
            let statusBadge = '';
            let actionBtn = '-';

            if (req.status === 'APPROVED') {
                statusBadge = '<span class="badge bg-success">APPROVED</span>';
                actionBtn = `<button class="btn btn-sm btn-success" onclick="downloadFile(${req.fileId}, '${escapeHtml(req.filename)}')"><i class="fa-solid fa-download me-1"></i> Download</button>`;
            } else if (req.status === 'PENDING') {
                statusBadge = '<span class="badge bg-warning text-dark">PENDING</span>';
            } else {
                statusBadge = '<span class="badge bg-danger">REJECTED</span>';
            }

            return `
                <tr>
                    <td class="fw-semibold text-white">${escapeHtml(req.filename)}</td>
                    <td class="text-secondary small">${escapeHtml(req.ownerEmail)}</td>
                    <td class="small text-info">${escapeHtml(req.accessReason || 'Standard file access')}</td>
                    <td>${statusBadge}</td>
                    <td><code>${req.masterKey || '••••••••'}</code></td>
                    <td>${actionBtn}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-danger text-center py-3">Error loading requests: ${err.message}</td></tr>`;
    }
}

async function downloadFile(fileId, filename) {
    try {
        showAlert('Decrypting and downloading file...', 'info');
        const res = await fetch(`${API_BASE}/files/download/${fileId}`, {
            headers: getAuthHeaders()
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Download failed');
        }

        const blob = await res.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
        showAlert('Decrypted file downloaded successfully!', 'success');
    } catch (err) {
        showAlert('Download error: ' + err.message, 'danger');
    }
}

// ----------------- VISUAL CRYPTO & BENCHMARK FUNCTIONS -----------------

async function viewQRAndVC(fileId, filename) {
    try {
        const modalEl = document.getElementById('qrVcModal');
        const modal = new bootstrap.Modal(modalEl);

        document.getElementById('modalDocName').innerText = filename;
        document.getElementById('modalBitLength').innerText = '... bits';
        document.getElementById('modalEntropy').innerText = 'Calculating...';
        document.getElementById('modalBitStreamPreview').innerText = 'Fetching stream...';
        document.getElementById('imgOriginalQR').src = '';
        document.getElementById('imgShare1').src = '';
        document.getElementById('imgShare2').src = '';
        document.getElementById('imgSuperimposed').src = '';

        modal.show();

        const res = await fetch(`${API_BASE}/files/${fileId}/qr-vc`, {
            headers: getAuthHeaders()
        });

        if (!res.ok) {
            throw new Error('Failed to load QR & VC details.');
        }

        const data = await res.json();
        const formatSrc = (str) => {
            if (!str) return '';
            return str.startsWith('data:') ? str : 'data:image/png;base64,' + str;
        };

        document.getElementById('modalBitLength').innerText = (data.bitStreamLength ? data.bitStreamLength.toLocaleString() : '65,536') + ' bits';
        document.getElementById('modalEntropy').innerText = (data.shannonEntropy != null ? data.shannonEntropy.toFixed(4) : '1.0000') + ' (Near-Maximal)';
        document.getElementById('imgOriginalQR').src = formatSrc(data.qrCodeBase64);
        document.getElementById('imgShare1').src = formatSrc(data.vcShare1Base64);
        document.getElementById('imgShare2').src = formatSrc(data.vcShare2Base64);
        document.getElementById('imgSuperimposed').src = formatSrc(data.superimposedQRBase64);
        document.getElementById('modalBitStreamPreview').innerText = data.bitStreamPreview || '01011001010101...';
    } catch (err) {
        showAlert('Error loading QR/VC shares: ' + err.message, 'danger');
    }
}

async function viewBenchmark(fileId, filename) {
    try {
        const modalEl = document.getElementById('benchmarkModal');
        const modal = new bootstrap.Modal(modalEl);
        const tbody = document.getElementById('benchmarkTableBody');

        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4"><span class="spinner-border spinner-border-sm me-2"></span> Running benchmark across 6 lossless algorithms...</td></tr>';
        document.getElementById('benchmarkConclusionText').innerText = `Running lossless transformation benchmark on bit stream for "${filename}"...`;

        modal.show();

        const res = await fetch(`${API_BASE}/files/${fileId}/benchmark`, {
            headers: getAuthHeaders()
        });

        if (!res.ok) {
            throw new Error('Failed to run lossless benchmark.');
        }

        const data = await res.json();

        if (!data.results || data.results.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No metrics available.</td></tr>';
            return;
        }

        tbody.innerHTML = data.results.map(m => `
            <tr>
                <td class="fw-bold text-white">${escapeHtml(m.algorithmName)}</td>
                <td><span class="badge ${getCategoryBadge(m.algorithmCategory)}">${escapeHtml(m.algorithmCategory)}</span></td>
                <td class="text-light">${m.originalSizeChars ? m.originalSizeChars.toLocaleString() : '-'}</td>
                <td class="text-light">${m.compressedSizeChars ? m.compressedSizeChars.toLocaleString() : '-'}</td>
                <td class="${m.compressionRatioPercent > 100 ? 'text-danger' : 'text-success'} fw-semibold">${m.compressionRatioPercent.toFixed(2)}%</td>
                <td class="${m.spaceSavingsPercent < 0 ? 'text-danger' : 'text-success'} fw-semibold">${m.spaceSavingsPercent.toFixed(2)}%</td>
                <td class="small text-secondary">${m.compressionTimeMs} ms / ${m.decompressionTimeMs} ms</td>
                <td>
                    ${m.losslessFidelity ? '<span class="badge bg-success"><i class="fa-solid fa-check me-1"></i> 100% Lossless</span>' : '<span class="badge bg-danger">Mismatch</span>'}
                </td>
            </tr>
        `).join('');

        document.getElementById('benchmarkConclusionText').innerHTML = `
            <strong>Optimal Transformation Identified:</strong> <u>${escapeHtml(data.optimalAlgorithm)}</u> — ${escapeHtml(data.conclusion)}
        `;
    } catch (err) {
        showAlert('Benchmark execution error: ' + err.message, 'danger');
    }
}

function getCategoryBadge(cat) {
    if (!cat) return 'bg-secondary';
    if (cat.includes('Encoding') || cat.includes('Base64')) return 'bg-primary';
    if (cat.includes('Statistical') || cat.includes('Huffman')) return 'bg-info text-dark';
    if (cat.includes('Dictionary') || cat.includes('LZW')) return 'bg-warning text-dark';
    if (cat.includes('Compound') || cat.includes('Pipeline')) return 'bg-secondary';
    return 'bg-secondary';
}

// ----------------- ADMIN FUNCTIONS -----------------

async function loadAdminData() {
    try {
        // Load Stats
        const statsRes = await fetch(`${API_BASE}/admin/stats`, { headers: getAuthHeaders() });
        if (statsRes.ok) {
            const stats = await statsRes.json();
            document.getElementById('statTotalUsers').innerText = stats.totalUsers;
            document.getElementById('statTotalFiles').innerText = stats.totalFiles;
            document.getElementById('statPendingUsers').innerText = stats.pendingUsers;
            document.getElementById('statPendingKeys').innerText = stats.pendingKeyRequests;
        }

        // Load Pending Users
        const usersRes = await fetch(`${API_BASE}/admin/pending-users`, { headers: getAuthHeaders() });
        const usersTbody = document.getElementById('pendingUsersTableBody');
        if (usersRes.ok) {
            const users = await usersRes.json();
            if (users.length === 0) {
                usersTbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No pending user registrations.</td></tr>';
            } else {
                usersTbody.innerHTML = users.map(u => `
                    <tr>
                        <td class="fw-semibold text-white">${escapeHtml(u.name)}</td>
                        <td class="text-light">${escapeHtml(u.email)}</td>
                        <td><span class="badge bg-secondary">${u.role.replace('ROLE_', '')}</span></td>
                        <td><button class="btn btn-sm btn-primary-custom" onclick="approveUser(${u.id})">Approve</button></td>
                    </tr>
                `).join('');
            }
        }

        // Load Pending Keys
        const keysRes = await fetch(`${API_BASE}/admin/pending-keys`, { headers: getAuthHeaders() });
        const keysTbody = document.getElementById('pendingKeysTableBody');
        if (keysRes.ok) {
            const keys = await keysRes.json();
            if (keys.length === 0) {
                keysTbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No pending key requests.</td></tr>';
            } else {
                keysTbody.innerHTML = keys.map(k => `
                    <tr>
                        <td class="fw-semibold text-white">${escapeHtml(k.filename)}</td>
                        <td class="text-light">${escapeHtml(k.consumerEmail)}</td>
                        <td class="small text-info">${escapeHtml(k.accessReason || 'Standard access')}</td>
                        <td class="text-secondary small">${new Date(k.requestedAt).toLocaleTimeString()}</td>
                        <td><button class="btn btn-sm btn-success" onclick="approveKey(${k.requestId})">Issue Key</button></td>
                    </tr>
                `).join('');
            }
        }
    } catch (err) {
        showAlert('Error loading admin data: ' + err.message, 'danger');
    }
}

async function approveUser(userId) {
    try {
        const res = await fetch(`${API_BASE}/admin/approve-user/${userId}`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        const data = await res.json();
        showAlert(data.message, 'success');
        loadAdminData();
    } catch (err) {
        showAlert('Approval error: ' + err.message, 'danger');
    }
}

async function approveKey(requestId) {
    try {
        const res = await fetch(`${API_BASE}/admin/approve-key/${requestId}`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        const data = await res.json();
        showAlert(data.message, 'success');
        loadAdminData();
    } catch (err) {
        showAlert('Key approval error: ' + err.message, 'danger');
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/&/g, "&amp;")
               .replace(/</g, "&lt;")
               .replace(/>/g, "&gt;")
               .replace(/"/g, "&quot;")
               .replace(/'/g, "&#039;");
}
