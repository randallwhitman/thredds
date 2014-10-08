/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENCE file for more information. */

package dap4.servlet;

import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4shared.XURI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URISyntaxException;

/**
 * Servlet specific info is captured once
 * and stored here.
 *
 * @author Dennis Heimbigner
 */


public class ServletInfo
{

    //////////////////////////////////////////////////
    // Constants

    static public final String RESOURCEDIRNAME = "resources";

    //////////////////////////////////////////////////
    // Instance variables

    protected HttpServlet servlet = null;
    protected ServletConfig servletconfig = null;
    protected ServletContext servletcontext = null;
    protected String servletname = null;
    protected String server = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public ServletInfo(HttpServlet sv)
        throws DapException
    {
        this.servlet = sv;
        this.servletconfig = sv.getServletConfig();
        if(this.servletconfig == null)
            throw new DapException("Cannot locate servlet config object");
        this.servletcontext = this.servletconfig.getServletContext();
        this.servletname = this.servletconfig.getServletName();
    }

    //////////////////////////////////////////////////
    // Accessors

    public HttpServlet getServlet()
    {
        return servlet;
    }

    public ServletConfig getServletconfig()
    {
        return servletconfig;
    }

    public ServletContext getServletcontext()
    {
        return servletcontext;
    }

    public String getServletname()
    {
        return servletname;
    }

    /**
     * Return the absolute path for the /WEB-INF/resources directory
     *
     * @return the absolute path for the /WEB-INF/resources directory
     */

    public String getRealPath(String virtual)
    {
        String realpath = null;
        if(this.servletconfig != null) {
            realpath = servletcontext.getRealPath(virtual);
            realpath = DapUtil.nullify(DapUtil.canonicalpath(realpath));
        }
        return realpath;
    }

    public String
    getContextPath()
    {
        return servletcontext.getContextPath();
    }

    public String getServer()
    {
        return this.server;
    }

    // We can only set this after we get some kind of call
    public void setServer(String url)
    {
        try {
            XURI xurl = new XURI(url);
            String simpleurl = xurl.getLeadProtocol()+"://"+xurl.getHost();
            if(this.server != null && !this.server.equals(simpleurl))
                DapLog.warn("ServletInfo.setServer: server mismatch: "+this.server+" :: "+simpleurl);
            this.server = simpleurl;
        } catch (URISyntaxException use) {
            DapLog.warn("ServletInfo.setServer: malformed url: " + url);
        }
    }

}
