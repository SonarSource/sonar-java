import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.owasp.encoder.Encode;

public class GreetingServlet extends HttpServlet {

	private static final long serialVersionUID = -1883050705260497076L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		String unsafeName = req.getParameter("name");
		if (unsafeName != null) {
			String safeName = Encode.forHtml(unsafeName);

			req.setAttribute("unsafeName", unsafeName);
			req.setAttribute("safeName", safeName);
		} else {
			req.setAttribute("unsafeName", "World");
			req.setAttribute("safeName", "World");
		}

		RequestDispatcher requestDispatcher = req.getRequestDispatcher("views/greeting.jsp");
		requestDispatcher.forward(req, resp);
	}
}
