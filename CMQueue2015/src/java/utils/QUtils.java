/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.primefaces.context.RequestContext;

/**
 *
 * @author prego
 */
public abstract class QUtils {
   
    
    
    public static Pattern myPattern(String expr){
        expr = expr.toLowerCase().replaceAll("a|à|á|ã|â|A|À|Á|Ã|Â", "[aáàãâAÁÀÃÂ]").replaceAll("e|è|é|ê|E|È|É|Ê", "[eéèêEÉÈÊ]").replaceAll("i|ì|í|î|I|Ì|Í|Î", "[iìíîIÌÍÎ]").replaceAll("o|ò|ó|õ|ô|O|Ò|Ó|Õ|Ô", "[oóòôõOÓÒÔÕ]").replaceAll("u|ù|ú|û|U|Ù|Ú|Û", "[uúùûUÚÙÛ]").replaceAll("c|ç|C|Ç", "[cçCÇ]");
        Pattern regex = Pattern.compile(expr, Pattern.CASE_INSENSITIVE);
        
        return regex;
    }
    
    
    
    public static void atualizarBean(String obj){
        RequestContext.getCurrentInstance().update(obj);
    }
    
    
    
    
    
    
    
    
    public static void printMessageThread(FacesContext context, String message1, String message2){
        context.addMessage(null, new FacesMessage(message1, message2));
    }
    
    
    public static void printMessage(String message1, String message2){
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(message1, message2));
    }
    
    
    public static String[] getDateByStr(String desc) {
        int count=0;
        String[] allMatches = new String[2];
        Matcher m = Pattern.compile("(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d").matcher(desc);
        while (m.find()) {
            allMatches[count] = m.group();
            count++;
        }
        if(count==0){
            
            m = Pattern.compile("(19|20)\\d\\d[- /.](0[1-9]|1[012])[- /.](0[1-9]|[12][0-9]|3[01])").matcher(desc);
            while (m.find()) {
                allMatches[count] = m.group();
                count++;
            }
            
            if(count==0){
                return null;
            }else{
                return allMatches;
            }   
            
        }else{
            return allMatches;
        }    
    }
    
    
    public static String getTermoPrincipal(String expr, Map<String, String> mymap){
        return mymap.get(expr);
    }
    
    
    public static void executeJS(String comando){
        RequestContext.getCurrentInstance().execute(comando);
    }
    
    
    
}
