# CSE 414 Homework 4: Database Design

Repo ID: `cse414-hw4-richtian-ruikang`

Total: 81/100

---

* Part 1 (20/20)
* Part 2a.1 (6/10)
    * -1: NonProfessionalDriver and ProfessionalDriver should reference Driver.ssn instead of Person.ssn since a Person is not necessarily a Driver.
    * -1: ProfessionalDriver should not have a UNIQUE constraint on Truck.licensePlate. That would imply that each ProfessionalDriver can only drive one Truck, when it's the other way around. Truck should have the reference to ProfessionalDriver.ssn.
    * -1: Person should not have a UNIQUE constraint on Vehicle.licensePlate--the two entities aren't directly related.
    * -1: Drives should have a composite primary key of (licensePlate, ssn).
* Part 2a.2 (0/5), 2a.3 (0/5)
    * -10 Please answer 2a.2 and 2a.3 explicitly
* Part 2b (15/15)
* Part 3 (15/15)
* Part 4.1 (0/5), 4.2 (10/10), 4.3 (10/10), 4.4 (5/5)
    * -5: no table created
