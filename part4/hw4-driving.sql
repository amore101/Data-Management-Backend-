CREATE TABLE NonProfessionalDriver(ssn INT PRIMARY KEY REFERENCES Person(ssn));

CREATE TABLE ProfessionalDriver(ssn INT PRIMARY KEY REFERENCES Person(ssn),
                    medicalHistory VARCHAR,
                    t_licensePlate VARCHAR UNIQUE REFERENCES Truck(licensePlate));

CREATE TABLE Driver(ssn INT PRIMARY KEY REFERENCES Person(ssn), 
                    driverID INT);

CREATE TABLE Person(ssn INT PRIMARY KEY, 
                    name VARCHAR,
                    v_licensePlate VARCHAR UNIQUE REFERENCES Vehicle(licensePlate));

CREATE TABLE Car(licensePlate VARCHAR PRIMARY KEY REFERENCES Vehicle(licensePlate),
                    make VARCHAR);

CREATE TABLE Truck(licensePlate VARCHAR PRIMARY KEY REFERENCES Vehicle(licensePlate),
                    capacity INT);

CREATE TABLE Vehicle(licensePlate VARCHAR PRIMARY KEY,
                    year INT);

CREATE TABLE InsuranceCo(name VARCHAR PRIMARY KEY,
                    phone INT);

-- many to at-most-one
CREATE TABLE Insurance(v_licensePlate VARCHAR UNIQUE REFERENCES Vehicle(licensePlate),
                    i_name VARCHAR REFERENCES InsuranceCo(name),
                    maxLiability FLOAT,
                    PRIMARY KEY(v_licensePlate, i_name));

-- many to many
CREATE TABLE Drives(c_licensePlate VARCHAR REFERENCES Car(licensePlate),
                    np_ssn INT REFERENCES NonProfessionalDriver(ssn));