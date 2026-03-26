const BASE_URL = "https://your-render-url.onrender.com";

// ================= ADD EXPENSE =================
function addExpense() {

    const amount = document.getElementById("amount").value;
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value;

    const bodyData = editIndex === -1
        ? amount + "," + category + "," + description
        : editIndex + "," + amount + "," + category + "," + description;


    const url = editIndex === -1 ? "/addExpense" : "/updateExpense";

    if (!amount || !category) {
    alert("Please fill all required fields");
    return;
}
    fetch(BASE_URL + url, {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: bodyData
    })
    .then(res => res.text())
    .then(data => {
        showMessage("Expense saved successfully");

        document.getElementById("amount").value = "";
        document.getElementById("category").value = "";
        document.getElementById("description").value = "";

        editIndex = -1; 

        loadExpenses();
        getSummary();
    });
}


// ================= LOAD EXPENSES =================
function loadExpenses() {
    if (html === "") {
    document.getElementById("expenseTable").innerHTML =
        `<tr><td colspan="5">No expenses yet</td></tr>`;
} else {
    document.getElementById("expenseTable").innerHTML = html;
}

document.getElementById("totalAmount").innerText = total;


    fetch(BASE_URL + "/getExpenses")
    .then(res => res.text())
    .then(data => {

        // ✅ NOW this is inside correctly
        const rows = data.trim().split("\n");

        let expenseList = [];

        for (let i = 0; i < rows.length; i++) {

            if (rows[i].trim() === "") continue;

            const parts = rows[i].split(",");

            expenseList.push({
                index: i,
                amount: parseInt(parts[0]),
                category: parts[1],
                description: parts[2] || ""
            });
        }

        // ✅ SORTING HERE
        const sortOption = document.getElementById("sortOption")?.value || "NONE";

        if (sortOption === "HIGH") {
            expenseList.sort((a, b) => b.amount - a.amount);
        }
        else if (sortOption === "LOW") {
            expenseList.sort((a, b) => a.amount - b.amount);
        }
        else if (sortOption === "NEW") {
            expenseList.reverse();
        }

        // ✅ BUILD TABLE
        let html = "";
        let total = 0;

        for (let i = 0; i < expenseList.length; i++) {

            const exp = expenseList[i];

            total += exp.amount;

            html += `<tr>
                <td>${exp.amount}</td>
                <td>${exp.category}</td>
                <td>-</td>
                <td>${exp.description}</td>
                <td>
                    <button onclick="editExpense(${exp.index}, '${exp.amount}', '${exp.category}', '${exp.description}')">Edit</button>
                    <button onclick="deleteExpense(${exp.index})">Delete</button>
                </td>
            </tr>`;
        }

        if (html === "") {
    document.getElementById("expenseTable").innerHTML =
        `<tr><td colspan="5">No expenses found</td></tr>`;
} else {
    document.getElementById("expenseTable").innerHTML = html;
}
        document.getElementById("totalAmount").innerText = total;

    })
    .catch(err => console.error(err));
}


// ================= DELETE =================
function deleteExpense(index) {

    let editIndex = -1;

function editExpense(index, amount, category, description) {

    document.getElementById("amount").value = amount;
    document.getElementById("category").value = category;
    document.getElementById("description").value = description;

    editIndex = index;
}

if (!confirm("Are you sure you want to delete this expense?")) {
    return;
}
    fetch(BASE_URL + "/deleteExpense", {
        method: "POST",
        body: index.toString()
    })
    .then(res => res.text())
    .then(data => {
        alert(data);
        loadExpenses();
        getSummary();
    });
}


// ================= LOGIN =================
function loginUser() {

    const username = document.getElementById("loginUsername").value;
    const password = document.getElementById("loginPassword").value;

    const bodyData = username + "|" + password;

    fetch(BASE_URL + "/login", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: bodyData
    })
    .then(res => res.text())
    .then(data => {

        if (data === "SUCCESS") {
            localStorage.setItem("username", username);
            alert("Login successful");
            showDashboard();
        } else {
            alert("Invalid credentials");
        }
    });
}


// ================= REGISTER =================
function registerUser() {

    const username = document.getElementById("registerUsername").value;
    const password = document.getElementById("registerPassword").value;

    const bodyData = username + "|" + password;

    fetch(BASE_URL + "/register", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: bodyData
    })
    .then(res => res.text())
    .then(data => alert(data));
}


// ================= DASHBOARD =================
function showDashboard() {

    document.getElementById("loginSection").style.display = "none";
    document.getElementById("dashboardSection").style.display = "block";

    const user = localStorage.getItem("username");
    document.getElementById("welcomeUser").innerText = "Logged in as: " + user;

    loadExpenses();
    getSummary();
}


// ================= LOGOUT =================
function logoutUser() {

    fetch(BASE_URL + "/logout", { method: "POST" });

    localStorage.removeItem("username");

    document.getElementById("dashboardSection").style.display = "none";
    document.getElementById("loginSection").style.display = "block";
}


// ================= SUMMARY =================
function getSummary() {

    fetch(BASE_URL + "/summary")
    .then(res => res.json())
    .then(data => {

        const total = data.total;
        const budget = data.budget;

        document.getElementById("totalAmount").innerText = total;
        document.getElementById("budgetAmount").innerText = budget;

        const remaining = budget - total;
        document.getElementById("remainingAmount").innerText = remaining;

        const warning = document.getElementById("budgetWarning");

        if (remaining < 0) {
            warning.innerText = "⚠ Budget Exceeded!";
            warning.style.color = "red";
        } else if (remaining < budget * 0.1) {
            warning.innerText = "⚠ Only 10% budget left!";
            warning.style.color = "orange";
        } else {
            warning.innerText = "Within Budget";
            warning.style.color = "green";
        }

        renderChart(data);
    });
}


// ================= CHART =================
let chart;

function renderChart(data) {

    const ctx = document.getElementById("expenseChart").getContext("2d");

    if (chart) {
        chart.destroy();
    }

    chart = new Chart(ctx, {
        type: "pie",
        data: {
            labels: Object.keys(data.byCategory),
            datasets: [{
                data: Object.values(data.byCategory)
            }]
        }
    });
}


function showMessage(msg) {
    const div = document.createElement("div");
    div.innerText = msg;
    div.style.background = "green";
    div.style.color = "white";
    div.style.padding = "10px";
    div.style.margin = "10px";
    document.body.appendChild(div);

    setTimeout(() => div.remove(), 2000);
}

// ================= SET BUDGET =================
function setBudget() {

    const budget = document.getElementById("budgetInput").value;

    fetch(BASE_URL + "/set-budget", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: budget
    })
    .then(res => res.text())
    .then(() => {
        alert("Budget updated");
        getSummary();
    });
}


// ================= TOGGLE =================
function showRegister(){
    document.getElementById("registerForm").style.display = "block";
}

function showLogin(){
    document.getElementById("registerForm").style.display = "none";
}


// ================= AUTO LOGIN =================
window.onload = function () {
    const user = localStorage.getItem("username");
    if (user) {
        showDashboard();
    }
};


function showToast(msg) {
    const div = document.createElement("div");
    div.innerText = msg;
    div.style.position = "fixed";
    div.style.bottom = "20px";
    div.style.right = "20px";
    div.style.background = "#22c55e";
    div.style.color = "white";
    div.style.padding = "10px";
    document.body.appendChild(div);

    setTimeout(() => div.remove(), 2000);
}