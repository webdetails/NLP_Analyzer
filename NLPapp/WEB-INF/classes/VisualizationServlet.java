import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

import org.json.JSONObject;
import org.json.JSONException;
import hitachi.vantara.nlp.NaturalLanguageVis;

public class VisualizationServlet extends HttpServlet{
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    }
    
    @Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        BufferedReader br = request.getReader();

        File schema = new File("schema.xml");

        FileOutputStream fos = new FileOutputStream(schema);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        String line = null;

        while ((line = br.readLine())!=null) {

            bw.write(line);
            bw.newLine();
        
        }
        bw.close();
        br.close();
   
		String query = request.getParameter("query");

        JSONObject json = NaturalLanguageVis.run(query, schema);

        schema.delete();

        response.getOutputStream().println(json.toString());
	}

}