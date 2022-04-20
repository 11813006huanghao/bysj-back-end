import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/morning")
public class MorningServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
        req.getRequestDispatcher("/").forward(req,rsp);
    }
}
