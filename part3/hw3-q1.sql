SELECT COUNT(*) AS num_connected_cities 
  FROM 
       (SELECT DISTINCT F1.origin_city, F1.dest_city 
          FROM Flights AS F1 
         WHERE F1.origin_city > F1.dest_city) AS F3 
       FULL OUTER JOIN 
       (SELECT DISTINCT F2.origin_city, F2.dest_city 
          FROM Flights AS F2 
         WHERE F2.origin_city < F2.dest_city) AS F4 
       ON F3.origin_city = F4.dest_city 
           AND F3.dest_city = F4.origin_city;

/*
Rows returned: 1 row
Execution time: 00:00:14.971
First 20 rows:

num_connected_cities
2351
*/