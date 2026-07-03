// ===== LOGIN CHECK =====
const user = JSON.parse(localStorage.getItem("loggedInUser"));

if (!user) {
    window.location.href = "/login";
    throw new Error("User not logged in");
}


// ===== SIDEBAR =====
const sidebarTitle = document.querySelector(".sidebar h2");
const dashboardLink = document.querySelector(".sidebar a");

if (user.role === "ADMIN") {
    if (sidebarTitle) sidebarTitle.innerText = "Admin Panel";
    if (dashboardLink) dashboardLink.innerText = "🏠 Admin Dashboard";
} else {
    if (sidebarTitle) sidebarTitle.innerText = "Student Panel";
    if (dashboardLink) dashboardLink.innerText = "🏠 Dashboard";
}


// ===== PROFILE =====
const profileName = document.getElementById("profileName");
if (profileName) {
    profileName.innerText = "👤 " + user.username;
}


// ===== HIDE STUDENT FEATURES FOR ADMIN =====
if (user.role === "ADMIN") {
    document.getElementById("studentCards")?.style.setProperty("display", "none");
    document.getElementById("aiCard")?.style.setProperty("display", "none");
}


// ===== LOAD TABLE =====
async function loadAchievements() {
    try {
        const response = await fetch("/api/achievements/all");

        if (!response.ok) {
            console.error("Failed to fetch achievements");
            return;
        }

        const data = await response.json();

        const tableBody = document.getElementById("achievementTableBody");
        if (!tableBody) return;

        tableBody.innerHTML = "";

        let rowsHTML = "";

        data.forEach(a => {

            const highlight =
                a.username === user.username
                    ? "style='background:#e3f2fd;font-weight:500;'"
                    : "";

            // ===== ROLE-BASED ACTION BUTTON =====
            let actionBtn = "";

            if (user.role === "ADMIN") {
                actionBtn = `<button onclick="deleteRecord(${a.id})" class="btn-action btn-delete">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2"/></svg>
                                Delete
                             </button>`;
            } else if (a.username === user.username) {
                actionBtn = `<button onclick="openEditModal(${a.id})" class="btn-action btn-update">
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                                Edit
                             </button>`;
            }

            rowsHTML += `
            <tr ${highlight}
                data-year="${a.year || ""}"
                data-sem="${a.semester || ""}"
                data-category="${(a.category || "").toLowerCase()}">

                <td>${a.username}</td>
                <td>${a.title}</td>
                <td>${a.category}</td>
                <td>${a.year}</td>
                <td>${a.semester}</td>
                <td>${a.status}</td>
                <td>${a.score}</td>

                <td>${actionBtn}</td>

                <td>
                    ${a.fileName
                        ? `<a href="/uploads/${a.fileName}" target="_blank" class="btn-action btn-view">View</a>`
                        : `<span class="no-file">No File</span>`}
                </td>

                <td>
                    <a href="/api/achievements/report/${a.username}" target="_blank" class="btn-action btn-pdf">PDF</a>
                </td>

            </tr>
            `;
        });

        tableBody.innerHTML = rowsHTML;

    } catch (err) {
        console.error("LOAD ERROR:", err);
    }
}

loadAchievements();


// ===== DELETE (ADMIN ONLY) =====
async function deleteRecord(id) {

    if (!confirm("Are you sure you want to delete this record?")) return;

    try {
        const response = await fetch(`/api/achievements/delete/${id}`, {
            method: "DELETE"
        });

        if (!response.ok) {
            alert("Delete failed!");
            return;
        }

        alert("Deleted successfully!");
        location.reload();

    } catch (err) {
        console.error("DELETE ERROR:", err);
        alert("Server error!");
    }
}


// ===== OPEN EDIT MODAL (STUDENT ONLY) =====
async function openEditModal(id) {

    try {
        // Fetch the existing record
        const res = await fetch(`/api/achievements/id/${id}`);

        if (!res.ok) {
            alert("Could not load record.");
            return;
        }

        const data = await res.json();

        // Pre-fill modal fields
        document.getElementById("editId").value        = id;
        document.getElementById("editTitle").value     = data.title      || "";
        document.getElementById("editCategory").value  = data.category   || "";
        document.getElementById("editStatus").value    = data.status     || "";
        document.getElementById("editYear").value      = data.year       || "";
        document.getElementById("editSemester").value  = data.semester   || "";
        document.getElementById("editEventLevel").value= data.eventLevel || "";
        document.getElementById("editSkills").value    = data.skills     || "";

        // Show existing file name
        const fileInfo = document.getElementById("editFileInfo");
        fileInfo.textContent = data.fileName
            ? "Current file: " + data.fileName
            : "No file uploaded";

        // Show modal
        document.getElementById("editModal").classList.add("modal--open");
        document.body.style.overflow = "hidden";

    } catch (err) {
        console.error("MODAL ERROR:", err);
        alert("Failed to load record.");
    }
}


// ===== CLOSE EDIT MODAL =====
function closeEditModal() {
    document.getElementById("editModal").classList.remove("modal--open");
    document.body.style.overflow = "";
    document.getElementById("editForm").reset();
}

// Close modal when clicking backdrop
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("editModal");
    if (modal) {
        modal.addEventListener("click", function(e) {
            if (e.target === this) closeEditModal();
        });
    }
});


// ===== SAVE EDIT (PUT request) =====
async function saveEdit(e) {
    e.preventDefault();

    const id         = document.getElementById("editId").value;
    const title      = document.getElementById("editTitle").value.trim();
    const category   = document.getElementById("editCategory").value;
    const status     = document.getElementById("editStatus").value;
    const year       = document.getElementById("editYear").value.trim();
    const semester   = document.getElementById("editSemester").value.trim();
    const eventLevel = document.getElementById("editEventLevel").value;
    const skills     = document.getElementById("editSkills").value.trim();
    const file       = document.getElementById("editFile").files[0];

    if (!title || !category || !status || !year || !semester) {
        alert("Please fill all required fields.");
        return;
    }

    const saveBtn = document.getElementById("editSaveBtn");
    saveBtn.textContent = "Saving...";
    saveBtn.disabled = true;

    const formData = new FormData();
    formData.append("title",      title);
    formData.append("category",   category);
    formData.append("status",     status);
    formData.append("year",       year);
    formData.append("semester",   semester);
    formData.append("eventLevel", eventLevel);
    formData.append("skills",     skills);
    if (file) formData.append("file", file);

    try {
        const response = await fetch(`/api/achievements/update/${id}`, {
            method: "PUT",
            body: formData
        });

        if (response.ok) {
            closeEditModal();
            await loadAchievements();  // refresh table without full page reload

            // also refresh score/rank cards
            if (user.role !== "ADMIN") {
                loadPerformance();
                loadRanking();
            }
        } else {
            const msg = await response.text();
            alert("Update failed: " + msg);
        }

    } catch (err) {
        console.error("SAVE ERROR:", err);
        alert("Server error!");
    } finally {
        saveBtn.textContent = "Save Changes";
        saveBtn.disabled = false;
    }
}


// ===== FILTER =====
const yearFilter     = document.getElementById("year");
const semFilter      = document.getElementById("sem");
const categoryFilter = document.getElementById("category");

function filterTable() {

    const selectedYear     = yearFilter.value;
    const selectedSem      = semFilter.value;
    const selectedCategory = categoryFilter.value.toLowerCase();

    const rows = document.querySelectorAll("#achievementTableBody tr");

    rows.forEach(row => {

        const rowYear     = row.getAttribute("data-year");
        const rowSem      = row.getAttribute("data-sem")?.replace("sem", "");
        const rowCategory = row.getAttribute("data-category");

        let show = true;

        if (selectedYear !== "Year" && rowYear != selectedYear) show = false;
        if (selectedSem !== "Semester" && rowSem != selectedSem) show = false;
        if (selectedCategory !== "category" && rowCategory !== selectedCategory) show = false;

        row.style.display = show ? "" : "none";
    });
}

if (yearFilter && semFilter && categoryFilter) {
    yearFilter.addEventListener("change", filterTable);
    semFilter.addEventListener("change", filterTable);
    categoryFilter.addEventListener("change", filterTable);
}


// ===== STUDENT FEATURES =====
if (user.role !== "ADMIN") {
    loadPerformance();
    loadRanking();
    loadRecommendation();
    loadAiReport();
}


// ===== PERFORMANCE =====
async function loadPerformance() {
    try {
        const res = await fetch(`/api/achievements/performance/${user.username}`);
        const data = await res.json();
        document.getElementById("score").innerText = data.score || 0;
    } catch (err) {
        console.error("PERFORMANCE ERROR:", err);
    }
}


// ===== RANK =====
async function loadRanking() {
    try {
        const res = await fetch(`/api/achievements/ranking/${user.username}`);
        const data = await res.json();
        document.getElementById("rank").innerText = `${data.rank}/${data.totalUsers}`;
    } catch (err) {
        console.error("RANK ERROR:", err);
    }
}


// ===== RECOMMENDATION =====
async function loadRecommendation() {
    try {
        const res = await fetch(`/api/achievements/recommendation/${user.username}`);
        const data = await res.json();
        document.getElementById("suggestion").innerText = data.suggestion || "-";
    } catch (err) {
        console.error("RECOMMENDATION ERROR:", err);
    }
}


// ===== AI REPORT =====
async function loadAiReport() {
    try {
        const res = await fetch(`/api/achievements/ai-report/${user.username}`);
        const data = await res.json();
        document.getElementById("aiReport").innerText = data.report || "No AI report available.";
    } catch (err) {
        console.error("AI ERROR:", err);
        document.getElementById("aiReport").innerText = "AI analysis failed.";
    }
}


// ===== LOGOUT =====
function logout() {
    localStorage.removeItem("loggedInUser");
    window.location.href = "/login";
}


// ===== EXPORT — sends active filter params so backend exports only filtered data =====
function downloadExcel() {
    const year     = document.getElementById("year").value;
    const sem      = document.getElementById("sem").value;
    const category = document.getElementById("category").value;

    const params = new URLSearchParams();

    // Only add param if a real filter is selected (not the placeholder default)
    if (year     && year     !== "Year")     params.append("year",     year);
    if (sem      && sem      !== "Semester") params.append("semester", sem);
    if (category && category !== "Category") params.append("category", category);

    window.location.href = "/api/achievements/export?" + params.toString();
}