// Get filter elements
const yearFilter = document.getElementById("year");
const semFilter = document.getElementById("sem");
const categoryFilter = document.getElementById("category");

// Get all table rows (except header)
const rows = document.querySelectorAll("table tr[data-year]");

// Function to filter table
function filterTable() {
    const selectedYear = yearFilter.value.toLowerCase();
    const selectedSem = semFilter.value.toLowerCase().replace(" ", "");
    const selectedCategory = categoryFilter.value.toLowerCase();

    rows.forEach(row => {
        const rowYear = row.getAttribute("data-year");
        const rowSem = row.getAttribute("data-sem");
        const rowCategory = row.getAttribute("data-category");

        let show = true;

        // Check Year
        if (selectedYear !== "year" && selectedYear !== rowYear) {
            show = false;
        }

        // Check Semester
        if (selectedSem !== "semester" && selectedSem !== rowSem) {
            show = false;
        }

        // Check Category
        if (selectedCategory !== "category" && selectedCategory !== rowCategory) {
            show = false;
        }

        // Show or hide row
        row.style.display = show ? "" : "none";
    });
}

// Add event listeners
yearFilter.addEventListener("change", filterTable);
semFilter.addEventListener("change", filterTable);
categoryFilter.addEventListener("change", filterTable);