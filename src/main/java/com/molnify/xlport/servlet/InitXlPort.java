package com.molnify.xlport.servlet;

import com.molnify.xlport.core.TemplateManager;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Initialization servlet for the xlPort web service.
 *
 * <p>Handles {@code /ready} and {@code /alive} health check endpoints for Kubernetes
 * liveness/readiness probes. Also configures Google Cloud credentials from environment
 * variables on startup.
 */
@WebServlet({"/ready", "/alive"})
public class InitXlPort extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(InitXlPort.class.getName());
  private boolean READY = false;
  private static final Date _DEPLOY_TIME = new Date();
  public static String GOOGLE_CREDENTIAL = null;

  public static String getDeployTime() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
    return df.format(_DEPLOY_TIME);
  }

  /**
   * Allow configuration as a library, not only as a service with environment variables
   *
   * @param credential
   */
  public static void setGoogleCredential(String credential) {
    try {
      JSONObject o = new JSONObject(credential);
      GOOGLE_CREDENTIAL = credential;
      if (o.has("project_id") && o.has("private_key"))
        log.info("Google credential configured correctly");
      else log.warning("Google Credential appears to be incorrect");

    } catch (Exception e) {
      log.warning("Failed to set Google credential: " + e.getMessage());
    }
  }

  public static String getGoogleCredential() {
    return GOOGLE_CREDENTIAL;
  }

  static {
    String private_key_id = System.getenv("XLPORT_gcs_private_key_id");
    String private_key = System.getenv("XLPORT_gcs_private_key");
    String project_id = System.getenv("XLPORT_gcs_project_id");
    String client_id = System.getenv("XLPORT_gcs_client_id");
    String client_email = System.getenv("XLPORT_gcs_client_email");

    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("\"type\": \"service_account\",\n");
    sb.append("\"project_id\": \"" + project_id + "\",\n");
    sb.append("\"private_key_id\": \"" + private_key_id + "\",\n");
    sb.append("\"private_key\": \"" + private_key + "\",\n");
    sb.append("\"client_email\": \"" + client_email + "\",\n");
    sb.append("\"client_id\": \"" + client_id + "\",\n");
    sb.append("\"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n");
    sb.append("\"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n");
    sb.append("\"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n");
    String certUrl =
        client_email != null
            ? "https://www.googleapis.com/robot/v1/metadata/x509/"
                + client_email.replace("@", "%40")
            : "";
    sb.append("\"client_x509_cert_url\": \"" + certUrl + "\"\n");
    sb.append("}\n");
    if (private_key == null)
      log.info("Google Credentials not picked up from environment variables");
    else {
      GOOGLE_CREDENTIAL = sb.toString();
      log.info("Google credentials configured correctly from environment variables");
    }
  }

  @Override
  public void init(ServletConfig config) {
    if (config != null) {
      TemplateManager.init(config.getServletContext());
      log.info("xlPortV2 started");
      READY = true;
    } else {
      log.info("xlPortV2 not initialized correctly");
      READY = false;
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    if ("/ready".equals(req.getRequestURI()) && !READY)
      resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    else resp.setStatus(HttpServletResponse.SC_OK);
  }
}
