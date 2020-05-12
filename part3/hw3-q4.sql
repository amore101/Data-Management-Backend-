SELECT C2.origin_city AS origin_city, ISNULL(100.0 * C1.non_canceled_3_hrs / C2.non_canceled, 0) AS percentage 
  FROM 
       (SELECT COUNT(*) AS non_canceled_3_hrs, F1.origin_city 
          FROM Flights AS F1 
         WHERE F1.actual_time < 180 
           AND F1.canceled = 0 
         GROUP BY F1.origin_city) AS C1 
       RIGHT OUTER JOIN 
       (SELECT COUNT(*) AS non_canceled, F1.origin_city 
          FROM Flights AS F1 
         WHERE F1.canceled = 0 
         GROUP BY F1.origin_city) AS C2 
       ON C1.origin_city = C2.origin_city 
 ORDER BY 100.0 * C1.non_canceled_3_hrs / C2.non_canceled;

/*
Rows returned: 327 rows
Execution time: 00:00:14.961
First 20 rows:

origin_city	percentage
Guam TT	0.000000000000
Pago Pago TT	0.000000000000
Aguadilla PR	28.897338403041
Anchorage AK	31.812080536912
San Juan PR	33.660531697341
Charlotte Amalie VI	39.558823529411
Ponce PR	40.983606557377
Fairbanks AK	50.116550116550
Kahului HI	53.514471352628
Honolulu HI	54.739028823682
San Francisco CA	55.828864537188
Los Angeles CA	56.080890822987
Seattle WA	57.609387792231
Long Beach CA	62.176439513998
New York NY	62.371834136728
Kona HI	63.160792951541
Las Vegas NV	64.920256372037
Christiansted VI	65.100671140939
Newark NJ	65.849971096980
Plattsburgh NY	66.666666666666
*/