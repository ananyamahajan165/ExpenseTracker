const BASE_URL = "http://localhost:8080";

function addExpense() {
    const amount = document.getElementById("amount").value;
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value;

    fetch(BASE_URL + "/add-expense", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ amount, category, description })
    })
    .then(res => res.json())
    .then(data => alert(data.message));
}

function getSummary() {
    fetch(BASE_URL + "/summary")
    .then(res => res.json())
    .then(data => {
        document.getElementById("summary").textContent =
            JSON.stringify(data, null, 2);
    });
}