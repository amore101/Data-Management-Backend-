CREATE TABLE Users (username VARCHAR(20) PRIMARY KEY,
    password_hash VARBINARY(20),
    salt VARBINARY(20),
    balance INT
);

CREATE TABLE Reservations (rid INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(20),
    fid1 INT,
    fid2 INT,
    one_hop INT,
    paid INT,
    canceled INT
);

CREATE TABLE FlightSeatsBooked (fid INT,
    seats_booked INT
);