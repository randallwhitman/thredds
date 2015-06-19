/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.util.net;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.util.UnitTestCommon;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.test.util.ThreddsServer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class NoTestAuth2 extends UnitTestCommon
{

  static class Data
  {
      String url;
      CredentialsProvider provider;
      public Data(String u,CredentialsProvider p) {this.url=u; this.provider=p;}
  }
    protected Data[] cases = new Data[] {
      new Data("http://"+ TestDir.threddsTestServer+"/thredds/dodsC/restrict/testdata/testData.nc.html",
               new CredentialsProvider() {
                   public Credentials getCredentials(AuthScope scope) //AuthScheme sch, String h, int p, boolean pr)
                   {
                     UsernamePasswordCredentials creds
                       = new UsernamePasswordCredentials("tiggeUser","tigge");
                     System.out.printf("getCredentials called: creds=|%s| host=%s port=%d%n",
                                  creds.toString(),scope.getHost(),scope.getPort());
                     return creds;
                   }
                   public void setCredentials(AuthScope scope, Credentials creds) {}
                   public void clear() {}
               }),
      new Data("https://"+TestDir.dap2TestServer+"/dts/b31.dds",null),
    };

  @Override
  public void setUp() {
    ThreddsServer.REMOTETEST.assumeIsAvailable();
  }

  public void testAuth2() throws Exception
  {
    boolean pass = true;
    for(Data data: cases) {
        System.out.printf("url %s%n", data.url);
        try (HTTPMethod m = HTTPFactory.Get(data.url)) {
            try {
                if(data.provider != null)
                    m.getSession().setCredentialsProvider(data.provider);
                m.getSession().setUserAgent("tdmRunner");
                HTTPSession.setGlobalUserAgent("TDM v4.3");
                int status = m.execute();
                String s = m.getResponseAsString();
                System.out.printf("Trigger response = %d == %s%n", status, s);
                if(status != 200 && status != 404)
                    pass = false;
            } catch (HTTPException e) {
                System.err.println("Fail: " + data.url);
                ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
                e.printStackTrace(new PrintStream(bos));
                e.printStackTrace();
                pass = false;
            }
        }
    }
    if(pass)
      assertTrue("testAuth2", true);
    else
      assertTrue("testAuth2", false);
  }

}
