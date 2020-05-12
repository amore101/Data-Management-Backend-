SELECT DISTINCT F2.dest_city AS city 
  FROM Flights AS F2 
 WHERE F2.dest_city <> 'Seattle WA' 
   AND F2.dest_city NOT IN 
       (SELECT DISTINCT F1.dest_city 
          FROM Flights AS F1 
         WHERE F1.origin_city = 'Seattle WA') 
   AND F2.origin_city IN 
       (SELECT DISTINCT F1.dest_city 
          FROM Flights AS F1 
         WHERE F1.origin_city = 'Seattle WA') 
 ORDER BY F2.dest_city;

/*
Rows returned: 256 rows
Execution time: 00:00:22.774
First 20 rows:

city
Aberdeen SD
Abilene TX
Adak Island AK
Aguadilla PR
Akron OH
Albany GA
Albany NY
Alexandria LA
Allentown/Bethlehem/Easton PA
Alpena MI
Amarillo TX
Appleton WI
Arcata/Eureka CA
Asheville NC
Ashland WV
Aspen CO
Atlantic City NJ
Augusta GA
Bakersfield CA
Bangor ME
*/