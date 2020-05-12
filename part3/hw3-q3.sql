SELECT DISTINCT F1.origin_city AS city 
  FROM Flights AS F1 
 WHERE F1.origin_city NOT IN 
       (SELECT DISTINCT F2.origin_city 
          FROM Flights AS F2 
         WHERE F2.actual_time >= 180) 
   AND F1.canceled = 0 
 ORDER BY F1.origin_city;

/*
Rows returned: 109 rows
Execution time: 00:00:16.323
First 20 rows:

city
Aberdeen SD
Abilene TX
Alpena MI
Ashland WV
Augusta GA
Barrow AK
Beaumont/Port Arthur TX
Bemidji MN
Bethel AK
Binghamton NY
Brainerd MN
Bristol/Johnson City/Kingsport TN
Butte MT
Carlsbad CA
Casper WY
Cedar City UT
Chico CA
College Station/Bryan TX
Columbia MO
Columbus GA
*/