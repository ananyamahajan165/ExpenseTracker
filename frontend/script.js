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

        document.getElementById("amount").value = "";
        document.getElementById("category").value = "";
        document.getElementById("description").value = "";
        
        loadExpenses();
        getSummary();
    })
    .catch(err => console.error(err));
}



function loadExpenses() {
    fetch("http://localhost:8080/getExpenses")
    .then(res => res.text())
    .then(data => {

        const rows = data.trim().split("\n");
        let html = "";
        let total = 0;

        for (let i = 0; i < rows.length; i++) {

            if (rows[i].trim() === "") continue;

            const parts = rows[i].split(",");

            const amount = parseInt(parts[0]);
            const category = parts[1];

            total += amount;

            html += `<tr>
                        <td>${amount}</td>
                        <td>${category}</td>
                        <td>-</td>
                        <td>-</td>
                        <td><button onclick="deleteExpense(${i})">Delete</button></td>
                     </tr>`;
        }

        document.getElementById("expenseTable").innerHTML = html;
        document.getElementById("totalAmount").innerText = total;
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



function registerUser() {

    const username = document.getElementById("registerUsername").value;
    const password = document.getElementById("registerPassword").value;

    const bodyData = username + "|" + password;

    fetch("http://localhost:8080/register", {
        method: "POST",
        headers: {
            "Content-Type": "text/plain"
        },
        body: bodyData
    })
    .then(res => res.json())
    .then(data => {

        alert(data.message);

    })
    .catch(err => console.error(err));
}





// SHOW DASHBOARD AFTER LOGIN
function showDashboard() {

    document.getElementById("loginSection").style.display = "none";
    document.getElementById("dashboardSection").style.display = "block";

    const user = localStorage.getItem("username");

    document.getElementById("welcomeUser").innerText = "Logged in as: " + user;

    loadExpenses();   // IMPORTANT
    getSummary();     // IMPORTANT
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


function setBudget(){

    const budget = document.getElementById("budgetInput").value;

    fetch("http://localhost:8080/set-budget", {
        method:"POST",
        headers:{
            "Content-Type":"text/plain"
        },
        body: budget
    })
    .then(res=>res.json())
    .then(data=>{

        alert("Budget updated");

        getSummary();

    });

}

function showRegister(){
document.getElementById("registerForm").style.display="block";
}

function showLogin(){
document.getElementById("registerForm").style.display="none";
}

window.onload = function () {

    const user = localStorage.getItem("username");

    if (user) {
        showDashboard();
    }
};


function signup() {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  fetch(BASE_URL + "/signup", {
    method: "POST",
    body: JSON.stringify({ username, password })
  })
  .then(res => res.text())
  .then(data => alert(data));
}

function login() {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;

  fetch(BASE_URL + "/login", {
    method: "POST",
    body: JSON.stringify({ username, password })
  })
  .then(res => res.text())
  .then(data => {
    if(data === "success") {
      localStorage.setItem("user", username);
      alert("Login successful");
    } else {
      alert("Invalid login");
    }
  });
}

loadExpenses();