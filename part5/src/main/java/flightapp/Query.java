package flightapp;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
  // DB Connection
  private Connection conn;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;

  // Canned queries
  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  // For check dangling
  private static final String TRANCOUNT_SQL = "SELECT @@TRANCOUNT AS tran_count";
  private PreparedStatement tranCountStatement;

  // YOUR CODE HERE
  private static final String BEGIN_TRANSACTION_SQL = "BEGIN TRANSACTION;";
  private static final String COMMIT_SQL = "COMMIT TRANSACTION";
  private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";

  // For inserting users
  private static final String INSERT_USER_SQL = "INSERT INTO Users VALUES (?, ?, ?, ?)";
  private PreparedStatement insertUserStatement;


  public Query() throws SQLException, IOException {
    this(null, null, null, null);
  }

  protected Query(String serverURL, String dbName, String adminName, String password) throws SQLException, IOException {
    conn = serverURL == null ? openConnectionFromDbConn()
        : openConnectionFromCredential(serverURL, dbName, adminName, password);

    prepareStatements();
  }

  /**
   * Return a connecion by using dbconn.properties file
   *
   * @throws SQLException
   * @throws IOException
   */
  public static Connection openConnectionFromDbConn() throws SQLException, IOException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("dbconn.properties"));
    String serverURL = configProps.getProperty("hw5.server_url");
    String dbName = configProps.getProperty("hw5.database_name");
    String adminName = configProps.getProperty("hw5.username");
    String password = configProps.getProperty("hw5.password");
    return openConnectionFromCredential(serverURL, dbName, adminName, password);
  }

  /**
   * Return a connecion by using the provided parameter.
   *
   * @param serverURL example: example.database.widows.net
   * @param dbName    database name
   * @param adminName username to login server
   * @param password  password to login server
   *
   * @throws SQLException
   */
  protected static Connection openConnectionFromCredential(String serverURL, String dbName, String adminName,
      String password) throws SQLException {
    String connectionUrl = String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
        dbName, adminName, password);
    Connection conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    return conn;
  }

  /**
   * Get underlying connection
   */
  public Connection getConnection() {
    return conn;
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {
      // YOUR CODE HERE
      String clearTablesSQL = "TRUNCATE TABLE Users; TRUNCATE TABLE FlightSeatsBooked; "
        + "TRUNCATE TABLE Reservations;";
      Statement clearTablesStatement = conn.createStatement();
      clearTablesStatement.executeQuery(clearTablesSQL);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    tranCountStatement = conn.prepareStatement(TRANCOUNT_SQL);
    // YOUR CODE HERE
    insertUserStatement = conn.prepareStatement(INSERT_USER_SQL);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged
   *         in\n" For all other errors, return "Login failed\n". Otherwise,
   *         return "Logged in as [username]\n".
   */
  private String userloggedIn = null;

  public String transaction_login(String username, String password) {
    if (userloggedIn != null)
      return "User already logged in\n";
    try {
      // YOUR CODE HERE
      try {
        String selectUserSQL = "SELECT password_hash, salt FROM Users WHERE username = \'" + username + "\'";
        Statement selectUserStatement = conn.createStatement();
        ResultSet oneHopResults = selectUserStatement.executeQuery(selectUserSQL);
        oneHopResults.next();
        byte[] result_hash = oneHopResults.getBytes("password_hash");
        byte[] result_salt = oneHopResults.getBytes("salt");
        oneHopResults.close();

        // Specify the hash parameters
        KeySpec spec = new PBEKeySpec(password.toCharArray(), result_salt, HASH_STRENGTH, KEY_LENGTH);

        // Generate the hash
        SecretKeyFactory factory = null;
        byte[] hash = null;
        try {
          factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
          hash = factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
          throw new IllegalStateException();
        }
        if (Arrays.equals(hash, result_hash)) {
          userloggedIn = username;
          return "Logged in as " + username + "\n";
        }
        return "Login failed\n";
      } catch (SQLException e) {
        e.printStackTrace();
        return "Login failed\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    if (initAmount < 0)
      return "Failed to create user\n";
    try {
      // YOUR CODE HERE
      // Generate a random cryptographic salt
      SecureRandom random = new SecureRandom();
      byte[] salt = new byte[16];
      random.nextBytes(salt);

      // Specify the hash parameters
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

      // Generate the hash
      SecretKeyFactory factory = null;
      byte[] hash = null;
      try {
        factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        hash = factory.generateSecret(spec).getEncoded();
        try {
          insertUserStatement.setString(1, username);
          insertUserStatement.setBytes(2, hash);
          insertUserStatement.setBytes(3, salt);
          insertUserStatement.setInt(4, initAmount);
          insertUserStatement.execute();
        } catch (SQLException e) {
          e.printStackTrace();
          return "Failed to create user\n";
        }
        return "Created user " + username + "\n";
      } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
        throw new IllegalStateException();
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given day
   * of the month. If {@code directFlight} is true, it only searches for direct flights, otherwise
   * is searches for direct flights and flights with two "hops." Only searches for up to the number
   * of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  private ArrayList<Itinerary> itineraries = new ArrayList<>();

  public String transaction_search(String originCity, String destinationCity, boolean directFlight,
      int dayOfMonth, int numberOfItineraries) {
    try {
      // WARNING the below code is unsafe and only handles searches for direct flights
      // You can use the below code as a starting reference point or you can get rid
      // of it all and replace it with your own implementation.
      //
      // YOUR CODE HERE

      StringBuffer sb = new StringBuffer();

      if (directFlight == true) {
        try {
          // one hop itineraries
          String unsafeSearchSQL = "SELECT TOP (" + numberOfItineraries
              + ") fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
              + "FROM Flights " + "WHERE canceled = 0 AND origin_city = \'" + originCity + "\' AND dest_city = \'"
              + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
              + "ORDER BY actual_time ASC";
  
          Statement searchStatement = conn.createStatement();
          ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);
          
          itineraries = new ArrayList<>();
          while (oneHopResults.next()) {
            int result_fid = oneHopResults.getInt("fid");
            int result_dayOfMonth = oneHopResults.getInt("day_of_month");
            String result_carrierId = oneHopResults.getString("carrier_id");
            String result_flightNum = oneHopResults.getString("flight_num");
            String result_originCity = oneHopResults.getString("origin_city");
            String result_destCity = oneHopResults.getString("dest_city");
            int result_time = oneHopResults.getInt("actual_time");
            int result_capacity = oneHopResults.getInt("capacity");
            int result_price = oneHopResults.getInt("price");

            Flight f = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity, result_destCity, result_time, result_capacity, result_price);
            itineraries.add(new Itinerary(f, new Flight(), true));
          }
          oneHopResults.close();
          if(itineraries.isEmpty())
            return "No flights match your selection\n";
          for (int itineraryCount = 0; itineraryCount < itineraries.size(); itineraryCount++) {
            Itinerary itin = itineraries.get(itineraryCount);
            sb.append("Itinerary " + itineraryCount + ": 1 flight(s), " + itin.totalTime + " minutes\n"
              + "ID: " + itin.flight1.fid + " Day: " + itin.flight1.dayOfMonth + " Carrier: " + itin.flight1.carrierId
              + " Number: " + itin.flight1.flightNum + " Origin: " + itin.flight1.originCity + " Dest: " + itin.flight1.destCity
              + " Duration: " + itin.flight1.time + " Capacity: " + itin.flight1.capacity + " Price: " + itin.flight1.price + "\n");
          }

        } catch (SQLException e) {
          e.printStackTrace();
        }
  
        return sb.toString();
      }
      else {
        itineraries = new ArrayList<>();
        try {
          // one hop itineraries
          String unsafeSearchSQL = "SELECT TOP (" + numberOfItineraries
              + ") fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
              + "FROM Flights " + "WHERE canceled = 0 AND origin_city = \'" + originCity + "\' AND dest_city = \'"
              + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
              + "ORDER BY actual_time ASC";
  
          Statement searchStatement = conn.createStatement();
          ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);
          
          while (oneHopResults.next()) {
            int result_fid = oneHopResults.getInt("fid");
            int result_dayOfMonth = oneHopResults.getInt("day_of_month");
            String result_carrierId = oneHopResults.getString("carrier_id");
            String result_flightNum = oneHopResults.getString("flight_num");
            String result_originCity = oneHopResults.getString("origin_city");
            String result_destCity = oneHopResults.getString("dest_city");
            int result_time = oneHopResults.getInt("actual_time");
            int result_capacity = oneHopResults.getInt("capacity");
            int result_price = oneHopResults.getInt("price");

            Flight f = new Flight(result_fid, result_dayOfMonth, result_carrierId, result_flightNum, result_originCity, result_destCity, result_time, result_capacity, result_price);
            itineraries.add(new Itinerary(f, new Flight(), true));
          }
          oneHopResults.close();

          int numberOfIndirectItineraries = numberOfItineraries - itineraries.size();

          // two hop itineraries
          String selectItinerariesSQL = "SELECT TOP (" + numberOfIndirectItineraries + ") F1.fid AS F1_fid,F1.day_of_month AS F1_day_of_month,F1.carrier_id AS F1_carrier_id,F1.flight_num AS F1_flight_num,F1.origin_city AS F1_origin_city,F1.dest_city AS F1_dest_city,F1.actual_time AS F1_actual_time,F1.capacity AS F1_capacity,F1.price AS F1_price,"
                +       "F2.fid AS F2_fid,F2.day_of_month AS F2_day_of_month,F2.carrier_id AS F2_carrier_id,F2.flight_num AS F2_flight_num,F2.origin_city AS F2_origin_city,F2.dest_city AS F2_dest_city,F2.actual_time AS F2_actual_time,F2.capacity AS F2_capacity,F2.price AS F2_price "
                +       "FROM Flights F1, Flights F2 WHERE F1.canceled = 0 AND F2.canceled = 0 AND F1.day_of_month = " + dayOfMonth 
                +       " AND F1.day_of_month = F2.day_of_month"
                +       " AND F1.origin_city = \'" + originCity + "\'"
                +       " AND F1.dest_city = F2.origin_city"
                +       " AND F2.dest_city = \'" + destinationCity + "\' "
                +       "ORDER BY (F1.actual_time+F2.actual_time) ASC";
  
          Statement selectItinerariesStatement = conn.createStatement();
          oneHopResults = selectItinerariesStatement.executeQuery(selectItinerariesSQL);

          while (oneHopResults.next()) {
            int result_F1_fid = oneHopResults.getInt("F1_fid");
            int result_F1_dayOfMonth = oneHopResults.getInt("F1_day_of_month");
            String result_F1_carrierId = oneHopResults.getString("F1_carrier_id");
            String result_F1_flightNum = oneHopResults.getString("F1_flight_num");
            String result_F1_originCity = oneHopResults.getString("F1_origin_city");
            String result_F1_destCity = oneHopResults.getString("F1_dest_city");
            int result_F1_time = oneHopResults.getInt("F1_actual_time");
            int result_F1_capacity = oneHopResults.getInt("F1_capacity");
            int result_F1_price = oneHopResults.getInt("F1_price");

            Flight f1 = new Flight(result_F1_fid, result_F1_dayOfMonth, result_F1_carrierId, result_F1_flightNum, result_F1_originCity, result_F1_destCity, result_F1_time, result_F1_capacity, result_F1_price);

            int result_F2_fid = oneHopResults.getInt("F2_fid");
            int result_F2_dayOfMonth = oneHopResults.getInt("F2_day_of_month");
            String result_F2_carrierId = oneHopResults.getString("F2_carrier_id");
            String result_F2_flightNum = oneHopResults.getString("F2_flight_num");
            String result_F2_originCity = oneHopResults.getString("F2_origin_city");
            String result_F2_destCity = oneHopResults.getString("F2_dest_city");
            int result_F2_time = oneHopResults.getInt("F2_actual_time");
            int result_F2_capacity = oneHopResults.getInt("F2_capacity");
            int result_F2_price = oneHopResults.getInt("F2_price");

            Flight f2 = new Flight(result_F2_fid, result_F2_dayOfMonth, result_F2_carrierId, result_F2_flightNum, result_F2_originCity, result_F2_destCity, result_F2_time, result_F2_capacity, result_F2_price);
            itineraries.add(new Itinerary(f1, f2, false));
          }
          oneHopResults.close();

          if(itineraries.isEmpty())
            return "No flights match your selection\n";

          Collections.sort(itineraries);
          
          for (int itineraryCount = 0; itineraryCount < itineraries.size(); itineraryCount++) {
            Itinerary itin = itineraries.get(itineraryCount);
            if (itin.oneHop == true) {
              sb.append("Itinerary " + itineraryCount + ": 1 flight(s), " + itin.totalTime + " minutes\n"
                + "ID: " + itin.flight1.fid + " Day: " + itin.flight1.dayOfMonth + " Carrier: " + itin.flight1.carrierId
                + " Number: " + itin.flight1.flightNum + " Origin: " + itin.flight1.originCity + " Dest: " + itin.flight1.destCity
                + " Duration: " + itin.flight1.time + " Capacity: " + itin.flight1.capacity + " Price: " + itin.flight1.price + "\n");

            }
            else {
              sb.append("Itinerary " + itineraryCount + ": 2 flight(s), " + itin.totalTime + " minutes\n"
                + "ID: " + itin.flight1.fid + " Day: " + itin.flight1.dayOfMonth + " Carrier: " + itin.flight1.carrierId
                + " Number: " + itin.flight1.flightNum + " Origin: " + itin.flight1.originCity + " Dest: " + itin.flight1.destCity
                + " Duration: " + itin.flight1.time + " Capacity: " + itin.flight1.capacity + " Price: " + itin.flight1.price + "\n"
                + "ID: " + itin.flight2.fid + " Day: " + itin.flight2.dayOfMonth + " Carrier: " + itin.flight2.carrierId
                + " Number: " + itin.flight2.flightNum + " Origin: " + itin.flight2.originCity + " Dest: " + itin.flight2.destCity
                + " Duration: " + itin.flight2.time + " Capacity: " + itin.flight2.capacity + " Price: " + itin.flight2.price + "\n");
            }
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
  
        return sb.toString();
      }
      
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in
   *                    the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   *         If the user is trying to book an itinerary with an invalid ID or without having done a
   *         search, then return "No such itinerary {@code itineraryId}\n". If the user already has
   *         a reservation on the same day as the one that they are trying to book now, then return
   *         "You cannot book two flights in the same day\n". For all other errors, return "Booking
   *         failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from 1 and
   *         increments by 1 each time a successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId) {
    // YOUR CODE HERE
    if (userloggedIn == null)
      return "Cannot book reservations, not logged in\n";
    if (itineraries.size() == 0 || itineraryId < 0 || itineraryId >= itineraries.size())
      return "No such itinerary " + itineraryId + "\n";
    try {
      try {
        Itinerary itin = itineraries.get(itineraryId);
        int dayOfMonth = itin.flight1.dayOfMonth;

        // See if any reservations for that day
        String selectReservationsAndFlightsSQL = "SELECT F.fid FROM Reservations R, Flights F "
          + "WHERE R.username = \'" + userloggedIn + "\' AND R.fid1 = F.fid AND F.day_of_month = " + dayOfMonth;
        Statement SQLStatement = conn.createStatement();
        SQLStatement.execute(BEGIN_TRANSACTION_SQL);
        ResultSet results = SQLStatement.executeQuery(selectReservationsAndFlightsSQL);
        
        // If already booked for that day
        if (results.next()) {
          results.close();
          SQLStatement.execute(ROLLBACK_SQL);
          SQLStatement.close();
          return "You cannot book two flights in the same day\n";
        }
        // One hop itinerary
        if (itin.oneHop == true) {
          int fid1 = itin.flight1.fid;
          // See how many seats are left for first flight
          String selectFlightsAndFlightSeatsBookedSQL = "SELECT F.capacity, FSB.seats_booked FROM Flights F, FlightSeatsBooked FSB WHERE F.fid = FSB.fid AND F.fid = " + fid1;
          results = SQLStatement.executeQuery(selectFlightsAndFlightSeatsBookedSQL);
          
          // Record exists already
          if (results.next()) {
            int result_F1_capacity = results.getInt("capacity");
            int result_F1_seats_booked = results.getInt("seats_booked");
            results.close();
            // Not enough capacity
            if(result_F1_capacity <= result_F1_seats_booked) {
              SQLStatement.execute(ROLLBACK_SQL);
              SQLStatement.close();
              return "Booking failed\n";
            }
            // Enough capacity
            // Update the number of booked seats for flight
            String updateFlightSeatsBookedSQL = "UPDATE FlightSeatsBooked SET seats_booked = seats_booked + 1 WHERE fid = " + fid1;
            SQLStatement.execute(updateFlightSeatsBookedSQL);
          }
          // No record exists yet
          else {
            results.close();
            // Create a record
            String insertFlightSeatsBookedSQL = "INSERT INTO FlightSeatsBooked (fid, seats_booked) "
              + "VALUES (" + fid1 + ", 1)";
            SQLStatement.execute(insertFlightSeatsBookedSQL);
          }
          
          // Create a reservation
          String insertReservationsSQL = "INSERT INTO Reservations (username, fid1, one_hop, paid, canceled) "
            + "VALUES (\'" + userloggedIn + "\', " + fid1 + ", 1, 0, 0) "
            + "SELECT SCOPE_IDENTITY()";
          SQLStatement.execute(insertReservationsSQL);
          results = SQLStatement.getGeneratedKeys();
          results.next();
          int rid = results.getInt(1);
          results.close();
          SQLStatement.execute(COMMIT_SQL);
          SQLStatement.close();

          return "Booked flight(s), reservation ID: " + rid + "\n";
        }

        // Two hop itinerary
        else {
          results.close();
          int fid1 = itin.flight1.fid;
          int fid2 = itin.flight2.fid;

          // See how many seats are left for both flights
          String selectFlightsAndFSBSQL = "SELECT F1.capacity AS F1_capacity, FSB1.seats_booked AS F1_seats_booked, F2.capacity AS F2_capacity, FSB2.seats_booked AS F2_seats_booked "
           + "FROM Flights F1, FlightSeatsBooked FSB1, Flights F2, FlightSeatsBooked FSB2 WHERE F1.fid = FSB1.fid AND F1.fid = " + fid1 + " AND F2.fid = FSB2.fid AND F2.fid = " + fid2;
          results = SQLStatement.executeQuery(selectFlightsAndFSBSQL);
          
          // Record exists already
          if (results.next()) {
            results.close();
            int result_F1_capacity = results.getInt("F1_capacity");
            int result_F1_seats_booked = results.getInt("F1_seats_booked");
            int result_F2_capacity = results.getInt("F2_capacity");
            int result_F2_seats_booked = results.getInt("F2_seats_booked");
            // Not enough capacity
            if(result_F1_capacity <= result_F1_seats_booked || result_F2_capacity <= result_F2_seats_booked) {
              SQLStatement.execute(ROLLBACK_SQL);
              SQLStatement.close();
              return "Booking failed\n";
            }
            // Enough capacity
            // Update the number of booked seats for flight
            String updateFlightSeatsBookedSQL = "UPDATE FlightSeatsBooked FSB1, FlightSeatsBooked FSB2 "
              + "SET FSB1.seats_booked = FSB1.seats_booked + 1, FSB2.seats_booked = FSB2.seats_booked + 1 "
              + "WHERE FSB1.fid = " + fid1 + " AND FSB2.fid = " + fid2;
              SQLStatement.execute(updateFlightSeatsBookedSQL);
          }
          // No record exists yet for one or both flights
          else {
            results.close();
            // See how many seats are left for first flight
            String selectF1CapacitySQL = "SELECT F.capacity, FSB.seats_booked FROM Flights F, FlightSeatsBooked FSB WHERE F.fid = FSB.fid AND F.fid = " + fid1;
            results = SQLStatement.executeQuery(selectF1CapacitySQL);
            
            // Record exists already for first flight
            if (results.next()) {
              results.close();
              int result_F1_capacity = results.getInt("capacity");
              int result_F1_seats_booked = results.getInt("seats_booked");
              // Not enough capacity
              if(result_F1_capacity <= result_F1_seats_booked) {
                SQLStatement.execute(ROLLBACK_SQL);
                SQLStatement.close();
                return "Booking failed\n";
              }

              // See how many seats are left for second flight
              String selectF2CapacitySQL = "SELECT F.capacity, FSB.seats_booked FROM Flights F, FlightSeatsBooked FSB WHERE F.fid = FSB.fid AND F.fid = " + fid2;
              results = SQLStatement.executeQuery(selectF2CapacitySQL);
              
              // Record exists already for second flight
              if (results.next()) {
                results.close();
                int result_F2_capacity = results.getInt("capacity");
                int result_F2_seats_booked = results.getInt("seats_booked");
                // Not enough capacity
                if(result_F2_capacity <= result_F2_seats_booked) {
                  SQLStatement.execute(ROLLBACK_SQL);
                  SQLStatement.close();
                  return "Booking failed\n";
                }
                
                // Enough capacity
                // Update the number of booked seats for both flights
                String updateFlightSeatsBookedSQL = "UPDATE FlightSeatsBooked FSB1, FlightSeatsBooked FSB2 "
                  + "SET FSB1.seats_booked = FSB1.seats_booked + 1, FSB2.seats_booked = FSB2.seats_booked + 1 "
                  + "WHERE FSB1.fid = " + fid1 + " AND FSB2.fid = " + fid2;
                SQLStatement.execute(updateFlightSeatsBookedSQL);
              }
              // Record does not exist for second flight
              else {
                // Create a record
                String insertFlightSeatsBookedSQL = "INSERT INTO FlightSeatsBooked (fid, seats_booked) "
                  + "VALUES (" + fid2 + ", 1)";
                SQLStatement.execute(insertFlightSeatsBookedSQL);
              }
            }
            // Record does not exist for first flight
            else {
              // Create a record
              String insertFlightSeatsBookedSQL = "INSERT INTO FlightSeatsBooked (fid, seats_booked) "
                + "VALUES (" + fid1 + ", 1)";
              SQLStatement.execute(insertFlightSeatsBookedSQL);
            }
          }
          
          // Create a reservation
          String insertReservationsSQL = "INSERT INTO Reservations (username, fid1, fid2, one_hop, paid, canceled) "
            + "VALUES (\'" + userloggedIn + "\', " + fid1 + ", " + fid2 + ", 0, 0, 0) "
            + "SELECT SCOPE_IDENTITY()";
          SQLStatement.execute(insertReservationsSQL);
          results = SQLStatement.getGeneratedKeys();
          results.next();
          int rid = results.getInt(1);
          results.close();
          SQLStatement.execute(COMMIT_SQL);
          SQLStatement.close();

          return "Booked flight(s), reservation ID: " + rid + "\n";
        }
        
      } catch (SQLException e) {
        e.printStackTrace();
        return "Booking failed\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n" If the reservation
   *         is not found / not under the logged in user's name, then return "Cannot find unpaid
   *         reservation [reservationId] under user: [username]\n" If the user does not have enough
   *         money in their account, then return "User has only [balance] in account but itinerary
   *         costs [cost]\n" For all other errors, return "Failed to pay for reservation
   *         [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    // YOUR CODE HERE
    if (userloggedIn == null)
      return "Cannot pay, not logged in\n";
    try {
      try {
        // one hop itineraries
        String selectReservationsSQL = "SELECT fid1, fid2, one_hop FROM Reservations "
          + "WHERE rid = " + reservationId + " AND username = \'" + userloggedIn + "\' AND paid = 0";
        Statement SQLStatement = conn.createStatement();
        // Transaction begins here
        SQLStatement.execute(BEGIN_TRANSACTION_SQL);

        ResultSet results = SQLStatement.executeQuery(selectReservationsSQL);
        if(results.next()) {
          int fid1 = results.getInt("fid1");
          int fid2 = results.getInt("fid2");
          int one_hop = results.getInt("one_hop");
          results.close();
          // One hop itinerary
          if(one_hop == 1) {
            String selectUsersAndFlightsSQL = "SELECT U.balance, F.price FROM Users U, Flights F "
              + "WHERE U.username = \'" + userloggedIn + "\' AND F.fid = " + fid1;
            results = SQLStatement.executeQuery(selectUsersAndFlightsSQL);
            results.next();
            int balance = results.getInt("balance");
            int price = results.getInt("price");
            results.close();
            if (balance >= price) {
              int remainingBalance = balance - price;
              String updateUsersAndReservationsSQL = "UPDATE Users SET balance = balance - " + price + " "
                + "WHERE username = \'" + userloggedIn + "\'; "
                + "UPDATE Reservations SET paid = 1 WHERE rid = \'" + reservationId + "\'";
              SQLStatement.executeUpdate(updateUsersAndReservationsSQL);
              SQLStatement.execute(COMMIT_SQL);
              SQLStatement.close();
              return "Paid reservation: " + reservationId + " remaining balance: " + remainingBalance + "\n";
            }
            // Not enough money
            else {
              SQLStatement.execute(ROLLBACK_SQL);
              SQLStatement.close();
              return "User has only " + balance + " in account but itinerary costs " + price + "\n";
            }
          }
          // Two hop
          else {
            String selectUsersAndFlightsSQL = "SELECT U.balance, F1.price AS F1_price, F2.price AS F2_price "
              + "FROM Users U, Flights F1, Flights F2 "
              + "WHERE U.username = \'" + userloggedIn + "\' AND F1.fid = " + fid1 + " AND F2.fid = " + fid2;
            results = SQLStatement.executeQuery(selectUsersAndFlightsSQL);
            results.next();
            int balance = results.getInt("balance");
            int F1_price = results.getInt("F1_price");
            int F2_price = results.getInt("F2_price");
            int totalPrice = F1_price + F2_price;
            results.close();
            if (balance >= totalPrice) {
              int remainingBalance = balance - totalPrice;
              String updateUsersAndReservationsSQL = "UPDATE Users SET balance = balance - " + totalPrice + " "
                + "WHERE username = \'" + userloggedIn + "\'; "
                + "UPDATE Reservations SET paid = 1 WHERE rid = \'" + reservationId + "\'";
              SQLStatement.executeUpdate(updateUsersAndReservationsSQL);
              SQLStatement.execute(COMMIT_SQL);
              SQLStatement.close();
              return "Paid reservation: " + reservationId + " remaining balance: " + remainingBalance + "\n";
            }
            else {
              SQLStatement.execute(ROLLBACK_SQL);
              SQLStatement.close();
              return "User has only " + balance + " in account but itinerary costs " + totalPrice + "\n";
            }
          }
        }
        else {
          results.close();
          SQLStatement.execute(ROLLBACK_SQL);
          SQLStatement.close();
          return "Cannot find unpaid reservation " + reservationId + " under user: " + userloggedIn + "\n";
        }
      } catch (SQLException e) {
        e.printStackTrace();
        return "Failed to pay for reservation " + reservationId + "\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    try {
      // YOUR CODE HERE
      if (userloggedIn == null)
        return "Cannot view reservations, not logged in\n";
      try {
        String selectReservationsSQL = "SELECT rid, fid1, fid2, one_hop, paid FROM Reservations WHERE username = \'" + userloggedIn + "\'";
        Statement SQLStatement = conn.createStatement();
        SQLStatement.execute(BEGIN_TRANSACTION_SQL);
        ResultSet results = SQLStatement.executeQuery(selectReservationsSQL);

        if (!results.isBeforeFirst() ) {    
          results.close();
          SQLStatement.execute(ROLLBACK_SQL);
          SQLStatement.close();
          return "No reservations found\n";
        } 
        ArrayList<Reservation> reservations = new ArrayList<>();
        while (results.next()) {
          int rid = results.getInt("rid");
          int fid1 = results.getInt("fid1");
          int fid2 = results.getInt("fid2");
          int one_hop = results.getInt("one_hop");
          boolean paid = results.getInt("paid") == 1;

          reservations.add(new Reservation(rid, fid1, fid2, one_hop, paid));
        }
        results.close();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < reservations.size(); i++) {
          Reservation r = reservations.get(i);
          sb.append("Reservation " + r.rid + " paid: " + r.paid + ":\n");
          if (r.one_hop == 1) {
            String selectFlightsSQL = "SELECT day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
              + "FROM Flights WHERE fid = " + r.fid1;
            ResultSet F1_results = SQLStatement.executeQuery(selectFlightsSQL);
            F1_results.next();
            int day_of_month = F1_results.getInt("day_of_month");
            String carrier_id = F1_results.getString("carrier_id");
            String flight_num = F1_results.getString("flight_num");
            String origin_city = F1_results.getString("origin_city");
            String dest_city = F1_results.getString("dest_city");
            int actual_time = F1_results.getInt("actual_time");
            int capacity = F1_results.getInt("capacity");
            int price = F1_results.getInt("price");

            F1_results.close();
            
            sb.append("ID: " + r.fid1 + " Day: " + day_of_month + " Carrier: " + carrier_id
              + " Number: " + flight_num + " Origin: " + origin_city + " Dest: " + dest_city
              + " Duration: " + actual_time + " Capacity: " + capacity + " Price: " + price + "\n");
          }
          // Two hop
          else {
            String selectFlightsSQL = "SELECT F1.day_of_month AS F1_day_of_month,F1.carrier_id AS F1_carrier_id,F1.flight_num AS F1_flight_num,F1.origin_city AS F1_origin_city,F1.dest_city AS F1_dest_city,F1.actual_time AS F1_actual_time,F1.capacity AS F1_capacity,F1.price AS F1_price,"
              + "F2.day_of_month AS F2_day_of_month,F2.carrier_id AS F2_carrier_id,F2.flight_num AS F2_flight_num,F2.origin_city AS F2_origin_city,F2.dest_city AS F2_dest_city,F2.actual_time AS F2_actual_time,F2.capacity AS F2_capacity,F2.price AS F2_price "
              + "FROM Flights F1, Flights F2 WHERE F1.fid = " + r.fid1 + " AND F2.fid = " + r.fid2;
            results = SQLStatement.executeQuery(selectFlightsSQL);
            results.next();
            int F1_dayOfMonth = results.getInt("F1_day_of_month");
            String F1_carrierId = results.getString("F1_carrier_id");
            String F1_flightNum = results.getString("F1_flight_num");
            String F1_originCity = results.getString("F1_origin_city");
            String F1_destCity = results.getString("F1_dest_city");
            int F1_time = results.getInt("F1_actual_time");
            int F1_capacity = results.getInt("F1_capacity");
            int F1_price = results.getInt("F1_price");

            int F2_dayOfMonth = results.getInt("F2_day_of_month");
            String F2_carrierId = results.getString("F2_carrier_id");
            String F2_flightNum = results.getString("F2_flight_num");
            String F2_originCity = results.getString("F2_origin_city");
            String F2_destCity = results.getString("F2_dest_city");
            int F2_time = results.getInt("F2_actual_time");
            int F2_capacity = results.getInt("F2_capacity");
            int F2_price = results.getInt("F2_price");

            results.close();

            sb.append("ID: " + r.fid1 + " Day: " + F1_dayOfMonth + " Carrier: " + F1_carrierId
              + " Number: " + F1_flightNum + " Origin: " + F1_originCity + " Dest: " + F1_destCity
              + " Duration: " + F1_time + " Capacity: " + F1_capacity + " Price: " + F1_price + "\n"
              + "ID: " + r.fid1 + " Day: " + F2_dayOfMonth + " Carrier: " + F2_carrierId
              + " Number: " + F2_flightNum + " Origin: " + F2_originCity + " Dest: " + F2_destCity
              + " Duration: " + F2_time + " Capacity: " + F2_capacity + " Price: " + F2_price + "\n");

          }
        }
        SQLStatement.execute(COMMIT_SQL);
        SQLStatement.close();
        return sb.toString(); 
      } catch (SQLException e) {
        e.printStackTrace();
        return "Failed to retrieve reservations\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n" For
   *         all other errors, return "Failed to cancel reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    try {
      // YOUR CODE HERE
      if (userloggedIn == null)
        return "Cannot cancel reservations, not logged in\n";
      try {
        String selectReservationsSQL = "SELECT fid1, fid2, one_hop, paid FROM Reservations "
          + "WHERE username = \'" + userloggedIn + "\' AND canceled = 0";
        Statement SQLStatement = conn.createStatement();
        SQLStatement.execute(BEGIN_TRANSACTION_SQL);
        ResultSet results = SQLStatement.executeQuery(selectReservationsSQL);
        if (results.next()) {
          int fid1 = results.getInt("fid1");
          int fid2 = results.getInt("fid2");
          int one_hop = results.getInt("one_hop");
          int paid = results.getInt("paid");
          results.close();
          // If paid already
          if (paid == 1) {
            String selectFlightsSQL = "SELECT price FROM Flights WHERE fid = " + fid1;
            results = SQLStatement.executeQuery(selectFlightsSQL);
            results.next();
            int totalPrice = results.getInt("price");
            results.close();

            // Two hop
            if (one_hop == 0) {
              selectFlightsSQL = "SELECT price FROM Flights WHERE fid = " + fid2;
              results.next();
              totalPrice += results.getInt("price");
              results.close();
            }
            String updateUsersString = "UPDATE Users SET balance = balance + " + totalPrice 
              + " WHERE username = \'" + userloggedIn + "\'";
            SQLStatement.execute(updateUsersString);
          }
          String updateReservationsSQL = "UPDATE Reservations SET canceled = 1 WHERE rid = " + reservationId;
          SQLStatement.execute(updateReservationsSQL);
          SQLStatement.execute(COMMIT_SQL);
          SQLStatement.close();
          return "Canceled reservation " + reservationId + "\n";
        }
        else {
          results.close();
          SQLStatement.execute(ROLLBACK_SQL);
          SQLStatement.close();
          return "Failed to cancel reservation " + reservationId + "\n";
        }
        
      } catch (SQLException e) {
        e.printStackTrace();
        return "Failed to cancel reservation " + reservationId + "\n";
      }
    } finally {
      checkDanglingTransaction();
    }
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  /**
   * Throw IllegalStateException if transaction not completely complete, rollback.
   * 
   */
  private void checkDanglingTransaction() {
    try {
      try (ResultSet rs = tranCountStatement.executeQuery()) {
        rs.next();
        int count = rs.getInt("tran_count");
        if (count > 0) {
          throw new IllegalStateException(
              "Transaction not fully commit/rollback. Number of transaction in process: " + count);
        }
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Database error", e);
    }
  }

  private static boolean isDeadLock(SQLException ex) {
    return ex.getErrorCode() == 1205;
  }

  /**
   * A class to store flight information.
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    public Flight(int fid, int dayOfMonth, String carrierId, String flightNum, String originCity, String destCity, int time, int capacity, int price) {
      this.fid = fid;
      this.dayOfMonth = dayOfMonth;
      this.carrierId = carrierId;
      this.flightNum = flightNum;
      this.originCity = originCity;
      this.destCity = destCity;
      this.time = time;
      this.capacity = capacity;
      this.price = price;
    }

    public Flight() {
      // Not a real flight
      this.time = 0;
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  /**
   * A class to store user information.
   */
  class User {
    public String username; // Primary key
    public String password;
    public int balance;

    @Override
    public String toString() {
      return "Username: " + username + " Password: " + password + " Balance: " + balance;
    }
  }

  /**
   * A class to store itinerary information.
   */
  class Itinerary implements Comparable<Itinerary> {
    public Flight flight1; // First flight
    public Flight flight2; // Second flight id if layover
    public boolean oneHop;
    public int totalTime;

    public Itinerary (Flight flight1, Flight flight2, boolean oneHop) {
      this.flight1 = flight1;
      this.flight2 = flight2;
      this.oneHop = oneHop;
      this.totalTime = flight1.time + flight2.time;
    }

    @Override
    public int compareTo(Itinerary i) {
      return Comparator.comparingInt((Itinerary itin)->itin.totalTime).thenComparingInt(itin->itin.flight1.fid).thenComparingInt(itin->itin.flight2.fid).compare(this,i);
    }
  }

  class Reservation {
    public int rid;          
    public int fid1;
    public int fid2;
    public int one_hop;
    public boolean paid;
    public Reservation (int rid, int fid1, int fid2, int one_hop, boolean paid) {
      this.rid = rid;
      this.fid1 = fid1;
      this.fid2 = fid2;
      this.one_hop = one_hop;
      this.paid = paid;
    }
  }
}