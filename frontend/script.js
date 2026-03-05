const BASE_URL = "http://localhost:8080";
function addExpense() {

    const amount = document.getElementById("amount").value;
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value;

    const bodyData = amount + "|" + category + "|" + description;

    fetch("http://localhost:8080/add-expense", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: bodyData
    })
    .then(res => res.json())
    .then(data => {
        alert(data.message);
        loadExpenses();
        getSummary();
    })
    .catch(err => console.error(err));
}

function loadExpenses() {

    const loading = document.getElementById("loadingMessage");
    const empty = document.getElementById("emptyMessage");
    const tbody = document.querySelector("#expenseTable tbody");

    loading.style.display = "block";
    empty.style.display = "none";
    tbody.innerHTML = "";

    fetch(BASE_URL + "/expenses")
    .then(res => res.json())
    .then(data => {

        loading.style.display = "none";

        if (data.length === 0) {
            empty.style.display = "block";
            return;
        }

        data.forEach(exp => {

            const row = document.createElement("tr");

            row.innerHTML = `
                <td>₹${exp.amount}</td>
                <td>${exp.category}</td>
                <td>${exp.date}</td>
                <td>${exp.description}</td>
                <td>
                    <button class="delete-btn"
                        onclick="deleteExpense('${exp.timestamp}')">
                        Delete
                    </button>
                </td>
            `;

            tbody.appendChild(row);
        });
    })
    .catch(err => {
        loading.style.display = "none";
        console.error("Error loading expenses:", err);
    });
}



function deleteExpense(timestamp) {

    fetch(BASE_URL + "/delete-expense/" + timestamp, {
        method: "DELETE"
    })
    .then(res => res.json())
    .then(data => {

        alert(data.message);

        loadExpenses();
        getSummary();

    })
    .catch(err => console.error("Delete error:", err));
}



function loginUser() {

    const username = document.getElementById("loginUsername").value;
    const password = document.getElementById("loginPassword").value;

    const bodyData = username + "|" + password;

    fetch("http://localhost:8080/login", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: bodyData
    })
    .then(res => res.json())
    .then(data => {

        if (data.status === "success") {

            localStorage.setItem("username", data.username);

            alert("Login successful");

            showDashboard();

        } else {

            alert(data.message);

        }

    })
    .catch(err => console.error(err));
}


// SHOW DASHBOARD AFTER LOGIN
function showDashboard() {

    document.getElementById("loginSection").style.display = "none";
    document.getElementById("dashboardSection").style.display = "block";

    const user = localStorage.getItem("username");

    document.getElementById("welcomeUser").innerText =
        "Logged in as: " + user;

    loadExpenses();
    getSummary();
}


// LOGOUT FUNCTION
function logoutUser() {

    fetch("http://localhost:8080/logout", {
        method: "POST"
    });

    localStorage.removeItem("username");

    document.getElementById("dashboardSection").style.display = "none";
    document.getElementById("loginSection").style.display = "block";
}


// AUTO LOGIN IF USER ALREADY LOGGED IN




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

function renderChart(data) {
    const ctx = document.getElementById("expenseChart").getContext("2d");

    const labels = Object.keys(data.byCategory);
    const values = Object.values(data.byCategory);

    new Chart(ctx, {
        type: "pie",
        data: {
            labels: labels,
            datasets: [{
                data: values
            }]
        }
    });
}


function setBudget() {

    const budget = document.getElementById("newBudget").value;

    fetch("http://localhost:8080/set-budget", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: budget
    })
    .then(res => res.json())
    .then(data => {

        alert(data.message);

        getSummary(); // refresh dashboard

    })
    .catch(err => console.error(err));
}



window.onload = function () {

    const user = localStorage.getItem("username");

    if (user) {
        showDashboard();
    }
};
