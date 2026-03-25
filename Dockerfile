FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac backend/*.java

CMD ["java", "backend.ExpenseTracker"]