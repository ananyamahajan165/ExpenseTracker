const BASE_URL = "http://localhost:9080";

function getSelectedMonth() {
    const month = document.getElementById("monthSelector")?.value;
    return month || new Date().toISOString().slice(0, 7);
}

function setDefaultDates() {
    const today = new Date().toISOString().slice(0, 10);
    const currentMonth = today.slice(0, 7);

    const monthSelector = document.getElementById("monthSelector");
    const expenseDate = document.getElementById("expenseDate");

    if (monthSelector && !monthSelector.value) {
        monthSelector.value = currentMonth;
    }

    if (expenseDate && !expenseDate.value) {
        expenseDate.value = today;
    }
}

function getAuthToken() {
    return localStorage.getItem("authToken");
}

function getStoredUsername() {
    return localStorage.getItem("username");
}

function authHeaders(extraHeaders = {}) {
    const token = getAuthToken();
    const headers = { ...extraHeaders };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    return headers;
}

async function parseResponse(response) {
    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const message = typeof payload === "string"
            ? payload
            : payload.message || "Something went wrong";
        throw new Error(message);
    }

    return payload;
}

function resetExpenseForm() {
    document.getElementById("amount").value = "";
    document.getElementById("category").value = "";
    document.getElementById("description").value = "";
    document.getElementById("expenseDate").value = `${getSelectedMonth()}-01`;
}

function clearAuthState() {
    localStorage.removeItem("authToken");
    localStorage.removeItem("username");
}

function showToast(msg, isError = false) {
    const div = document.createElement("div");
    div.innerText = msg;
    div.style.position = "fixed";
    div.style.bottom = "20px";
    div.style.right = "20px";
    div.style.background = isError ? "#dc2626" : "#22c55e";
    div.style.color = "white";
    div.style.padding = "10px 14px";
    div.style.borderRadius = "10px";
    div.style.boxShadow = "0 10px 30px rgba(0,0,0,0.15)";
    div.style.zIndex = "1000";
    document.body.appendChild(div);

    setTimeout(() => div.remove(), 2200);
}

function renderExpenses(expenseList) {
    const expenseTable = document.getElementById("expenseTable");
    const totalAmount = document.getElementById("totalAmount");

    if (!expenseList.length) {
        expenseTable.innerHTML = `<tr><td colspan="5">No expenses yet</td></tr>`;
        totalAmount.innerText = "0";
        return;
    }

    let total = 0;
    let html = "";

    for (const exp of expenseList) {
        total += Number(exp.amount || 0);

        html += `
            <tr>
                <td>₹ ${Number(exp.amount).toFixed(2)}</td>
                <td>${exp.category || ""}</td>
                <td>${exp.date || ""}</td>
                <td>${exp.description || ""}</td>
                <td><button onclick="deleteExpense('${exp.timestamp}')">Delete</button></td>
            </tr>
        `;
    }

    expenseTable.innerHTML = html;
    totalAmount.innerText = total.toFixed(2);
}

async function loadExpenses() {
    try {
        const expenses = await parseResponse(
            await fetch(`${BASE_URL}/expenses?month=${encodeURIComponent(getSelectedMonth())}`, {
                headers: authHeaders()
            })
        );

        const sortOption = document.getElementById("sortOption")?.value || "NONE";
        const filterCategory = document.getElementById("filterCategory")?.value || "ALL";
        const searchText = document.getElementById("searchText")?.value.trim().toLowerCase() || "";

        let expenseList = expenses.filter(exp => {
            const matchesCategory =
                filterCategory === "ALL" || exp.category === filterCategory;
            const description = (exp.description || "").toLowerCase();
            return matchesCategory && description.includes(searchText);
        });

        if (sortOption === "HIGH") {
            expenseList.sort((a, b) => Number(b.amount) - Number(a.amount));
        } else if (sortOption === "LOW") {
            expenseList.sort((a, b) => Number(a.amount) - Number(b.amount));
        } else if (sortOption === "NEW") {
            expenseList.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        } else if (sortOption === "OLD") {
            expenseList.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
        }

        renderExpenses(expenseList);
    } catch (error) {
        if (error.message === "Please login first") {
            clearAuthState();
            showLoginScreen();
        }
        console.error(error);
    }
}

async function addExpense() {
    const amount = document.getElementById("amount").value.trim();
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value.trim();
    const expenseDate = document.getElementById("expenseDate").value;

    if (!amount || !category || !description || !expenseDate) {
        showToast("Please fill all expense fields", true);
        return;
    }

    try {
        await parseResponse(
            await fetch(`${BASE_URL}/add-expense`, {
                method: "POST",
                headers: authHeaders({
                    "Content-Type": "text/plain"
                }),
                body: `${amount}|${category}|${description}|${expenseDate}`
            })
        );

        resetExpenseForm();
        await Promise.all([loadExpenses(), getSummary()]);
        showToast("Expense added successfully");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function deleteExpense(timestamp) {
    if (!confirm("Are you sure you want to delete this expense?")) {
        return;
    }

    try {
        await parseResponse(
            await fetch(`${BASE_URL}/deleteExpense/${encodeURIComponent(timestamp)}`, {
                method: "DELETE",
                headers: authHeaders()
            })
        );

        await Promise.all([loadExpenses(), getSummary()]);
        showToast("Deleted successfully");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function loginUser() {
    const username = document.getElementById("loginUsername").value.trim();
    const password = document.getElementById("loginPassword").value;

    if (!username || !password) {
        showToast("Enter username and password", true);
        return;
    }

    try {
        const data = await parseResponse(
            await fetch(`${BASE_URL}/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "text/plain"
                },
                body: `${username}|${password}`
            })
        );

        localStorage.setItem("username", data.username);
        localStorage.setItem("authToken", data.token);
        document.getElementById("loginPassword").value = "";
        showDashboard();
        showToast(data.message || "Login successful");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function registerUser() {
    const username = document.getElementById("registerUsername").value.trim();
    const password = document.getElementById("registerPassword").value;

    if (!username || !password) {
        showToast("Enter username and password", true);
        return;
    }

    try {
        const data = await parseResponse(
            await fetch(`${BASE_URL}/register`, {
                method: "POST",
                headers: {
                    "Content-Type": "text/plain"
                },
                body: `${username}|${password}`
            })
        );

        localStorage.setItem("username", data.username);
        localStorage.setItem("authToken", data.token);
        document.getElementById("registerPassword").value = "";
        showDashboard();
        showToast(data.message || "Registration successful");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function forgotPassword() {
    const username = document.getElementById("forgotUsername").value.trim();
    const newPassword = document.getElementById("forgotNewPassword").value;

    if (!username || !newPassword) {
        showToast("Enter username and new password", true);
        return;
    }

    try {
        const data = await parseResponse(
            await fetch(`${BASE_URL}/forgot-password`, {
                method: "POST",
                headers: {
                    "Content-Type": "text/plain"
                },
                body: `${username}|${newPassword}`
            })
        );

        document.getElementById("forgotUsername").value = "";
        document.getElementById("forgotNewPassword").value = "";
        showLogin();
        showToast(data.message || "Password reset successful");
    } catch (error) {
        showToast(error.message, true);
    }
}

function showDashboard() {
    document.getElementById("loginSection").style.display = "none";
    document.getElementById("dashboardSection").style.display = "block";

    const user = getStoredUsername();
    document.getElementById("welcomeUser").innerText = user
        ? `Logged in as: ${user}`
        : "";

    loadExpenses();
    getSummary();
}

function showLoginScreen() {
    document.getElementById("dashboardSection").style.display = "none";
    document.getElementById("loginSection").style.display = "block";
    showLogin();
}

async function logoutUser() {
    try {
        await fetch(`${BASE_URL}/logout`, {
            method: "POST",
            headers: authHeaders()
        });
    } catch (error) {
        console.error(error);
    }

    clearAuthState();
    showLoginScreen();
}

async function getSummary() {
    try {
        const data = await parseResponse(
            await fetch(`${BASE_URL}/summary?month=${encodeURIComponent(getSelectedMonth())}`, {
                headers: authHeaders()
            })
        );

        const total = Number(data.total || 0);
        const budget = Number(data.budget || 0);
        const remaining = budget - total;

        document.getElementById("totalAmount").innerText = total.toFixed(2);
        document.getElementById("budgetAmount").innerText = budget.toFixed(2);
        document.getElementById("remainingAmount").innerText = remaining.toFixed(2);

        const warning = document.getElementById("budgetWarning");

        if (budget === 0) {
            warning.innerText = "Set a budget to track your monthly spending";
            warning.style.color = "#2563eb";
        } else if (remaining < 0) {
            warning.innerText = "Budget exceeded";
            warning.style.color = "red";
        } else if (remaining < budget * 0.1) {
            warning.innerText = "Only 10% budget left";
            warning.style.color = "orange";
        } else {
            warning.innerText = "Within budget";
            warning.style.color = "green";
        }

    } catch (error) {
        if (error.message === "Please login first") {
            clearAuthState();
            showLoginScreen();
        }
        console.error(error);
    }
}

async function setBudget() {
    const budget = document.getElementById("budgetInput").value.trim();

    if (!budget) {
        showToast("Enter a budget amount", true);
        return;
    }

    try {
        const data = await parseResponse(
            await fetch(`${BASE_URL}/set-budget`, {
                method: "POST",
                headers: authHeaders({
                    "Content-Type": "text/plain"
                }),
                body: `${getSelectedMonth()}|${budget}`
            })
        );

        await getSummary();
        showToast(data.message || "Budget updated");
    } catch (error) {
        showToast(error.message, true);
    }
}

function showRegister() {
    document.getElementById("registerForm").style.display = "block";
    document.getElementById("forgotPasswordForm").style.display = "none";
}

function showLogin() {
    document.getElementById("registerForm").style.display = "none";
    document.getElementById("forgotPasswordForm").style.display = "none";
}

function showForgotPassword() {
    document.getElementById("registerForm").style.display = "none";
    document.getElementById("forgotPasswordForm").style.display = "block";
}

function handleMonthChange() {
    document.getElementById("expenseDate").value = `${getSelectedMonth()}-01`;
    loadExpenses();
    getSummary();
}

async function exportCSV() {
    try {
        const expenses = await parseResponse(
            await fetch(`${BASE_URL}/expenses?month=${encodeURIComponent(getSelectedMonth())}`, {
                headers: authHeaders()
            })
        );

        if (!expenses.length) {
            showToast("No data to export", true);
            return;
        }

        let csv = "Amount,Category,Date,Description\n";

        for (const expense of expenses) {
            const safeDescription = `"${(expense.description || "").replace(/"/g, '""')}"`;
            csv += `${expense.amount},${expense.category},${expense.date},${safeDescription}\n`;
        }

        const blob = new Blob([csv], { type: "text/csv" });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "expenses.csv";
        a.click();
        window.URL.revokeObjectURL(url);
    } catch (error) {
        showToast(error.message, true);
    }
}

window.onload = function () {
    setDefaultDates();
    if (getAuthToken() && getStoredUsername()) {
        showDashboard();
    } else {
        showLoginScreen();
    }
};
