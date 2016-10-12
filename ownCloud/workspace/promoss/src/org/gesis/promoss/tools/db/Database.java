package org.gesis.promoss.tools.db;


import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.gesis.promoss.tools.text.Text;


//Code teilweise von Giri Mandalika, http://java.sun.com/developer/technicalArticles/mysql_java/

public class Database {


	
	Text db_config = new Text();
	//configuration file is in /home/$USER/.db_config, it consists of three lines: user name, user password, database name
	String config_location = "/home/" + System.getProperty("user.name") + "/.db_config";
	
	private String userName = null;
	private String userPassword = null;
	private String dbName = null;


	private Connection conn = null;

	private Statement stmt = null;

	public ResultSet rs = null;

	public String valueString = null;
	public String valueStringBuffer = "";
	public String nameString = null;
	public String nameStringBuffer = null;

	static private SimpleDateFormat mysqlFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	//this constructor establishes a connection to the mySQL-server
	public Database() { 

		
		
		if (!new File(config_location).exists()) {
			System.out.println("Can not find database configuration file under " + config_location);
			System.out.println("Please create a .db_config that consists of three lines: user name, user password, database name");
		}
		
		userName = db_config.readLine(config_location); //user name is in line 1
		userPassword = db_config.readLine(config_location); //user password is in line 2
		dbName = db_config.readLine(config_location); //database name is in line 3

		try {
			//    	Load JDBC-Driver

			Class.forName("com.mysql.jdbc.Driver").newInstance(); 
			try {
				//        	Connect to the DB

				conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+dbName,userName,userPassword);
				stmt = conn.createStatement();

			}
			catch (SQLException ex) {
				System.out.println("SQLException3: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
				close();
			}
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
	}



	public Database(String type) { 

	if (type.equals("estrella")) {

			try {
				//    	Load JDBC-Driver
				Class.forName("com.mysql.jdbc.Driver").newInstance(); 
				try {
					//        	Connect to the DB
					conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+dbName,userName,userPassword);
					stmt = conn.createStatement();

				}
				catch (SQLException ex) {
					System.out.println("SQLException3: " + ex.getMessage());
					System.out.println("SQLState: " + ex.getSQLState());
					System.out.println("VendorError: " + ex.getErrorCode());
					close();
				}
			}
			catch (Exception ex) {
				System.out.println(ex);
			}

		}

	}
	
	public Database(String server, String port, String user, String password, String database_name) {
		
		dbName = database_name; //database name
		userName = user;
		userPassword = password; //user password
		
		try {
			//    	Load JDBC-Driver

			Class.forName("com.mysql.jdbc.Driver").newInstance(); 
			try {
				//        	Connect to the DB

				conn = DriverManager.getConnection("jdbc:mysql://"+server+":"+port+"/"+dbName,userName,userPassword);
				stmt = conn.createStatement();

			}
			catch (SQLException ex) {
				System.out.println("SQLException: jdbc:mysql://"+server+":"+port+"/"+dbName);
				System.out.println("SQLException: " + ex.getMessage());
				System.out.println("SQLState: " + ex.getSQLState());
				System.out.println("VendorError: " + ex.getErrorCode());
				close();
			}
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
		
	}

	//Surprise: close() closes the connections, statements and results of mySQL
	public void close() { 

		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException sqlEx) {
				System.out.println("SQLException1: " + sqlEx.getMessage());
			}
			rs = null;
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException sqlEx) {
				System.out.println("SQLException2: " + sqlEx.getMessage());
			}

			stmt = null;
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException sqlEx) {
				// Ignore
			}

			conn = null;
		}
	}

	//here we go with the functions

	public void executeUpdate(String update)  {

		try {   
			if (update != null) {
				stmt.executeUpdate(update);
			}
			else {
				stmt.executeUpdate(valueString);
			}

		}
		catch (SQLException ex) {

			close();

			System.out.println("SQLException3: " + ex.getMessage() + 
					"\nSQLState: " + ex.getSQLState() + 
					"\nVendorError: " + ex.getErrorCode() +
					"\nQuery was: " + update
					); 

		}


	}

	public int executeUpdateKey(String update) {

		try {   

			if (update != null) {

				stmt.executeUpdate(update,Statement.RETURN_GENERATED_KEYS);
			}
			else {
				stmt.executeUpdate(valueString);
			}

			ResultSet rsGen = stmt.getGeneratedKeys();
			if (rsGen.next()) {
				return rsGen.getInt(1);
			}

		}
		catch (SQLException ex) {

			close();

			System.out.println("SQLException3: " + ex.getMessage() + 
					"\nSQLState: " + ex.getSQLState() + 
					"\nVendorError: " + ex.getErrorCode() +
					"\nQuery was: " + update
					); 

		}
		return -1;

	}

	public void updateInto(String command,String name) {

		//System.out.println(command + " INTO " + name + " " + nameString + ") " + valueString + ")");

		this.executeUpdate(command + " INTO " + name + " " + nameString + ") VALUES " + valueString + ")");

		nameString = null;
		valueString = null;

	}

	public void clean() {

		nameString = null;
		valueString = null;

	}


	public int updateIntoKey(String command,String name) {

		//System.out.println("REPLACE INTO " + name + " " + nameString + ") " + valueString + ")");

		int result = this.executeUpdateKey(command + " INTO " + name + " " + nameString + ") VALUES " + valueString + ")");

		nameString = null;
		valueString = null;

		return result;

	}


	public void updateIntoBuffer() {

		if (!valueStringBuffer.equals(""))
			valueStringBuffer = valueStringBuffer + ",";

		valueStringBuffer += valueString+")";
		if (nameStringBuffer == null)
			nameStringBuffer = nameString;
		nameString = null;
		valueString = null;

	}

	public void updateIntoCommit(String command,String name) {

		//System.out.println("REPLACE INTO " + name + " " + nameString + ") " + valueString + ")");

		if (nameStringBuffer != null)
			this.executeUpdate(command + " INTO " + name + " " + nameStringBuffer + ") VALUES " + valueStringBuffer);

		nameString = null;
		valueString = null;
		valueStringBuffer = "";
		nameStringBuffer = null;

	}




	public void addValueDirect(String name, String value) {
		if (nameString == null) {
			nameString = "(";
			valueString = "VALUES(";
		}
		else {
			nameString = nameString.concat(",");
			valueString = valueString.concat(",");
		}
		nameString = nameString.concat(name);

		if (value != null) {
			valueString = valueString.concat(value);
		}
		else {
			valueString = valueString.concat("null");
		}
	}

	public void addValue(String name, Date value) {
		String valueString = mysqlFormat.format(value);
		if (value.after(new GregorianCalendar(1970, 1, 1).getTime()) && value.before(new GregorianCalendar(2015, 1, 1).getTime())) {
			this.addValue(name, valueString);
		}
	}

	public void addValue(String name, int value) {
		String valueString = String.valueOf(value);
		this.addValue(name, valueString);
	}

	public void addValue(String name, double value) {
		String valueString = String.valueOf(value);
		this.addValue(name, valueString);
	}

	public void addValue(String name, float value) {
		String valueString = String.valueOf(value);
		this.addValue(name, valueString);
	}

	public void addValue(String name, long value) {
		String valueString = String.valueOf(value);
		this.addValue(name, valueString);
	}


	public void addValue (String name, String value) {

		if (value != null) {
			value = value.replace("\\", "\\\\");
			value = value.replace("'", "\\'");
		}

		if (nameString == null) {
			nameString = "(";
			valueString = " (";
		}
		else {
			nameString = nameString.concat(",");
			valueString = valueString.concat(",");
		}
		nameString = nameString.concat(name);

		if (value != null) {
			valueString = valueString.concat("'");
			valueString = valueString.concat(value);
			valueString = valueString.concat("'");
		}
		else {
			valueString = valueString.concat("null");
		}

	}

	public void executeQuery(String query) {

		try {   


			rs = stmt.executeQuery(query);


		}
		catch (SQLException ex) {
			close();
			System.out.println("SQLException3: " + ex.getMessage() + 
					"\nSQLState: " + ex.getSQLState() + 
					"\nVendorError: " + ex.getErrorCode() +
					"\nQuery was: " + query
					); 

		}


	}

	public String printQuery(String input, String separator) throws SQLException {

		return printQuery(input,null,separator);

	}

	public String printQuery(String input, String separator,String lineBreak) throws SQLException {

		String output = "";
		this.executeQuery(input);
		boolean firstLine = true;
		while (this.rs.next()) {

			if (!firstLine) {
				output+=lineBreak;
			}
			else {
				firstLine = false;
			}


			int columnCount = rs.getMetaData().getColumnCount();
			boolean first = true;

			for (int i = 1; i <= columnCount; i++) {

				if (!first) {
					output+=separator;
				}
				else {
					first = false;
				}

				output+=this.rs.getString(i);

			}

		}
		return output;

	}



} // MySQLclient
