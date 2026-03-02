const BASE_URL = "http://localhost:8080";
function addExpense() {

    const amount = document.getElementById("amount").value;
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value;

    fetch("http://localhost:8080/add-expense", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            amount: amount,
            category: category,
            description: description
        })
    })
    .then(res => res.json())
    .then(data => {
        alert(data.message);
        loadSummary(); // refresh summary automatically
    })
    .catch(err => console.error(err));
}

function loadExpenses() {
    fetch(BASE_URL + "/expenses")
    .then(res => res.json())
    .then(data => {

        const tbody = document.querySelector("#expenseTable tbody");
        tbody.innerHTML = "";

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
    .catch(err => console.error("Error loading expenses:", err));
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




    
function getSummary() {
    fetch(BASE_URL + "/summary")
    .then(res => res.json())
    .then(data => {

        // Update total amount text
        document.getElementById("totalAmount").innerText = data.total;

        // Render chart
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