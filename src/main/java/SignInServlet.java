import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = "/signin")
public class SignInServlet extends HttpServlet {
    private Map<String, String> users = new HashMap<String,String>(){{put("a","b");}};

    protected void doGet(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        rsp.setContentType("text/html");
        PrintWriter pw= rsp.getWriter();
        pw.write("<h1>Sign In</h1>");
        pw.write("<form action=\"/signin\" method=\"post\">");
        pw.write("<p>Username: <input name=\"username\"></p>");
        pw.write("<p>Password: <input name=\"password\" type=\"password\"></p>");
        pw.write("<p><button type=\"submit\">Sign In</button> <a href=\"/\">Cancel</a></p>");
        pw.write("</form>");
        pw.flush();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        String userName=req.getParameter("username");
        String password=req.getParameter("password");
        String expectedPwd=users.get(userName);
        if(expectedPwd!=null && expectedPwd.equals(password)){
            req.getSession().setAttribute("userName",userName);
            PrintWriter pw= rsp.getWriter();
            pw.write("hhh");
            pw.flush();
        }else{
            rsp.sendError(403);
        }
    }
}
