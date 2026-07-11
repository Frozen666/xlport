package com.molnify.xlport.core;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.Storage.Objects.Get;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.molnify.xlport.servlet.InitXlPort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;
import jakarta.servlet.ServletContext;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Service class for loading and processing Excel templates.
 *
 * <p>Supports multiple template sources: local filesystem, Google Sheets, and Google Cloud Storage.
 * Templates are processed to extract named ranges and tables, which are then used by
 * {@link Exporter} to populate data.
 */
public class TemplateManager {
  private static final Logger log = Logger.getLogger(TemplateManager.class.getName());

  private static ServletContext context = null;
  private static Storage storage;
  private static boolean GCS_INITIALIZED = false;
  private static final String GCS_BUCKET_NAME =
      System.getenv("XLPORT_GCS_BUCKET_NAME") != null
          ? System.getenv("XLPORT_GCS_BUCKET_NAME")
          : "xlport-templates";
  private static final String GCS_PATH =
      System.getenv("XLPORT_GCS_PATH") != null ? System.getenv("XLPORT_GCS_PATH") : "xlport/";

  private static boolean USE_GCS = false;

  static {
    String local = System.getenv("XLPORT_USE_LOCAL_TEMPLATES");
    if (local != null && "TRUE".equalsIgnoreCase(local.trim())) USE_GCS = false;
    if (USE_GCS) log.info("Using GCS bucket " + GCS_BUCKET_NAME + " for templates");
    else log.info("Using /WEB-INF/templates for templates");
  }

  public static void init(ServletContext context) {
    TemplateManager.context = context;
  }

  public static Template getLocalTemplateFromDirectory(String dirPath, String fileName)
      throws EncryptedDocumentException {
    Template template = new Template();
    try {
      template.workbook = getWorkbookFromFileStorage(dirPath, fileName);
      template.originalFileName = fileName;
      processTemplate(template);
      return template;
    } catch (Exception e) {
      log.warning("Failed to load template from " + dirPath + ": " + e.getMessage());
      return null;
    }
  }

  private static XSSFWorkbook getWorkbookFromFileStorage(String dirPath, String fileName) {
    try {
      File original = new File(dirPath + "/" + fileName);
      File copiedFile = File.createTempFile("xlport-temp" + fileName, ".xlsx");
      Files.copy(original, copiedFile);
      return (XSSFWorkbook) WorkbookFactory.create(copiedFile);
    } catch (IOException e) {
      log.warning("Failed to read workbook from " + dirPath + "/" + fileName + ": " + e.getMessage());
      return null;
    }
  }

  public static Template getLocalTemplateInTestDirectory(String fileName)
      throws EncryptedDocumentException {
    return getLocalTemplateFromDirectory("src/test/resources", fileName);
  }

  /**
   * Loads a template by ID. The ID can be a local filename, a Google Sheets URL,
   * or a GCS object path (depending on configuration).
   *
   * @param id the template identifier
   * @return the loaded and processed template, or null if not found
   */
  public static Template getTemplate(String id) {
    long t = System.currentTimeMillis();
    Template template = new Template();
    final String dummyTemplateName = "template1.xlsx";

    if (id == null || dummyTemplateName.equals(id) || "dummy".equals(id) || id.contains("..")) {
      template.workbook = (XSSFWorkbook) getWorkbookForFile("/WEB-INF/", dummyTemplateName);
      template.originalFileName = dummyTemplateName;
    } else if (id.startsWith("http") && id.contains("google.com/")) {
      try {
        template.workbook = WorkbookFetcherGoogleSheets.fetchGoogleSheetsTemplate(id);
        template.originalFileName = WorkbookFetcherGoogleSheets.getIdFromUrl(id);
      } catch (IOException e) {
        log.warning("Failed to fetch Google Sheet: " + e.getMessage());
      }
    } else if (USE_GCS) {
      ByteArrayOutputStream baos = getWithFullIdFromGCS(id);
      if (baos == null) return null;
      ByteArrayInputStream inStream = new ByteArrayInputStream(baos.toByteArray());
      try {
        template.workbook = (XSSFWorkbook) WorkbookFactory.create(inStream);
      } catch (EncryptedDocumentException | IOException e) {
        log.warning("Failed to create workbook from GCS: " + e.getMessage());
      }
      template.originalFileName = id;
    } else {
      template.workbook = (XSSFWorkbook) getWorkbookForFile("/WEB-INF/templates/", id);
      template.originalFileName = id;
    }

    log.info(
        "Template ["
            + template.originalFileName
            + "] read in "
            + (System.currentTimeMillis() - t)
            + " ms (cacheable, apart from first request)");
    processTemplate(template);
    return template;
  }

  public static void processTemplate(Template template) {
    // Process all single names in the workbook, and all tables
    long ts = System.currentTimeMillis();
    for (Name name : template.workbook.getAllNames()) {
      // Gives rise to Query table bug, that corrupts the Excel file written
      // name.setSheetIndex(-1);
      String sheetName = null;
      if (name.getSheetIndex() > -1)
        sheetName = template.workbook.getSheetName(name.getSheetIndex());
      template.addTemplateItem(
          new TemplateItem(name.getNameName(), name.getRefersToFormula(), sheetName));
    }
    List<XSSFTable> allTables = Utils.getAllTables(template.workbook);
    for (XSSFTable t : allTables) {
      TemplateItem theTable = new TemplateItem(t);
      template.addTemplateItem(theTable);
    }

    /*
    // This creates problems when there is some other object with the same name, e.g., a table with the same name as a sheet

    for(int i = 0; i < template.workbook.getNumberOfSheets(); i++) {
    	template.addTemplateItem(new TemplateItem(template.workbook.getSheetName(i)));
    }
    */

    // Check validity of the template (all should be fine, though)
    for (TemplateItem item : template.items.values()) {
      if (item.reference == null) // Empty
      log.warning(item.name + " does not refer to any cell range");
      else if (item.reference.indexOf(":")
          != item.reference.lastIndexOf(":")) // Multiple use of ":"
      log.warning(
            item.name
                + " refers to multiple ranges ["
                + item.reference
                + "] which is undefined and not allowed");
      else if (item.reference.contains("(")) // Potential formula
      log.warning(
            item.name
                + " refers to a formula ["
                + item.reference
                + "] which is undefined and not allowed");
    }
    log.info(
        "Template ["
            + template.originalFileName
            + "] processed and ready in "
            + (System.currentTimeMillis() - ts)
            + " ms (cacheable, apart from first request)");
  }

  public static void initGCS() {
    GCS_INITIALIZED = true;
    try {
      GoogleCredential credential =
          GoogleCredential.fromStream(
                  new ByteArrayInputStream(
                      InitXlPort.getGoogleCredential().getBytes(StandardCharsets.UTF_8)))
              .createScoped(Lists.newArrayList(StorageScopes.all()));
      storage =
          new Storage.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
              .setApplicationName("xlport File Storage")
              .build();
    } catch (IOException e) {
      throw new RuntimeException("failure while initializing GCS api", e);
    }
  }

  public static ByteArrayOutputStream getWithFullIdFromGCS(String fullId) {
    if (!GCS_INITIALIZED) initGCS();
    try {
      Get get = storage.objects().get(GCS_BUCKET_NAME, GCS_PATH + fullId);
      get.setAlt("media");
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      get.executeAndDownloadTo(bos);
      return bos;
    } catch (IOException e) {
      if ((e instanceof HttpResponseException)
          && ((HttpResponseException) e).getStatusCode() == 404) {
        // Convert NOT FOUND errors to nulls
        log.info("returning null");
        return null;
      }
      throw new RuntimeException("failure reading file from GCS", e);
    }
  }

  /** For debugging only */
  @SuppressWarnings("unused")
  private static void listAllObjects(Storage storage) throws IOException {
    com.google.api.services.storage.Storage.Objects.List list =
        storage.objects().list(GCS_BUCKET_NAME);
    for (StorageObject e : list.execute().getItems()) log.info("Object matching: " + e.getName());
  }

  private static Workbook getWorkbookForFile(String dummyDir, String dummyFile) {
    try {
      // If running stand alone, just get the file
      if (context == null) {
        log.info("Getting file from local storage");
        return getWorkbookFromFileStorage("src/main/webapp" + dummyDir, dummyFile);
      }
      // If running inside container, get the file from the context
      else {
        log.info("Getting file through application server");
        InputStream resourceAsStream = context.getResourceAsStream(dummyDir + dummyFile);
        if (resourceAsStream == null) return null;
        return WorkbookFactory.create(resourceAsStream);
      }
    } catch (EncryptedDocumentException | IOException e) {
      log.warning("Failed to load workbook: " + e.getMessage());
    }
    return null;
  }
}
