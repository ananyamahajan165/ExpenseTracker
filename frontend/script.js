document.getElementById("expenseForm").addEventListener("submit", function (e) {
    e.preventDefault(); // stop page reload

    const amount = document.getElementById("amount").value;
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value;

    if (!amount || !category || !description) {
        alert("Please fill all fields");
        return;
    }

    console.log("Expense Data:", {
        amount,
        category,
        description
    });

    alert("Expense captured on frontend!");
    this.reset();
});