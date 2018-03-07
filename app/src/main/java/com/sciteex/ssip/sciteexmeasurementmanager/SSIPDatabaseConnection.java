package com.sciteex.ssip.sciteexmeasurementmanager;

/**
 * Created by Gajos on 9/28/2017.
 */

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

public class SSIPDatabaseConnection
{
    private class HTMLStrings
    {
        public final static String MIME_TYPE = "text/html";

        public final static String ENCODING = "UTF-8";

        public final static String HTML_TABLE_BEGIN = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<style>\n" +
                "table {\n" +
                "    font-family: arial, sans-serif;\n" +
                "    border-collapse: collapse;\n" +
                "    width: 100%;\n" +
                "}\n" +
                "\n" +
                "td, th {\n" +
                "    border: 1px solid #dddddd;\n" +
                "    text-align: left;\n" +
                "    padding: 8px;\n" +
                "}\n" +
                "\n" +
                "tr:nth-child(even) {\n" +
                "    background-color: #dddddd;\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>";

        public final static String HTML_TABLE_END = "\n" +
                "</body>\n" +
                "</html>";
    }

    private class MSSQLStrings
    {
        public final static String CONNECTION_PREFIX = "jdbc:sqlserver://";

        public final static String CLASS_STRING = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    private class MySQLStrings
    {
        public final static String CONNECTION_PREFIX = "jdbc:mysql://";

        public final static String CLASS_STRING = "com.mysql.jdbc.Driver";
    }

    private static SSIPDatabaseConnection singletonInstance = new SSIPDatabaseConnection();

    private SSIPDatabaseConnection()
    {

    }

    public static SSIPDatabaseConnection getSingletonInstance()
    {
        return singletonInstance;
    }


    private final static String ADD_COMMENT_QUERY = "INSERT INTO MeasureComments (job_id, program_number, measure_number, person_id, comment) VALUES (?,?,?,?,?)";


    //Variables needed to connection with server.

    /**
     * URL to connection with database.
     */
    private String connectionURL;

    /**
     * Database user
     */
    private String databaseServer;

    private String databaseName;

    private String databaseUser;

    private String databasePassword;

    private String databaseType;

    public String getConnectionUrl()
    {
        return this.connectionURL;
    }

    public String getDatabaseUser()
    {
        return this.databaseUser;
    }

    public String getDatabasePassword()
    {
        return this.databasePassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseType()
    {
        return this.databaseType;
    }

    //Setters
    public void setConnectionURL(String url)
    {
        this.connectionURL = url;
    }

    public void setDatabaseUser(String user)
    {
        this.databaseUser = user;
    }

    public void setDatabasePassword(String password)
    {
        this.databasePassword = password;
    }

    public void setDatabaseName(String name)
    {
        this.databaseName = name;
    }

    public void setDatabaseServer(String server)
    {
        this.databaseServer = server;
    }

    public void setDatabaseType(String type)
    {
        this.databaseType = type;
    }

    public static void startSingleton() throws SQLException
    {
        singletonInstance.start();
    }


    /**
     * Default ssip user.
     */
    private final static String DEFAULT_SSIP_USER = "";

    public static String getDefaultSSIPUser()
    {
        return DEFAULT_SSIP_USER;
    }

    /**
     * Default ssip password.
     */
    private final static String DEFAULT_SSIP_PASSWORD = "";

    public static String getDefaultSSIPPasswor()
    {
        return DEFAULT_SSIP_PASSWORD;
    }

    /**
     * Connection to database.
     */
    Connection conn;

    /**
     * Prepared statement for sending comments.
     */
    PreparedStatement stmt;

    /**
     * Method to connect with database
     */

    private boolean start() throws SQLException
    {
        try {
            switch (databaseType) {
                case "MSSQL":
                    dbConnect(MSSQLStrings.CLASS_STRING, MSSQLStrings.CONNECTION_PREFIX);
                    return true;
                case "MYSQL":
                    dbConnect(MySQLStrings.CLASS_STRING, MySQLStrings.CONNECTION_PREFIX);
                    return true;
            }
        }
        catch(ClassNotFoundException e)
        {
            Log.e("ERROR", "Class Exception: ", e);
        }
        return false;
    }

    public SQLQueryObject createNewQuery(String title, String body)
    {
        try {
            return new SQLQueryObject(title, conn.prepareStatement(body));
        } catch (SQLException e)
        {
            return null;
        }
    }

    private void dbConnect(String class_string, String prefix_string) throws SQLException, ClassNotFoundException
    {
        connectionURL = prefix_string + databaseServer + "/" + databaseName;
        try {
            Class.forName(class_string).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Runnable r = new Runnable() {
            @Override
            public void run(){
                try
                {
                    conn = DriverManager.getConnection(connectionURL,
                            databaseUser, databasePassword);
                    makeNotification(MeasurementManager.thisActivity.getString(R.string.connected_with_database), MeasurementManager.thisActivity.getString(R.string.successfully_connected_with) + getDatabaseName(),999);

                }catch(SQLException e)
                {
                    makeNotification(MeasurementManager.thisActivity.getString(R.string.sql_error), e.getLocalizedMessage(),999);
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    private static void makeNotification(String title, String content, int position)
    {
        NotificationManager mNotificationManager =
                (NotificationManager) MeasurementManager.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MeasurementManager.applicationContext)
                .setSmallIcon(R.mipmap.sciteex_logo_icon)
                .setLargeIcon(BitmapFactory.decodeResource(MeasurementManager.applicationContext.getResources(),
                        R.mipmap.sciteex_logo_icon))
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(PRIORITY_MAX);
        mNotificationManager.notify(position, mBuilder.build());
    }

    public String showTable(SQLQueryObject query)
    {
        StringBuffer buffer = new StringBuffer(HTMLStrings.HTML_TABLE_BEGIN);
        try {
            ResultSet rs = query.getStatement().executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            buffer.append("<table>\n" + "<tr>\n");

            //Init header
            for(int i = 0; i < rsmd.getColumnCount(); ++i)
            {
                buffer.append("<th>");
                buffer.append(rsmd.getColumnName(i));
                buffer.append("</th>\n");
            }
            buffer.append("</tr>\n");

            //Init body of table.
            while (rs.next())
            {
                buffer.append("<tr>\n");

                for(int i = 0; i < rsmd.getColumnCount(); ++i)
                {
                    buffer.append("<td>");
                    buffer.append(rs.getString(i));
                    buffer.append("</td>\n");
                }
                buffer.append("</tr>\n");
            }
            //Close table.
            buffer.append("</table>");
        } catch (SQLException e) {
            buffer.append("echo \"" + e.getSQLState() + "\";");
            e.printStackTrace();
        } finally {
            buffer.append(HTMLStrings.HTML_TABLE_END);
            return buffer.toString();
        }
    }


    public void sendComments (List<List<String>> commentsList, int jobId, int personId) throws SQLException
    {
        try {
            stmt = conn.prepareStatement(ADD_COMMENT_QUERY);
            conn.setAutoCommit(false);

            //Set constant values of prepared statement.
            stmt.setInt(1, jobId);
            stmt.setInt(4, personId);

            for (int i = 0; i < commentsList.size(); ++i)
            {
                for (int j = 0; j < commentsList.get(i).size(); ++j)
                {
                    String s = commentsList.get(i).get(j);
                    if("".equals(s))
                    {
                        //Set preparedStatement values.
                        stmt.setInt(2, i);
                        stmt.setInt(3, j);
                        stmt.setString(5, commentsList.get(i).get(j));
                        stmt.executeUpdate();
                        conn.commit();
                    }
                }
            }
        } catch (SQLException e ) {
            if (conn != null) {
                try {
                    System.err.print("Transaction is being rolled back");
                    conn.rollback();
                } catch(SQLException excep) {
                    throw e;
                }
            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }

                conn.setAutoCommit(true);
            }catch(SQLException e)
            {
                throw e;
            }
        }
    }

    public void close() throws SQLException {

        conn.close();
    }
}