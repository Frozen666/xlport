package com.molnify.xlport.servlet;

import com.molnify.xlport.core.Exporter;
import com.molnify.xlport.core.Importer;
import com.molnify.xlport.core.Template;
import com.molnify.xlport.core.TemplateManager;
import com.molnify.xlport.core.Utils;
import com.molnify.xlport.pdf.ExportFormat;
import com.molnify.xlport.pdf.PDFExporter;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * HTTP servlet providing {@code /import} and {@code /export} endpoints.
 *
 * <p>{@code PUT /export} accepts a JSON payload to populate an Excel template and returns the
 * generated spreadsheet (or PDF). {@code PUT /import} accepts an Excel file and returns
 * extracted data as JSON.
 *
 * <p>Supports API key authentication via the {@code XLPORT_API_KEY} environment variable
 * and CORS via {@code XLPORT_USE_CORS}.
 */
@WebServlet({"/import", "/export"})
public class ImportAndExportServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final Logger log = Logger.getLogger(ImportAndExportServlet.class.getName());
  private String API_KEY = null;
  private boolean USE_CORS = true;

  @Override
  public void init(ServletConfig config) {
    API_KEY = System.getenv("XLPORT_API_KEY");
    if (API_KEY != null) log.info("ApiKey set to [" + API_KEY + "]");
    else
      log.info(
          "Authorization DISABLED. Enabled it by setting the environment variable XLPORT_API_KEY");
    String cors = System.getenv("XLPORT_USE_CORS");
    if (cors != null && "FALSE".equalsIgnoreCase(cors.trim())) USE_CORS = false;
    if (USE_CORS) log.info("Cross origin requests allowed");
    else log.info("Cross origin requests not allowed");
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // Enable CORS
    if (USE_CORS) {
      resp.setHeader("Access-Control-Allow-Origin", "*");
      resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT");
      resp.setHeader(
          "Access-Control-Allow-Headers", "Authorization, Content-Type, Data-Type, Origin");
      resp.setHeader("Access-Control-Max-Age", "86400");
    }

    log.info(req.getRequestURI());
    // Security check
    if (API_KEY != null)
      if (req.getHeader("Authorization") == null
          || !req.getHeader("Authorization").equals("xlport apikey " + API_KEY)) {
        resp.setContentType("application/json");
        resp.getWriter()
            .println(
                new JSONObject()
                    .put("status", "error")
                    .put("message", "Please provide a valid apikey for authentication"));
        return;
      }

    // Request mapping based on URI and HTTP method
    if (req.getRequestURI().startsWith("/import")
        && ("PUT".equals(req.getMethod()) || "GET".equals(req.getMethod()))) {
      try {
        doImport(req, resp);
      } catch (Exception e) {
        log.warning("Import failed: " + e.getMessage());
        resp.setContentType("application/json");
        resp.getWriter()
            .println(new JSONObject().put("status", "error").put("message", e.getMessage()));
        return;
      }
    } else if (req.getRequestURI().startsWith("/export") && "PUT".equals(req.getMethod())) {
      JSONObject requestPayload = new JSONObject();
      // Validate JSON payload
      try {
        requestPayload = payloadAsJSON(req);
      } catch (JSONException e) {
        resp.setContentType("application/json");
        resp.getWriter()
            .println(new JSONObject().put("status", "error").put("message", e.getMessage()));
        return;
      }
      if (!requestPayload.has("templateId")) {
        resp.setContentType("application/json");
        resp.getWriter()
            .println(
                new JSONObject()
                    .put("status", "error")
                    .put("message", "templateId must be set when using export"));
      } else doExport(requestPayload, resp);
    } else {
      resp.setContentType("application/json");
      resp.getWriter()
          .println(
              new JSONObject()
                  .put("status", "error")
                  .put(
                      "message",
                      "Invalid request. Needs to be a call to /import or /export with GET or PUT"));
    }
  }

  // Excel to JSON
  private void doImport(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException, EncryptedDocumentException, FileUploadException {
    log.info("IMPORT from multipart");
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType("application/json");
    XSSFWorkbook workbook = null; // This should be the uploaded file
    String requestAsString = null; // This should be the request as JSON
    if (req.getContentType() != null
        && (req.getContentType().equals("application/octet-stream")
        || req.getContentType().equals("application/x-www-form-urlencoded"))) {
      workbook = (XSSFWorkbook) WorkbookFactory.create(req.getInputStream());
    } else {
      JakartaServletFileUpload<?, ?> upload = new JakartaServletFileUpload<>();
      FileItemInput item;
      InputStream stream = null;
      FileItemInputIterator iterator = upload.getItemIterator(req);
      // This is a multipart request with two files/fields sent
      // Both need to present to process the data
      // 1) The request as JSON
      // 2) The Excel file to extract data from
      while (iterator.hasNext()) {
        item = iterator.next();
        stream = item.getInputStream();
        if (("file".equals(item.getFieldName()) || "request".equals(item.getFieldName()))
            && !item.isFormField()) {
          if ("request".equals(item.getFieldName())) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Utils.copyFromInputToOutput(stream, bytes);
            requestAsString = bytes.toString("UTF-8");
          } else if ("file".equals(item.getFieldName())) {
            workbook = (XSSFWorkbook) WorkbookFactory.create(stream);
          }
        }
      }
    }
    if (workbook != null) {
      if (requestAsString == null)
        requestAsString =
            new JSONObject()
                .put("properties", new JSONArray().put("*"))
                .put("tables", new JSONArray().put("*"))
                .toString();
      JSONArray potentialErrors = new JSONArray();
      JSONObject result =
          Importer.importData(new JSONObject(requestAsString), workbook, potentialErrors, false);
      if (potentialErrors.length() > 0)
        resp.getWriter()
            .println(new JSONObject().put("status", "error").put("errors", potentialErrors));
      else resp.getWriter().println(new JSONObject().put("status", "success").put("data", result));
    } else
      resp.getWriter()
          .println(
              new JSONObject()
                  .put("status", "error")
                  .put(
                      "message",
                      "Both 'file' (Excel) and 'request' (JSON) objects needs to be passed to this"
                          + " service. See API documentation for more information and an example of"
                          + " a valid API call"));
  }

  // JSON to Excel
  private void doExport(JSONObject json, HttpServletResponse resp)
      throws ServletException, IOException {
    log.info("EXPORT from json: " + json.toString());
    // Default values + overrides from the request
    String fileName = "Result.xlsx", templateId = "template1.xlsx";
    boolean overwriteFormatting = true;
    boolean protectWorkbook = false;
    if (json.has("templateId")) templateId = json.getString("templateId");
    if (json.has("overwriteFormatting"))
      overwriteFormatting = json.getBoolean("overwriteFormatting");
    if (json.has("protectWorkbook")) protectWorkbook = json.getBoolean("protectWorkbook");
    Template template = TemplateManager.getTemplate(templateId);
    if (template == null) {
      resp.setContentType("application/json");
      resp.getWriter()
          .println(
              new JSONObject()
                  .put("status", "error")
                  .put(
                      "message",
                      "Template could not be found. Please specify a template that exists (e.g.,"
                          + " template1.xlsx)"));
      return;
    }

    if (!json.has("data")) {
      resp.setContentType("application/json");
      resp.getWriter()
          .println(
              new JSONObject()
                  .put("status", "error")
                  .put(
                      "message",
                      "Request needs to contain a key 'data' to specify the data to be exported"));
      return;
    }

    // Pipe back the result with the correct name
    JSONArray potentialErrors = new JSONArray();
    try {
      Exporter.exportToExcel(
          json.getJSONObject("data"), template, potentialErrors, overwriteFormatting);
      if (potentialErrors.length() > 0) {
        // resp.setContentType("application/json");
        // ﬁresp.getWriter().println(new JSONObject().put("status",
        // "error").put("error",potentialErrors));
        log.warning("Potential Errors: " + potentialErrors);
      }
      if (json.has("format") && "pdf".equals(json.getString("format"))) {
        ExportFormat exportFormat = new ExportFormat();
        boolean landscape = false;
        if (json.has("landscape")) {
          landscape = json.getBoolean("landscape");
          exportFormat.setPortrait(!landscape);
        }
        log.info(
            "Will export PDF with url (document id dummy as X): "
                + exportFormat.getExportURLForId("X"));
        String id = PDFExporter.uploadAndReturnId(template.workbook);
        log.info("ID: " + id);

        String url = exportFormat.getExportURLForId(id);
        log.info("URL: " + url);
        if (json.has("filename")) fileName = json.getString("filename") + "." + "pdf";
        if (json.has("mime")) resp.setContentType(json.getString("mime"));
        else resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        URL u = new URL(url);
        HttpURLConnection conn = null;
        try {
          // Add delay to Google to ensure permissions are updated
          try {
            Thread.sleep(800);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }

          conn = (HttpURLConnection) u.openConnection();
          HttpURLConnection.setFollowRedirects(true);
          Utils.copyFromInputToOutput(conn.getInputStream(), resp.getOutputStream());
        } catch (Exception e) {
          log.warning("Failed to connect to PDF export URL: " + e.getMessage());
        } finally {
          if (conn != null) conn.disconnect();
        }
      } else {
        if (json.has("filename"))
          fileName = json.getString("filename") + "." + template.getOriginalFileSuffix();
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        if (protectWorkbook) {
          String password = null;
          if (json.has("workbookPassword")) password = json.getString("workbookPassword");
          template.protectWorkbook(password);
        }
        template.workbook.write(resp.getOutputStream());
      }
    } catch (Throwable t) {
      resp.setContentType("application/json");
      resp.getWriter()
          .println(new JSONObject().put("status", "error").put("error", t.getMessage()));
    }
    template.workbook.close();
  }

  private static JSONObject payloadAsJSON(HttpServletRequest req) {
    StringBuffer jb = new StringBuffer();
    String line = null;
    try {
      BufferedReader reader = req.getReader();
      while ((line = reader.readLine()) != null) jb.append(line);
    } catch (Exception e) {
      log.warning("Failed to read request body: " + e.getMessage());
    }
    JSONObject jsonObject = new JSONObject(jb.toString());
    return jsonObject;
  }
}
