DROP TABLE Users;

DROP TABLE Reservations;

DROP TABLE FlightSeatsBooked;

SELECT * FROM Users;

SELECT * FROM Reservations;

SELECT * FROM FlightSeatsBooked;

mvn clean compile assembly:single
java -jar target/FlightApp-1.0-jar-with-dependencies.jar