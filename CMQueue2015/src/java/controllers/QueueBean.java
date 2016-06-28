/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import db.MyConnection;
import db.MySQLConnIFlow;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;
import scheduler.MyScheduler;
import utils.QUtils;

/**
 *
 * @author prego
 */
@ManagedBean
@ApplicationScoped
public class QueueBean {

    
    private String comando;
    private MyScheduler mys;
    private String pass;
    private MyConnection conn;
    private MySQLConnIFlow sqlconn;
    
    
    /**
     * Creates a new instance of QueueBean
     */
    public QueueBean() {
        comando = "";
    }
    
    
    
    
    public void verifyPass(){
        if(pass.equals("15375910")){
            QUtils.executeJS("PF('dlgDesb').hide();");
        }else{
            QUtils.printMessage("Quase lá...", "Tó tó :-P");
        }
    }
    
    
    public void windowManager(){
            QUtils.printMessage("Sistema fechado!", "");
            QUtils.executeJS("PF('dlgDesb').show();");
            comando="";
    }
    
    
    
    public void serverManager(){
        if(comando.toLowerCase().equals("start")){
            iniciarProcessamento();
            mys.startScheduler();
        }else if(comando.toLowerCase().equals("stop")){
            pararProcessamento();
            mys.stopScheduler();
        }else if(comando.toLowerCase().equals("resume")){
            arrancarProcessamento();
            mys.resumeScheduler();
        }else{
            suspenderProcessamento();
            mys.suspendScheduler();
        }  
    }
    
    
    public void iniciarProcessamento(){
        QUtils.printMessage("Iniciou...", "");
        mys = new MyScheduler();
    }
    
    
    public void pararProcessamento(){
        QUtils.printMessage("Parou...", "");
    }
    
    
    public void suspenderProcessamento(){
        QUtils.printMessage("Suspenso...", "");
    }
    
    public void arrancarProcessamento(){
        QUtils.printMessage("Voltar a arrancar...", "");
    }
    
    
    
    
    /**
     * @return the comando
     */
    public String getComando() {
        return comando;
    }

    /**
     * @param comando the comando to set
     */
    public void setComando(String comando) {
        this.comando = comando;
    }

    
    /**
     * @return the pass
     */
    public String getPass() {
        return pass;
    }

    /**
     * @param pass the pass to set
     */
    public void setPass(String pass) {
        this.pass = pass;
    }

    /**
     * @return the conn
     */
    public MyConnection getConn() {
        return conn;
    }

    /**
     * @param conn the conn to set
     */
    public void setConn(MyConnection conn) {
        this.conn = conn;
    }

    /**
     * @return the sqlconn
     */
    public MySQLConnIFlow getSqlconn() {
        return sqlconn;
    }

    /**
     * @param sqlconn the sqlconn to set
     */
    public void setSqlconn(MySQLConnIFlow sqlconn) {
        this.sqlconn = sqlconn;
    }
    
}
