/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package threads;

/**
 *
 * @author prego
 */
public class QueueThread implements Runnable {

    private String command;
    
    public QueueThread(String command){
        this.command = command;
    }
    
    
    @Override
    public void run() {
        for(int i = 0; i < 1000000; i++){
            if(command.toLowerCase().equals("start")){
                System.out.println(i);
            }else{
                System.out.println("stoped...");
            }    
        }
    }
    
    
    
    
    
    
    

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }
    
    
}
