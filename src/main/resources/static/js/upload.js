// ===== PAGE LOAD =====
document.addEventListener("DOMContentLoaded", async () => {

    const user = JSON.parse(localStorage.getItem("loggedInUser"));

    // LOGIN CHECK
    if (!user) {
        window.location.href = "/login";
        return;
    }

    // SIDEBAR
    const sidebarTitle = document.querySelector(".sidebar h2");
    const dashboardLink = document.querySelector(".sidebar a");

    if (user.role === "ADMIN") {
        if (sidebarTitle) sidebarTitle.innerText = "Admin Panel";
        if (dashboardLink) dashboardLink.innerText = "🏠 Admin Dashboard";
    } else {
        if (sidebarTitle) sidebarTitle.innerText = "Student Panel";
        if (dashboardLink) dashboardLink.innerText = "🏠 Dashboard";
    }

    // USERNAME BLOCK: show for admin, hide for student
    const block = document.getElementById("usernameBlock");
    if (block) {
        block.style.display = user.role === "ADMIN" ? "block" : "none";
    }

    // ===== EDIT MODE — detect ?edit=ID in URL =====
    const urlParams = new URLSearchParams(window.location.search);
    const editId = urlParams.get("edit");

    if (editId) {
        // Store edit ID for use in form submit
        document.getElementById("uploadForm").dataset.editId = editId;

        // Change page heading & button text
        const pageTitle = document.querySelector(".page-title");
        const pageSub   = document.querySelector(".page-subtitle");
        const submitBtn = document.querySelector(".submit-btn");

        if (pageTitle) pageTitle.innerText = "Update Achievement";
        if (pageSub)   pageSub.innerText   = "Edit the details below and save your changes";
        if (submitBtn) {
            submitBtn.innerHTML = `
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                Save Changes`;
        }

        // Make file optional in edit mode (user may keep existing file)
        const pdfInput = document.getElementById("pdfFile");
        if (pdfInput) pdfInput.removeAttribute("required");

        // Update file-drop hint text
        const fileSub = document.querySelector(".file-drop-sub");
        if (fileSub) fileSub.innerText = "Leave empty to keep existing certificate · PDF only";

        // Pre-fill form with existing data
        try {
            const res = await fetch(`/api/achievements/id/${editId}`);

            if (!res.ok) {
                alert("Could not load achievement data.");
                return;
            }

            const data = await res.json();

            // Fill each field
            const set = (id, val) => {
                const el = document.getElementById(id);
                if (el && val !== undefined && val !== null) el.value = val;
            };

            set("title",      data.title);
            set("category",   data.category);
            set("status",     data.status);
            set("year",       data.year);
            set("semester",   data.semester);
            set("eventLevel", data.eventLevel);
            set("skills",     data.skills);

            // Show existing file name
            if (data.fileName) {
                const display = document.getElementById("fileNameDisplay");
                if (display) display.textContent = "Current: " + data.fileName;
            }

        } catch (err) {
            console.error("Pre-fill error:", err);
            alert("Failed to load achievement data.");
        }
    }
});


// ===== FORM SUBMIT =====
document.getElementById("uploadForm").addEventListener("submit", async function(e) {
    e.preventDefault();

    const user = JSON.parse(localStorage.getItem("loggedInUser"));

    if (!user) {
        alert("Login first!");
        window.location.href = "/login";
        return;
    }

    const editId = this.dataset.editId;  // set in edit mode, undefined otherwise

    const title      = document.getElementById("title").value.trim();
    const category   = document.getElementById("category").value;
    const year       = document.getElementById("year").value.trim();
    const semester   = document.getElementById("semester").value.trim();
    const status     = document.getElementById("status").value;
    const eventLevel = document.getElementById("eventLevel").value;
    const skills     = document.getElementById("skills").value;
    const file       = document.getElementById("pdfFile").files[0];

    // USERNAME LOGIC
    let finalUsername;

    if (user.role === "ADMIN") {
        const input = document.getElementById("username");
        if (!input || !input.value.trim()) {
            alert("Please enter student username!");
            return;
        }
        finalUsername = input.value.toLowerCase().trim();
    } else {
        finalUsername = user.username;
    }

    // FILE VALIDATION — only required for new upload, not edit
    if (!editId) {
        if (!file) {
            alert("Please upload PDF!");
            return;
        }
        if (file.type !== "application/pdf") {
            alert("Only PDF files allowed!");
            return;
        }
        if (file.size > 10 * 1024 * 1024) {
            alert("File too large! Max 10MB");
            return;
        }
    } else if (file && file.type !== "application/pdf") {
        alert("Only PDF files allowed!");
        return;
    }

    // BUTTON LOADING
    const submitBtn = document.querySelector(".submit-btn");
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = editId ? "Saving..." : "Uploading...";
    submitBtn.disabled = true;

    const formData = new FormData();
    formData.append("username",   finalUsername);
    formData.append("title",      title);
    formData.append("category",   category);
    formData.append("year",       year);
    formData.append("semester",   semester);
    formData.append("eventLevel", eventLevel);
    formData.append("skills",     skills);
    formData.append("status",     status);

    // Only append file if provided
    if (file) formData.append("file", file);

    try {
        let response;

        if (editId) {
            // ===== UPDATE existing record =====
            response = await fetch(`/api/achievements/update/${editId}`, {
                method: "PUT",
                body: formData
            });
        } else {
            // ===== NEW upload =====
            response = await fetch("/api/achievements/addWithFile", {
                method: "POST",
                body: formData
            });
        }

        if (response.ok) {
            alert(editId ? "Updated successfully!" : "Uploaded successfully!");
            window.location.href = "/dashboard";
        } else {
            const msg = await response.text();
            alert((editId ? "Update" : "Upload") + " failed: " + msg);
        }

    } catch (err) {
        console.error("ERROR:", err);
        alert("Server error!");
    } finally {
        submitBtn.innerHTML = originalText;
        submitBtn.disabled = false;
    }
});