package pl.net.bluesoft.rnd.processtool.editor.platform.filter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.signavio.platform.core.Platform;
import com.signavio.platform.core.PlatformProperties;
import com.signavio.platform.servlets.DispatcherServlet;

/**
 * Filter to provide authentication between modeler webapp and aperte runtime environment
 */
public class AperteTokenFilter implements Filter {

    private static final String APERTE_TOKEN_ATTRIBUTE_NAME = "aperteToken";
	
	private FilterConfig filterConfig;
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        HttpSession session = req.getSession();
        if (session.getAttribute(APERTE_TOKEN_ATTRIBUTE_NAME) == null) {
            PlatformProperties props = Platform.getInstance().getPlatformProperties();
            if (req.getParameter("token") == null) {
                //redirect to token generation url
                res.sendRedirect(props.getJbpmGuiUrl() + "/g_token?returl=" + req.getRequestURL() +
                        (req.getRequestURL().indexOf("?") != -1 ? "&" : "?") +
                        "token=");
            } else {
                //check token using background channel
                URL u = new URL(props.getServerName() + props.getJbpmGuiUrl() + "/v_token?token=" + req.getParameter("token"));
                filterConfig.getServletContext().log("url: "+u);
                
                //it has to be url connection!
                HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
                
                filterConfig.getServletContext().log("urlConnection: "+urlConnection);
                
                filterConfig.getServletContext().log("getResponseCode: "+urlConnection.getResponseCode());
                
                if (urlConnection.getResponseCode() == 200) {
                    String login = "";
                    InputStream is = urlConnection.getInputStream();
                    int c= 0;
                    while ((c = is.read()) >= 0) {
                        login += (char)c;
                    }
                    session.setAttribute(APERTE_TOKEN_ATTRIBUTE_NAME, login);

                    filterConfig.getServletContext().log("AperteTokenFilter complete");
                    
                    filterChain.doFilter(req, res);
                } 
                else 
                {
                	filterConfig.getServletContext().log("AperteTokenFilter no redirect");
                    res.setStatus(401);//do not redirect, this may result in infinite loop and server ddos
                }

            }

        } else {
            // do forward
            filterChain.doFilter(req, res);
        }

    }

    @Override
    public void destroy() {
       // do nothing
    }

}
