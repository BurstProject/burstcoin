package nxt.http;

import nxt.db.Db;
import nxt.util.Convert;
import org.h2.tools.Shell;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;

public final class DbShellServlet extends HttpServlet {

    private static final String header =
            "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\"/>\n" +
                    "    <title>Nxt H2 Database Shell</title>\n" +
                    "    <script type=\"text/javascript\">\n" +
                    "        function submitForm(form) {\n" +
                    "            var url = '/dbshell';\n" +
                    "            var params = '';\n" +
                    "            for (i = 0; i < form.elements.length; i++) {\n" +
                    "                if (! form.elements[i].name) {\n" +
                    "                    continue;\n" +
                    "                }\n" +
                    "                if (i > 0) {\n" +
                    "                    params += '&';\n" +
                    "                }\n" +
                    "                params += encodeURIComponent(form.elements[i].name);\n" +
                    "                params += '=';\n" +
                    "                params += encodeURIComponent(form.elements[i].value);\n" +
                    "            }\n" +
                    "            var request = new XMLHttpRequest();\n" +
                    "            request.open(\"POST\", url, false);\n" +
                    "            request.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n" +
                    "            request.send(params);\n" +
                    "            form.getElementsByClassName(\"result\")[0].textContent += request.responseText;\n" +
                    "            return false;\n" +
                    "        }\n" +
                    "    </script>\n" +
                    "</head>\n" +
                    "<body>\n";

    private static final String footer =
                    "</body>\n" +
                    "</html>\n";

    private static final String form =
            "<form action=\"/dbshell\" method=\"POST\" onsubmit=\"return submitForm(this);\">" +
                    "<table class=\"table\" style=\"width:90%;\">" +
                    "<tr><td><pre class=\"result\" style=\"float:top;width:90%;\">" +
                    "This is a database shell. Enter SQL to be evaluated, or \"help\" for help:" +
                    "</pre></td></tr>" +
                    "<tr><td><b>&gt;</b> <input type=\"text\" name=\"line\" style=\"width:90%;\"/></td></tr>" +
                    "</table>" +
                    "</form>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        if (API.allowedBotHosts != null && ! API.allowedBotHosts.contains(req.getRemoteHost())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (PrintStream out = new PrintStream(resp.getOutputStream())) {
            out.print(header);
            out.print(form);
            out.print(footer);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        if (API.allowedBotHosts != null && ! API.allowedBotHosts.contains(req.getRemoteHost())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String line = Convert.nullToEmpty(req.getParameter("line"));
        try (PrintStream out = new PrintStream(resp.getOutputStream())) {
            out.println("\n> " + line);
            try {
                Shell shell = new Shell();
                shell.setErr(out);
                shell.setOut(out);
                shell.runTool(Db.getConnection(), "-sql", line);
            } catch (SQLException e) {
                out.println(e.toString());
            }
        }
    }

}
