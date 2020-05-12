SELECT DISTINCT F3.origin_city AS origin_city, 
                F2.dest_city AS dest_city, 
                F3.time AS time 
  FROM Flights AS F2 
       JOIN 
       (SELECT F1.origin_city, MIN(F1.actual_time) AS time 
          FROM Flights AS F1 
         WHERE F1.canceled = 0 
         GROUP BY F1.origin_city) AS F3 
       ON F2.origin_city = F3.origin_city 
 WHERE F2.actual_time = F3.time 
 ORDER BY F3.time, F3.origin_city;

/*
Rows returned: 339 rows
Execution time: 00:00:15.307
First 20 rows:

origin_city	dest_city	time
Bend/Redmond OR	Los Angeles CA	10
Burbank CA	New York NY	10
Las Vegas NV	Chicago IL	10
New York NY	Nashville TN	10
Newark NJ	Detroit MI	10
Sacramento CA	Atlanta GA	10
Washington DC	Minneapolis MN	10
Boise ID	Chicago IL	11
Boston MA	Philadelphia PA	11
Buffalo NY	Orlando FL	11
Cincinnati OH	New Haven CT	11
Denver CO	Honolulu HI	11
Denver CO	Orlando FL	11
Denver CO	Philadelphia PA	11
Fort Myers FL	Chicago IL	11
Houston TX	Salt Lake City UT	11
Minneapolis MN	Newark NJ	11
Pittsburgh PA	Dallas/Fort Worth TX	11
Indianapolis IN	Houston TX	12
Phoenix AZ	Dallas/Fort Worth TX	12
*/