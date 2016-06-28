/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scheduler;

import db.MyConnection;
import db.MySQLConnIFlow;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author prego
 */
public class MyScheduler {
    
    private SchedulerFactory schedFact;
    private Scheduler sched;
    private JobDetail job;
    private Trigger trigger;
    private MyConnection conn;
    
    
    public MyScheduler(){}
    
    
    
    public void startScheduler(){
        try {
            
            JobKey jobKey = new JobKey("myTeste", "group1");
            
            job = JobBuilder.newJob(MyJob.class).withIdentity("myTeste", "group1").build();
            //CronScheduleBuilder.cronSchedule("0/1 * * * * ?")
            trigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity("myTrigger", "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(5000).repeatForever()
                    )
                    .build();
                    //.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(500).withRepeatCount(1))
            
            //job.getJobDataMap().put("serverNoSQL", conn);
            
            sched = new StdSchedulerFactory().getScheduler();
            
            sched.start();
            sched.scheduleJob(job, trigger);
             
        } catch (SchedulerException ex) {
            Logger.getLogger(MyScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void suspendScheduler(){
        try {
            
            sched.pauseJob(job.getKey());
            
        } catch (SchedulerException ex) {
            Logger.getLogger(MyScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void resumeScheduler(){
        try {
            
            sched.resumeJob(job.getKey());
            trigger.getNextFireTime(); 
            
        } catch (SchedulerException ex) {
            Logger.getLogger(MyScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void stopScheduler(){
        try {
            
            sched.shutdown();
            
        } catch (SchedulerException ex) {
            Logger.getLogger(MyScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    
    
}
