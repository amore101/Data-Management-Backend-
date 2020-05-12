SELECT W.day_of_week AS day_of_week, 
       max_avg_flights.dest_city AS dest_city, 
       max_avg_flights.avg_flights AS avg_flights 
  FROM 
       (SELECT F1.day_of_week_id AS day_of_week_id, 
               F1.dest_city AS dest_city, 
               1.0 * F1.num_flights / F2.num_days AS avg_flights 
          FROM 
               (SELECT COUNT(*) AS num_flights, 
                       F3.day_of_week_id, 
                       F3.dest_city 
                  FROM FLIGHTS AS F3 
                 GROUP BY F3.day_of_week_id, F3.dest_city) AS F1 
                       JOIN 
                       (SELECT COUNT(*) AS num_days, F2.day_of_week_id 
                          FROM 
                               (SELECT DISTINCT F4.month_id, 
                                                F4.day_of_month, 
                                                F4.day_of_week_id 
                                  FROM FLIGHTS AS F4) AS F2 
                         GROUP BY F2.day_of_week_id) AS F2 
                       ON F1.day_of_week_id = F2.day_of_week_id) AS max_avg_flights 
               JOIN 
               (SELECT MAX(1.0 * F1.num_flights / F2.num_days) AS avg_flights 
                  FROM 
                       (SELECT COUNT(*) AS num_flights, 
                               F3.day_of_week_id, 
                               F3.dest_city 
                          FROM FLIGHTS AS F3 
                         GROUP BY F3.day_of_week_id, F3.dest_city) AS F1 
                               JOIN 
                               (SELECT COUNT(*) AS num_days, F2.day_of_week_id 
                                  FROM 
                                       (SELECT DISTINCT F4.month_id, 
                                                        F4.day_of_month, 
                                                        F4.day_of_week_id 
                                          FROM FLIGHTS AS F4) AS F2 
                                 GROUP BY F2.day_of_week_id) AS F2 
                               ON F1.day_of_week_id = F2.day_of_week_id 
                 GROUP BY F1.day_of_week_id 
        
                UNION 
        
                SELECT MAX(1.0 * F1.num_flights / F2.num_days) AS avg_flights 
                  FROM 
                       (SELECT COUNT(*) AS num_flights, 
                               F3.day_of_week_id, 
                               F3.dest_city 
                          FROM FLIGHTS AS F3 
                         GROUP BY F3.day_of_week_id, F3.dest_city) AS F1 
                               JOIN 
                               (SELECT COUNT(*) AS num_days, F2.day_of_week_id 
                                  FROM 
                                       (SELECT DISTINCT F4.month_id, 
                                                        F4.day_of_month, 
                                                        F4.day_of_week_id 
                                          FROM FLIGHTS AS F4) AS F2 
                                 GROUP BY F2.day_of_week_id) AS F2 
                               ON F1.day_of_week_id = F2.day_of_week_id 
                 WHERE 1.0 * F1.num_flights / F2.num_days NOT IN 
                       (SELECT MAX(1.0 * F1.num_flights / F2.num_days) AS avg_flights 
                          FROM 
                               (SELECT COUNT(*) AS num_flights, 
                                       F3.day_of_week_id, 
                                       F3.dest_city 
                                  FROM FLIGHTS AS F3 
                                 GROUP BY F3.day_of_week_id, F3.dest_city) AS F1 
                                       JOIN 
                                       (SELECT COUNT(*) AS num_days, F2.day_of_week_id 
                                          FROM 
                                               (SELECT DISTINCT F4.month_id, 
                                                                F4.day_of_month, 
                                                                F4.day_of_week_id 
                                                  FROM FLIGHTS AS F4) AS F2 
                                         GROUP BY F2.day_of_week_id) AS F2 
                                       ON F1.day_of_week_id = F2.day_of_week_id 
                         GROUP BY F1.day_of_week_id) 
                 GROUP BY F1.day_of_week_id) AS max2_avg_flights 
               ON max_avg_flights.avg_flights = max2_avg_flights.avg_flights 
               JOIN Weekdays AS W 
               ON W.did = max_avg_flights.day_of_week_id 
 ORDER BY max_avg_flights.day_of_week_id, max_avg_flights.avg_flights DESC;

/* Rows returned: 14 rows 
Execution time: 00:01:10.742 
First 20 rows: 

day_of_week	dest_city	avg_flights
Monday	Chicago IL	2171.750000000000
Monday	Atlanta GA	2132.750000000000
Tuesday	Chicago IL	2400.750000000000
Tuesday	Atlanta GA	2334.500000000000
Wednesday	Chicago IL	2450.000000000000
Wednesday	Atlanta GA	2372.750000000000
Thursday	Chicago IL	2452.500000000000
Thursday	Atlanta GA	2348.250000000000
Friday	Chicago IL	2447.000000000000
Friday	Atlanta GA	2350.600000000000
Saturday	Chicago IL	2308.800000000000
Saturday	Atlanta GA	2286.000000000000
Sunday	Chicago IL	2320.000000000000
Sunday	Atlanta GA	2276.600000000000
*/