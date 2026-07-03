document.addEventListener("DOMContentLoaded", () => {

    const loginBtn = document.getElementById("loginBtn");
    const signupBtn = document.getElementById("signupBtn");

    const loginForm = document.getElementById("loginForm");
    const signupForm = document.getElementById("signupForm");

    const loginError = document.getElementById("loginError");
    const signupError = document.getElementById("signupError");

    // ===== TOGGLE =====
    loginBtn.addEventListener("click", () => {
        loginForm.classList.add("active");
        signupForm.classList.remove("active");
        loginBtn.classList.add("active");
        signupBtn.classList.remove("active");
    });

    signupBtn.addEventListener("click", () => {
        signupForm.classList.add("active");
        loginForm.classList.remove("active");
        signupBtn.classList.add("active");
        loginBtn.classList.remove("active");
    });

    // ===== SIGNUP =====
    signupForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        const username = document.getElementById("signupUsername").value.toLowerCase();
        const password = document.getElementById("signupPassword").value;
        const confirmPassword = document.getElementById("confirmPassword").value;
        const roleElement = document.getElementById("signupRole");

        if (!roleElement.value) {
            signupError.textContent = "Please select role!";
            return;
        }

        const role = roleElement.value.toUpperCase();

        if (password !== confirmPassword) {
            signupError.textContent = "Passwords do not match!";
            return;
        }

        try {
            const response = await fetch("/api/signup", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password, role })
            });

            if (!response.ok) {
                signupError.textContent = "Signup failed!";
                return;
            }

            alert("Signup successful! Please login.");
            signupForm.reset();
            loginBtn.click();

        } catch (err) {
            console.error(err);
            signupError.textContent = "Server error!";
        }
    });

    // ===== LOGIN =====
    loginForm.addEventListener("submit", async function (e) {
        e.preventDefault();

        const username = document.getElementById("loginUsername").value.toLowerCase();
        const password = document.getElementById("loginPassword").value;
        const roleElement = document.getElementById("loginRole");

        if (!roleElement.value) {
            loginError.textContent = "Please select role!";
            return;
        }

        const role = roleElement.value.toUpperCase();

        try {
            const response = await fetch("/api/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password, role })
            });

            if (!response.ok) {
                loginError.textContent = "Invalid credentials!";
                return;
            }

            const data = await response.json();

            // ✅ SAVE SESSION
            localStorage.setItem("loggedInUser", JSON.stringify({
                username: data.username,
                role: data.role
            }));

            // ✅ SINGLE REDIRECT
            window.location.href = "/dashboard";

        } catch (err) {
            console.error(err);
            loginError.textContent = "Server error!";
        }
    });

});