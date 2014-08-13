package nxt.http;

import nxt.util.Convert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class APITestServlet extends HttpServlet {

    private static final String header =
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\"/>\n" +
            "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" + 
            "    <title>Nxt http API</title>\n" +
            "    <link href=\"css/bootstrap.min.css\" rel=\"stylesheet\" type=\"text/css\" />" +
            "    <style type=\"text/css\">\n" +
            "        table {border-collapse: collapse;}\n" +
            "        td {padding: 10px;}\n" +
            "        .result {white-space: pre; font-family: monospace;}\n" +
            "    </style>\n" +
            "    <script type=\"text/javascript\">\n" +
            "        function submitForm(form) {\n" +
            "            var url = '/burst';\n" +
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
            "            var result = JSON.stringify(JSON.parse(request.responseText), null, 4);\n" +
            "            form.getElementsByClassName(\"result\")[0].textContent = result;\n" +
            "            return false;\n" +
            "        }\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div class=\"navbar navbar-default\" role=\"navigation\">" +
            "   <div class=\"container\">" + 
            "       <div class=\"navbar-header\">" +
            "           <a class=\"navbar-brand\" href=\"#\">Nxt http API</a>" + 
            "       </div>" +
            "       <div class=\"navbar-collapse collapse\">" +
            "           <ul class=\"nav navbar-nav navbar-right\">" +
            "               <li><a href=\"https://wiki.nxtcrypto.org/wiki/Nxt_API\" target=\"_blank\">Docs</a></li>" +
            "           </ul>" +
            "       </div>" +
            "   </div>" + 
            "</div>" +
            "<div class=\"container\">" +
            "<div class=\"row\">" +
            "<div class=\"col-xs-12\">" +
            "<div class=\"panel-group\" id=\"accordion\">";

    private static final String footer =
            "</div> <!-- panel-group -->" +
            "<br/><br/>" +
            "</div> <!-- col -->" +
            "</div> <!-- row -->" +
            "</div> <!-- container -->" +
            "<script src=\"js/3rdparty/jquery-2.1.0.js\"></script>" +
            "<script src=\"js/3rdparty/bootstrap.js\" type=\"text/javascript\"></script>" +
            "</body>\n" +
            "</html>\n";

    private static final List<String> requestTypes = new ArrayList<>(APIServlet.apiRequestHandlers.keySet());
    static {
        Collections.sort(requestTypes);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        resp.setContentType("text/html; charset=UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.print(header);
            String requestType = Convert.nullToEmpty(req.getParameter("requestType"));
            APIServlet.APIRequestHandler requestHandler = APIServlet.apiRequestHandlers.get(requestType);
            if (requestHandler != null) {
                writer.print(form(requestType, requestHandler.getParameters()));
            } else {
                for (String type : requestTypes) {
                    writer.print(form(type, APIServlet.apiRequestHandlers.get(type).getParameters()));
                }
            }
            writer.print(footer);
        }

    }

    private static String form(String requestType, List<String> parameters) {
        StringBuilder buf = new StringBuilder();
        buf.append("<div class=\"panel panel-default\">");
        buf.append("<div class=\"panel-heading\">");
        buf.append("<h4 class=\"panel-title\">");
        buf.append("<a data-toggle=\"collapse\" data-parent=\"#accordion\" href=\"#collapse");
        buf.append(requestType).append("\">");
        buf.append(requestType);
        buf.append("</a>");
        buf.append("</h4>");
        buf.append("</div> <!-- panel-heading -->");
        buf.append("<div id=\"collapse").append(requestType).append("\" class=\"panel-collapse collapse\">");
        buf.append("<div class=\"panel-body\">");
        buf.append("<form action=\"/burst\" method=\"POST\" onsubmit=\"return submitForm(this);\">");
        buf.append("<pre class=\"result\" style=\"float:right;width:50%;\">JSON response</pre>");
        buf.append("<input type=\"hidden\" name=\"requestType\" value=\"").append(requestType).append("\"/>");
        buf.append("<table class=\"table\" style=\"width:46%;\">");
        for (String parameter : parameters) {
            buf.append("<tr>");
            buf.append("<td>").append(parameter).append(":</td>");
            buf.append("<td><input type=\"");
            buf.append("secretPhrase".equals(parameter) ? "password" : "text");
            buf.append("\" name=\"").append(parameter).append("\" style=\"width:200px;\"/></td>");
            buf.append("</tr>");
        }
        buf.append("<tr>");
        buf.append("<td colspan=\"2\"><input type=\"submit\" class=\"btn btn-default\" value=\"submit\"/></td>");
        buf.append("</tr>");
        buf.append("</table>");
        buf.append("</form>");
        buf.append("</div> <!-- panel-body -->");
        buf.append("</div> <!-- panel-collapse -->");
        buf.append("</div> <!-- panel -->");
        return buf.toString();
    }

}
