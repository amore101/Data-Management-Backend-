SELECT DISTINCT F.dest_city AS city 
  FROM Flights AS F 
 WHERE F.dest_city <> 'Seattle WA' 
   AND F.dest_city NOT IN 
       (SELECT DISTINCT F2.dest_city 
          FROM Flights AS F2 
         WHERE F2.dest_city <> 'Seattle WA' 
           AND F2.dest_city NOT IN 
               (SELECT DISTINCT F1.dest_city 
                  FROM Flights AS F1 
                 WHERE F1.origin_city = 'Seattle WA') 
           AND F2.origin_city IN 
               (SELECT DISTINCT F1.dest_city 
                  FROM Flights AS F1 
                 WHERE F1.origin_city = 'Seattle WA')) 
   AND F.dest_city NOT IN 
       (SELECT DISTINCT F1.dest_city AS city 
          FROM Flights AS F1 
         WHERE F1.origin_city = 'Seattle WA') 
 UNION 
SELECT DISTINCT F.origin_city AS city 
  FROM Flights AS F 
 WHERE F.origin_city <> 'Seattle WA' 
   AND F.origin_city NOT IN 
       (SELECT DISTINCT F2.dest_city 
          FROM Flights AS F2 
         WHERE F2.dest_city <> 'Seattle WA' 
           AND F2.dest_city NOT IN 
               (SELECT DISTINCT F1.dest_city 
                  FROM Flights AS F1 
                 WHERE F1.origin_city = 'Seattle WA') 
           AND F2.origin_city IN 
               (SELECT DISTINCT F1.dest_city 
                  FROM Flights AS F1 
                 WHERE F1.origin_city = 'Seattle WA')) 
   AND F.origin_city NOT IN 
       (SELECT DISTINCT F1.dest_city AS city 
          FROM Flights AS F1 
         WHERE F1.origin_city = 'Seattle WA') 
 ORDER BY F.dest_city;

/*
Rows returned: 4 rows
Execution time: 00:01:25.960
First 20 rows:

city
Devils Lake ND
Hattiesburg/Laurel MS
St. Augustine FL
Victoria TX
*/