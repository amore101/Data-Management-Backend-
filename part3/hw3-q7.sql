SELECT DISTINCT C.name AS carrier 
  FROM Flights AS F 
       JOIN Carriers AS C 
       ON C.cid = F.carrier_id 
 WHERE F.origin_city = 'Seattle WA' 
   AND F.dest_city = 'San Francisco CA' 
 ORDER BY C.name;

/*
Rows returned: 4 rows
Execution time: 00:00:03.066
First 20 rows:

carrier
Alaska Airlines Inc.
SkyWest Airlines Inc.
United Air Lines Inc.
Virgin America
*/