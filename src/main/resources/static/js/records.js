document.addEventListener("DOMContentLoaded", async () => {

    const user = JSON.parse(localStorage.getItem("loggedInUser"));

    // ===== LOGIN CHECK =====
    if (!user) {
        window.location.href = "/login";
        return;
    }

    // ===== PROFILE =====
    const profileName = document.getElementById("profileName");
    if (profileName) {
        profileName.innerText = "👤 " + user.username;
    }

    try {

        let response;

        // ===== ROLE-BASED FETCH =====
        if (user.role === "ADMIN") {
            response = await fetch("/api/achievements/all");
        } else {
            response = await fetch(`/api/achievements/${user.username}`);
        }

        const data = await response.json();

        const tableBody = document.querySelector("#recordsTable tbody");
        tableBody.innerHTML = "";

        // ===== LOOP =====
        data.forEach(item => {

            const row = document.createElement("tr");

            // highlight current user
            if (item.username === user.username) {
                row.style.background = "#e3f2fd";
            }

            row.innerHTML = `
                <td>${item.title || ""}</td>
                <td>${item.category || ""}</td>
                <td>${item.year || ""}</td>
                <td>${item.semester || ""}</td>
                <td>${item.status || "-"}</td>
                <td>${item.score || 0}</td>
                <td>
                    ${item.fileName
                        ? `<a href="/uploads/${item.fileName}" target="_blank">View</a>`
                        : "No File"}
                </td>
            `;

            tableBody.appendChild(row);
        });

        // ===== COUNTS =====
        document.getElementById("totalCount").innerText = data.length;

        let winner = 0, runner = 0, participation = 0;

        data.forEach(item => {
            const s = (item.status || "").toLowerCase();

            if (s.includes("winner")) winner++;
            else if (s.includes("runner")) runner++;
            else participation++;
        });

        document.getElementById("winnerCount").innerText = winner;
        document.getElementById("runnerCount").innerText = runner;
        document.getElementById("participationCount").innerText = participation;

        // ===== CHARTS =====
        createBarChart("categoryChart", groupCount(data, "category"));
        createBarChart("yearChart", groupCount(data, "year"));
        createBarChart("semesterChart", groupCount(data, "semester"));
        createPieChart("statusChart", groupCount(data, "status"));

    } catch (err) {
        console.error("ERROR:", err);
    }

});


// ===== GROUP HELPER =====
function groupCount(data, field) {
    const map = {};

    data.forEach(item => {
        const key = (item[field] || "unknown").toLowerCase();
        map[key] = (map[key] || 0) + 1;
    });

    return map;
}


// ===== SEARCH =====
function searchTable() {
    const input = document.getElementById("search").value.toLowerCase();
    const rows = document.querySelectorAll("#recordsTable tbody tr");

    rows.forEach(row => {
        row.style.display = row.innerText.toLowerCase().includes(input) ? "" : "none";
    });
}


// ===== DELETE =====
async function deleteRecord(id) {
    if (!confirm("Delete this record?")) return;

    await fetch(`/api/achievements/delete/${id}`, { method: "DELETE" });
    location.reload();
}


// ===== BAR CHART =====
function createBarChart(canvasId, dataObj) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    new Chart(canvas, {
        type: "bar",
        data: {
            labels: Object.keys(dataObj),
            datasets: [{
                data: Object.values(dataObj)
            }]
        }
    });
}


// ===== PIE CHART =====
function createPieChart(canvasId, dataObj) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    new Chart(canvas, {
        type: "pie",
        data: {
            labels: Object.keys(dataObj),
            datasets: [{
                data: Object.values(dataObj)
            }]
        }
    });
}