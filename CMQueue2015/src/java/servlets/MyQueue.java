/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author prego
 */
public class MyQueue extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet MyQueue</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet MyQueue at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
            
            
            //Create a InitContext instance
	    InitialContext context = new InitialContext();
	 
	    //Obtaining a default ManagedExecutorService
	    //Using the java:comp/DefaultManagedExecutorService
	    ManagedExecutorService executorService = (ManagedExecutorService) context.lookup("java:comp/DefaultManagedExecutorService");
	 
	    out.println("MethodExecutorService#submit(Callable)<br/>");
	    //Using the submit method to submit a Callable task.
	    Future<String> result = executorService.submit(new Callable<String>() {
	      @Override
	      public String call() throws Exception {
	        StringBuilder builder = new StringBuilder();
	        for (int i = 0; i < 10; i++) {
	          builder.append("String number ").append(i).append("<br/>");
	        }
	 
	        return builder.toString();
	      }
	    });
	 
	    //retrieving the result from the callable task completion.
	    out.println("Output from the Callable invoked using <code>submit()</code>: <br/>" + result.get());
	 
	    //Using the submit method to submit a Runnable task which doesn't have any return value.
    out.println("ManagedExecutorService#submit(Runnable)<br/>");
	    executorService.submit(new Runnable() {
	      @Override
	      public void run() {
	        for (int i = 0; i < 10; i++) {
	          out.println("String number " + i+"<br/>");
	        }
	      }
	    });

            
            
            
        } catch (NamingException ex) {
            Logger.getLogger(MyQueue.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(MyQueue.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(MyQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
