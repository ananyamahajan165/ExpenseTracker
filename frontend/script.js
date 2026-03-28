const BASE_URL = "http://localhost:9080";

let editIndex = -1; 

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

    fetch(BASE_URL + "/getExpenses")
    .then(res => res.text())
    .then(data => {

        if (!data) {
            document.getElementById("expenseTable").innerHTML =
                `<tr><td colspan="5">No expenses yet</td></tr>`;
            document.getElementById("totalAmount").innerText = 0;
            return;
        }

        const rows = data.trim().split("\n");

        let html = "";
        let total = 0;

        const sortOption = document.getElementById("sortOption")?.value || "NONE";
        const filterCategory = document.getElementById("filterCategory")?.value || "ALL";
        const searchText = document.getElementById("searchText")?.value.toLowerCase() || "";

        let expenseList = [];

        // 👉 Convert raw data into objects
        for (let i = 0; i < rows.length; i++) {

    if (rows[i].trim() === "") continue;

    const parts = rows[i].split("|");

    const amount = parseFloat(parts[0]);
    const category = parts[1];
    const description = parts[2];
    const date = parts[3];
    const timestamp = parts[4]; 
    expenseList.push({           
        amount,
        category,
        description,
        date,
        timestamp
    });
}
        // 👉 FILTER
        expenseList = expenseList.filter(exp => {

            const matchCategory =
                filterCategory === "ALL" || exp.category === filterCategory;

            const matchSearch =
                exp.description.toLowerCase().includes(searchText);

            return matchCategory && matchSearch;
        });

        // 👉 SORT
        if (sortOption === "HIGH") {
            expenseList.sort((a, b) => b.amount - a.amount);
        } else if (sortOption === "LOW") {
            expenseList.sort((a, b) => a.amount - b.amount);
        } else if (sortOption === "NEW") {
            expenseList.reverse();
        }

        // 👉 BUILD TABLE
        for (let i = 0; i < expenseList.length; i++) {

            const exp = expenseList[i];

            total += exp.amount;

            html += `
                <tr>
                    <td>₹ ${exp.amount}</td>
                    <td>${exp.category}</td>
                    <td>${exp.date}</td>
                    <td>${exp.description}</td>
                    <td>
                    <button onclick="deleteExpense('${exp.timestamp}')">Delete</button>   
                    </td>
                </tr>
            `;
        }

        // 👉 SHOW DATA OR EMPTY MESSAGE
        if (html === "") {
            document.getElementById("expenseTable").innerHTML =
                `<tr><td colspan="5">No expenses yet</td></tr>`;
        } else {
            document.getElementById("expenseTable").innerHTML = html;
        }

        document.getElementById("totalAmount").innerText = total;
    })
    .catch(err => {
        console.error(err);
    });
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



function deleteExpense(timestamp) {

    fetch(BASE_URL + "/deleteExpense/" + timestamp, {
        method: "DELETE"
    })
    .then(res => res.json())
    .then(data => {
        showToast("Deleted successfully");
        loadExpenses();
    })
    .catch(err => console.error(err));
}





function exportCSV() {

    fetch(BASE_URL + "/getExpenses")
    .then(res => res.text())
    .then(data => {

        if (!data) {
            alert("No data to export");
            return;
        }

        const rows = data.trim().split("\n");

        let csv = "Amount,Category,Description\n";

        for (let i = 0; i < rows.length; i++) {
            const parts = rows[i].split("|");

            csv += `${parts[0]},${parts[1]},${parts[2]}\n`;
        }

        const blob = new Blob([csv], { type: "text/csv" });
        const url = window.URL.createObjectURL(blob);

        const a = document.createElement("a");
        a.href = url;
        a.download = "expenses.csv";
        a.click();
    });
}