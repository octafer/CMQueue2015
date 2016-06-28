/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author user
 */
public class MySQLConnIFlow {

    private String host;
    private String user;
    private String pass;
    private String database;
    public Connection c;
    boolean isConnected;

    public MySQLConnIFlow(String url){
        this.host = "localhost";
        isConnected = false;
        //url = "jdbc:mysql://192.168.1.59:3337/Zahara";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            System.out.println(url);
            //this.c = (Connection) DriverManager.getConnection(url,"paulo.rego","pankeca");
            //this.c = (Connection) DriverManager.getConnection(url,"paulo.rego","pankeca");
            //this.c = (Connection) DriverManager.getConnection(url,"root","15375900");
            this.c = (Connection) DriverManager.getConnection(url,"cma_zahara","ZaharaC#M#A_ZA");
            isConnected = true;
        } catch( SQLException e ) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            isConnected = false;
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            isConnected = false;
        } catch ( InstantiationException e ) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            isConnected = false;
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            isConnected = false;
        }
    }




    public MySQLConnIFlow(){
        isConnected = false;
        String url;

        url = "jdbc:mysql://192.168.1.26:3337/iflow?autoReconnect=true";
        //url = "jdbc:mysql://10.101.8.17:3306/iflow?autoReconnect=true";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            //System.out.println(url);
            

            //this.c = (Connection) DriverManager.getConnection(url,"root","15375900");
            this.c = (Connection) DriverManager.getConnection(url,"root","Mysecret1234");
            isConnected = true;
        } catch( SQLException e ) {
            System.out.println(e.getMessage());
            isConnected = false;
        } catch ( ClassNotFoundException e ) {
            System.out.println(e.getMessage());
            isConnected = false;
        } catch ( InstantiationException e ) {
            System.out.println(e.getMessage());
            isConnected = false;
        } catch ( IllegalAccessException e ) {
            System.out.println(e.getMessage());
            isConnected = false;
        }
    }

    public boolean isConnected(){ return isConnected; }

    public Connection getConnection(){ return c; }

    public void closeConnection(){
        try { c.close(); } catch (SQLException ex) { Logger.getLogger(MySQLConnIFlow.class.getName()).log(Level.SEVERE, null, ex); }
    }

    public ResultSet search(String sql) throws SQLException{
       PreparedStatement ps; 
       ps = c.prepareStatement(sql);
       return ps.executeQuery(); 
    }

    public int delete(String sql) throws SQLException{
       PreparedStatement ps;
       ps = c.prepareStatement(sql);
       return ps.executeUpdate(); 
    }

    public int insert(String sql) throws SQLException{
       PreparedStatement ps;
       ps = c.prepareStatement(sql);
       return ps.executeUpdate();
    }

    public int insert(String sql, InputStream dadosBIN) throws SQLException, IOException{
       PreparedStatement ps;
       ps = c.prepareStatement(sql);
       ps.setBinaryStream(1,dadosBIN); //,dadosBIN.available()
       return ps.executeUpdate();
    }


    public int update(String sql) throws SQLException{
       PreparedStatement ps;
       ps = c.prepareStatement(sql);
       return ps.executeUpdate();
    }

    public int update(String sql, InputStream dadosBIN) throws SQLException, IOException{
       PreparedStatement ps;
       ps = c.prepareStatement(sql);
       ps.setBinaryStream(1,dadosBIN); //,dadosBIN.available()
       return ps.executeUpdate();
    }


    public long getLastInsertID(){
        try {
            ResultSet rs = search("select LAST_INSERT_ID()");
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            Logger.getLogger(MySQLConnIFlow.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }


    public void setAutoCommit(boolean isAutoCommit) throws SQLException{
        c.setAutoCommit(isAutoCommit);
    }

    public void rollBack() throws SQLException{ c.rollback(); }
    public void commit() throws SQLException{ c.commit(); }




}