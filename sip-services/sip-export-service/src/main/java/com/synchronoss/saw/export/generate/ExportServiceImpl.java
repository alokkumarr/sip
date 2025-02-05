package com.synchronoss.saw.export.generate;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synchronoss.saw.analysis.modal.Analysis;
import com.synchronoss.saw.analysis.response.AnalysisResponse;
import com.synchronoss.saw.export.AmazonS3Handler;
import com.synchronoss.saw.export.S3Config;
import com.synchronoss.saw.export.ServiceUtils;
import com.synchronoss.saw.export.distribution.MailSenderUtil;
import com.synchronoss.saw.export.generate.interfaces.ExportService;
import com.synchronoss.saw.export.model.DataResponse;
import com.synchronoss.saw.export.model.DispatchType;
import com.synchronoss.saw.export.model.S3.S3Customer;
import com.synchronoss.saw.export.model.S3.S3Details;
import com.synchronoss.saw.export.model.ftp.FTPDetails;
import com.synchronoss.saw.export.model.ftp.FtpCustomer;
import com.synchronoss.saw.export.pivot.CreatePivotTable;
import com.synchronoss.saw.export.pivot.ElasticSearchAggregationParser;
import com.synchronoss.saw.export.util.ExportUtils;
import com.synchronoss.saw.model.Field;
import com.synchronoss.saw.model.SipQuery;
import com.synchronoss.sip.utils.RestUtil;

import com.synchronoss.sip.utils.SipCommonUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class ExportServiceImpl implements ExportService {

  private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

  @Value("${analysis.uiExportSize}")
  private String uiExportSize;

  // email export size
  @Value("${analysis.emailExportSize}")
  private String emailExportSize;

  // ftp export size
  @Value("${analysis.ftpExportSize}")
  private String ftpExportSize;

  // s3 export size
  @Value("${analysis.s3ExportSize}")
  private String s3ExportSize;

  @Value("${published.path}")
  private String publishedPath;

  @Value("${spring.mail.body}")
  private String mailBody;

  @Value("${ftp.details.file}")
  private String ftpDetailsFile;

  @Value("${ftp.details.privatekeyDir}")
  private String privatekeyDir;

  @Value("${s3.details.file}")
  private String s3DetailsFile;

  @Value("${exportChunkSize}")
  private String exportChunkSize;

  @Value("${proxy.service.host}")
  private String storageProxyUrl;

  @Value("${metadata.service.host}")
  private String metaDataServiceExport;

  @Autowired
  private ApplicationContext appContext;

  @Autowired private ServiceUtils serviceUtils;

  @Autowired private RestUtil restUtil;

  private RestTemplate restTemplate = null;

  private static final String PIVOT_ANALYSIS_TYPE = "pivot";
  private static final String DEFAULT_FILE_TYPE = "csv";
  private static final String INTERNAL_PROXY_STOR_URL = "/internal/proxy/storage/";
  private static final String INTERNAL_CALL = "internalCall=true";
  private static final String JOBGROUP = "jobGroup";
  private static final String EMAIL_LIST = "emailList";
  private static final String DELETE_EXPORT_FILE_CONST = "Deleting exported file.";
  private static final String DOUBLE_QUOTE_ESCAPE = "\"";

  @PostConstruct
  public void init() {
    restTemplate = restUtil.restTemplate();
  }

  @Override
  public ResponseEntity<DataResponse> dataToBeExportedAsync(
      String executionId,
      HttpServletRequest request,
      String analysisId,
      String analysisType,
      String executionType) {
    // During report extraction time, this parameter will not be passed.
    // Hence we should use uiExportSize configuration parameter.
    String sizOfExport;
    String url;
    sizOfExport =
        ((sizOfExport = request.getParameter("pageSize")) != null) ? sizOfExport : uiExportSize;
    if ((executionType != null && executionType.equalsIgnoreCase("onetime"))) {
      url =
          storageProxyUrl
              + INTERNAL_PROXY_STOR_URL
              + analysisId
              + "/executions/data?page=1&pageSize="
              + sizOfExport
              + "&analysisType="
              + analysisType
              + "&executionType="
              + executionType
              + "&" + INTERNAL_CALL;

    } else if (executionId == null) {
      url =
          storageProxyUrl
              + INTERNAL_PROXY_STOR_URL
              + analysisId
              + "/lastExecutions/data?page=1&pageSize="
              + sizOfExport
              + "&analysisType="
              + analysisType
              + "&" + INTERNAL_CALL;
    } else {
      url =
          storageProxyUrl
              + INTERNAL_PROXY_STOR_URL
              + analysisId
              + "/executions/data?page=1&pageSize="
              + sizOfExport
              + "&analysisType="
              + analysisType
              + "&" + INTERNAL_CALL;
    }
    HttpEntity<?> requestEntity = new HttpEntity<>(ExportUtils.setRequestHeader(request));
    /**
     * The AsyncRestTemplate api deprecated with spring(WebClient) which is having erroneous method,
     * So instead of calling asyncTemplate will call RestTemplate to perform UI export.
     */
    ResponseEntity<DataResponse> responseStringFuture =
        restTemplate.exchange(url, HttpMethod.GET, requestEntity, DataResponse.class);

    return responseStringFuture;
  }

  @Override
  @Async
  public void reportToBeDispatchedAsync(
      String executionId, RequestEntity request, String analysisId, String analysisType) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

    // at times we need synchronous processing even in async as because of massive parallelism
    // it may halt entire system or may not complete the request
    Object dispatchBean = request.getBody();

    logger.debug("Dispatch Bean = {}", dispatchBean);
    ExportBean exportBean = setExportBeanProps(dispatchBean,analysisId);
    logger.debug("Export Bean = {}", exportBean);

    String recipients = null;
    String ftp = null;
    String s3 = null;
    String jobGroup = null;
    boolean zip = false;
    // Read Additional props
    if (dispatchBean != null && dispatchBean instanceof LinkedHashMap) {
      if (((LinkedHashMap) dispatchBean).get(EMAIL_LIST) != null)
        recipients = String.valueOf(((LinkedHashMap) dispatchBean).get(EMAIL_LIST));
      if (((LinkedHashMap) dispatchBean).get("ftp") != null)
        ftp = String.valueOf(((LinkedHashMap) dispatchBean).get("ftp"));

      if (((LinkedHashMap) dispatchBean).get("s3") != null) {
        s3 = String.valueOf(((LinkedHashMap) dispatchBean).get("s3"));
        logger.trace("S3 list in reportToBeDispatchedAsync= {}", s3);
      }
      if (((LinkedHashMap) dispatchBean).get("zip") != null)
        zip = (Boolean) ((LinkedHashMap) dispatchBean).get("zip");

      jobGroup = String.valueOf(((LinkedHashMap) dispatchBean).get(JOBGROUP));
    }

    if ((recipients != null && !recipients.equals("") && recipients.contains("@"))
        || !StringUtils.isEmpty(ftp)
        || !StringUtils.isEmpty(s3)) {
      logger.trace("Recipients: {}", recipients);
      dispatchReport(
          analysisId, executionId, analysisType, exportBean, recipients, s3, ftp, zip, jobGroup);
    }
  }

  public ExportBean setExportBeanProps(Object dispatchBean, String analysisId) {

    ExportBean exportBean = new ExportBean();
    Analysis analysis = getAnalysis(analysisId);
    exportBean.setAnalysis(analysis);
    // presetting the variables, as their presence will determine which URLs to process
    if (dispatchBean != null && dispatchBean instanceof LinkedHashMap) {
      if (((LinkedHashMap) dispatchBean).get("fileType") != null) {
        exportBean.setFileType(String.valueOf(((LinkedHashMap) dispatchBean).get("fileType")));
      }
      exportBean.setReportName(analysis.getName());
      exportBean.setReportDesc(analysis.getDescription());
      exportBean.setPublishDate(
          String.valueOf(((LinkedHashMap) dispatchBean).get("publishedTime")));
      exportBean.setCreatedBy(String.valueOf(((LinkedHashMap) dispatchBean).get("userFullName")));
      // consider default format as csv if file type is not provided.
      String fileName = ExportUtils.prepareFileName(analysis.getName(), exportBean.getFileType());
      if (exportBean.getFileType() == null || exportBean.getFileType().isEmpty()) {
        exportBean.setFileType(DEFAULT_FILE_TYPE);
      }
      exportBean.setFileName(fileName);
    }
    return exportBean;
  }

  public void streamResponseToFile(
      ExportBean exportBean,
      ResponseEntity<DataResponse> entity,
      Long recordsToSkip,
      Long recordsTolimit) {
    try {
      logger.trace("Inside streamResponseToFile");
      String normalizedPath = SipCommonUtils.normalizePath(exportBean.getFileName());
      File file = new File(normalizedPath);
      file.getParentFile().mkdirs();
      // if the file is found, append the content
      // this is basically for entire for loop to execute correctly on the same file
      // as no two executions are going to have same ID.
      FileOutputStream fos = new FileOutputStream(file, true);
      OutputStreamWriter osw = new OutputStreamWriter(fos);
      Map<String, String> columnHeader = ExportUtils.buildColumnHeaderMap(exportBean.getAnalysis());
      logger.debug("Preparing csv report");
      streamToCSVReport(columnHeader, entity, exportBean, osw, recordsToSkip, recordsTolimit);
      osw.close();
      fos.close();
    } catch (IOException e) {
      logger.error(
          "Exception occurred while dispatching report :"
              + this.getClass().getName()
              + "  method dataToBeDispatchedAsync()");
    }
  }

  public void streamToCSVReport(
      Map<String, String> columnHeader,
      ResponseEntity<DataResponse> entity,
      ExportBean exportBean,
      OutputStreamWriter osw,
      Long recordsToSkip,
      Long recordsTolimit) {
    List<Object> data = entity.getBody().getData();
    logger.trace("data size size to stream to csv report:{}", data.size());
    logger.trace("recordsTolimit:{}", recordsTolimit);
    logger.trace("recordsToSkip:{}", recordsToSkip);
    logger.trace("Column Header = " + columnHeader);
    logger.trace("Export bean header = " + StringUtils.join(exportBean.getColumnHeader(), ", "));
    
    if (data == null || data.size() == 0) {
      logger.info("No data to export");
      return;
    }
    data.stream()
        .skip(recordsToSkip)
        .limit(recordsTolimit)
        .forEach(
            line -> {
              try {
                if (line instanceof LinkedHashMap) {
                  logger.trace("Line = " + line);
                  String[] header = null;
                  if (exportBean.getColumnHeader() == null
                      || exportBean.getColumnHeader().length == 0) {
                    logger.trace("Export header is null");
                    Object[] obj;
                    if (columnHeader != null && !columnHeader.isEmpty()) {
                      obj = columnHeader.keySet().toArray();
                    } else {
                      obj = ((LinkedHashMap) line).keySet().toArray();
                    }
                    if (exportBean.getColumnDataType() != null
                        && exportBean.getColumnDataType().length > 0) {
                      header = exportBean.getColumnHeader();
                    } else {
                      header = Arrays.copyOf(obj, obj.length, String[].class);
                    }

                    logger.trace("Header = " + StringUtils.join(header, ", "));
                    exportBean.setColumnHeader(header);

                    logger.trace(
                        "Export bean after setting = "
                            + StringUtils.join(exportBean.getColumnHeader(), ", "));
                    osw.write(
                        Arrays.stream(header)
                            .map(
                                i -> {
                                  String colHeader =
                                      columnHeader != null
                                              && !columnHeader.isEmpty()
                                              && columnHeader.get(i) != null
                                          ? columnHeader.get(i)
                                          : i;
                                  return DOUBLE_QUOTE_ESCAPE + colHeader + DOUBLE_QUOTE_ESCAPE;
                                })
                            .collect(Collectors.joining(",")));
                    osw.write("\n");

                    if (columnHeader == null || columnHeader.isEmpty()) {
                      osw.write(
                          Arrays.stream(exportBean.getColumnHeader())
                              .map(
                                  val -> {
                                    if (((LinkedHashMap) line).get(val) == null) {
                                      return "null";
                                    }
                                    return "\"" + ((LinkedHashMap) line).get(val) + "\"";
                                  })
                              .collect(Collectors.joining(",")));
                    } else {
                      osw.write(
                          columnHeader.entrySet().stream()
                              .map(
                                  entry -> {
                                    String key = entry.getKey();
                                    String value = entry.getValue();

                                    LinkedHashMap<String, Object> linkedHashMap =
                                        (LinkedHashMap) line;
                                    LinkedCaseInsensitiveMap<Object> linkedCaseInsensitiveMap =
                                        ExportUtils.convert(linkedHashMap);

                                    if (linkedCaseInsensitiveMap.get(key) != null) {
                                      return DOUBLE_QUOTE_ESCAPE
                                          + linkedCaseInsensitiveMap.get(key)
                                          + DOUBLE_QUOTE_ESCAPE;
                                    } else if (linkedCaseInsensitiveMap.get(value) != null) {
                                      return DOUBLE_QUOTE_ESCAPE
                                          + linkedCaseInsensitiveMap.get(value)
                                          + DOUBLE_QUOTE_ESCAPE;
                                    } else {
                                      return "null";
                                    }
                                  })
                              .collect(Collectors.joining(",")));
                    }

                    osw.write(System.getProperty("line.separator"));
                    logger.debug("Header for csv file: {}", header);
                  } else {
                    // ideally we shouldn't be using collectors but it's a single row so it
                    // won't hamper memory consumption

                    logger.trace("Export header is not null");

                    if (columnHeader == null || columnHeader.isEmpty()) {
                      osw.write(
                          Arrays.stream(exportBean.getColumnHeader())
                              .map(
                                  val -> {
                                    if (((LinkedHashMap) line).get(val) == null) {
                                      return "null";
                                    }
                                    return "\"" + ((LinkedHashMap) line).get(val) + "\"";
                                  })
                              .collect(Collectors.joining(",")));
                    } else {
                      osw.write(
                          columnHeader.entrySet().stream()
                              .map(
                                  entry -> {
                                    String key = entry.getKey();
                                    String value = entry.getValue();

                                    LinkedHashMap<String, Object> linkedHashMap =
                                        (LinkedHashMap) line;
                                    LinkedCaseInsensitiveMap<Object> linkedCaseInsensitiveMap =
                                        ExportUtils.convert(linkedHashMap);

                                    if (linkedCaseInsensitiveMap.get(key) != null) {
                                      return DOUBLE_QUOTE_ESCAPE
                                          + linkedCaseInsensitiveMap.get(key)
                                          + DOUBLE_QUOTE_ESCAPE;
                                    } else if (linkedCaseInsensitiveMap.get(value) != null) {
                                      return DOUBLE_QUOTE_ESCAPE
                                          + linkedCaseInsensitiveMap.get(value)
                                          + DOUBLE_QUOTE_ESCAPE;
                                    } else {
                                      return "null";
                                    }
                                  })
                              .collect(Collectors.joining(",")));
                    }
                    osw.write(System.getProperty("line.separator"));
                  }
                }
              } catch (Exception e) {
                logger.error("ERROR_PROCESSING_STREAM: {}", e.getMessage());
              }
            });
  }

  /**
   * This method to build excel file with batch size data by using apache API api.
   *
   * @param analysis query definition to filter out data
   * @param totalExportSize total configured size to be processed S3/FTP/Email
   * @param exportBean bean which have all the required fields to build fields
   * @throws IOException
   */
  public Boolean streamToXlsxReport(
      Analysis analysis,
      String executionId,
      String analysisType,
      long totalExportSize,
      ExportBean exportBean)
      throws IOException {
    logger.trace("Preparing file with size = {}", totalExportSize);
    Long lastExportedSize = exportBean.getLastExportedSize();
    logger.trace("lastExportedSize = {}", lastExportedSize);
    Long lastExportLimit = exportBean.getLastExportLimit();
    logger.trace("lastExportLimit = {}", lastExportLimit);
    Integer currentPage = exportBean.getPageNo();
    currentPage = currentPage == null ? 1 : currentPage;
    logger.trace("currentPage = {}", currentPage);
    long currentXlsRow = lastExportedSize == null ? 1 : lastExportedSize + 1;
    if (!(lastExportedSize != null
        && lastExportLimit != null
        && (lastExportedSize < lastExportLimit))) {
      String normalizedPath = SipCommonUtils.normalizePath(exportBean.getFileName());
      File xlsxFile = new File(normalizedPath);
      BufferedOutputStream stream = null;
      XlsxExporter xlsxExporter = new XlsxExporter();
      Workbook workBook = null;
      String sheetName = ExportUtils.prepareExcelSheetName(exportBean.getReportName());
      SXSSFSheet sheet = null;
      FileInputStream input = null;
      if (xlsxFile.exists()) {
        try {
          input = new FileInputStream(xlsxFile);
          workBook = new SXSSFWorkbook((XSSFWorkbook) WorkbookFactory.create(input));
          sheet = (SXSSFSheet) workBook.getSheet(sheetName);
        } catch (Exception e) {
          logger.error("ERROR: didn't create workbook :{} ", e);
        }
      } else {
        xlsxFile.getParentFile().mkdir();
        xlsxFile.createNewFile();
        workBook = new SXSSFWorkbook();
        sheet = (SXSSFSheet) workBook.createSheet(sheetName);
      }
      ResponseEntity<DataResponse> entity = null;
      try {
        // write the data in excel sheet in batch
        long batchSize = exportChunkSize != null ? Long.valueOf(exportChunkSize) : 0l;
        long totalNumberOfBatch = batchSize > 0 ? totalExportSize / batchSize : 0l;
        Long recordsToSkip = 0l;
        if (lastExportedSize != null)
          recordsToSkip = lastExportedSize - ((currentPage - 1) * batchSize);
        boolean flag = true;
        long pageNo;
        long totalRowsCount = 0;
        for (pageNo = currentPage; pageNo <= totalNumberOfBatch; pageNo++) {
          entity =
              getExecutionData(executionId, analysisType, pageNo, batchSize, DataResponse.class);
          DataResponse response = entity.getBody();
          List<Object> data = response.getData();
          logger.trace("total record count:{}", response.getTotalRows());

          // break if no data available
          if (CollectionUtils.isEmpty(data)) return false;
          // point the cursor in excel file
          if (recordsToSkip > 0) {
            data = data.stream().skip(recordsToSkip).collect(Collectors.toList());
            recordsToSkip = 0l;
          }
          xlsxExporter.buildXlsxSheet(
              analysis, exportBean, workBook, sheet, data, batchSize, currentXlsRow);
          currentXlsRow = currentXlsRow + data.size();
          currentPage++;
          // recalculate the total number of batch size if available record less than export size.
          totalRowsCount = response.getTotalRows();
          if (totalRowsCount < totalExportSize && flag) {
            totalNumberOfBatch = totalRowsCount / batchSize;
          }
          flag = false;
        }
        exportBean.setPageNo(currentPage);

        // final rows to process
        long leftOutRows;
        if (totalRowsCount > 0 && totalRowsCount <= totalExportSize) {
          leftOutRows = totalRowsCount - (pageNo - 1) * batchSize;
        } else {
          leftOutRows = totalExportSize - (pageNo - 1) * batchSize;
        }
        if (leftOutRows > 0 && totalNumberOfBatch >= 0 && batchSize > 0) {
          entity =
              getExecutionData(executionId, analysisType, pageNo, batchSize, DataResponse.class);
          DataResponse response = entity.getBody();
          List<Object> data = response.getData();
          // point the cursor in excel file
          long remainingRows = leftOutRows - recordsToSkip;
          logger.trace("left out rows = {}", leftOutRows);
          List<Object> leftOutRowsToProcess =
              data.stream().skip(recordsToSkip).limit(remainingRows).collect(Collectors.toList());
          xlsxExporter.buildXlsxSheet(
              analysis,
              exportBean,
              workBook,
              sheet,
              leftOutRowsToProcess,
              remainingRows,
              currentXlsRow);
        }
        Long lastExportSize = (currentPage - 1) * batchSize + leftOutRows;
        logger.trace("setting  lastExportSize  :{}", lastExportSize);
        exportBean.setLastExportedSize(lastExportSize);
        stream = new BufferedOutputStream(new FileOutputStream(xlsxFile));
        workBook.write(stream);
      } catch (IOException ex) {
        logger.error("Error occurred while writing the data in xls sheet with POI Api: {}", ex);
      } catch (Exception e) {
        logger.error("Exception occured while writing data to xlsx sheet:{}", e);
      } finally {
        stream.flush();
        stream.close();
      }
    }
    exportBean.setLastExportLimit(Long.valueOf(totalExportSize));
    return true;
  }

  @Override
  public List<String> listFtpsForCustomer(RequestEntity request) {
    Object dispatchBean = request.getBody();
    // this job group is customer unique identifier
    String jobGroup;
    List<String> aliases = new ArrayList<String>();

    if (dispatchBean != null && dispatchBean instanceof LinkedHashMap) {
      jobGroup = String.valueOf(((LinkedHashMap) dispatchBean).get(JOBGROUP));
      ObjectMapper jsonMapper = new ObjectMapper();
      try {
        String normalizedFtpDetailsFile = SipCommonUtils.normalizePath(ftpDetailsFile);
        File f = new File(normalizedFtpDetailsFile);
        if (f.exists() && !f.isDirectory()) {
          FtpCustomer obj = jsonMapper.readValue(f, FtpCustomer.class);
          for (FTPDetails alias : obj.getFtpList()) {
            if (alias.getCustomerName().equals(jobGroup)) {
              aliases.add(alias.getAlias());
            }
          }
        }
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }
    return aliases;
  }

  @Override
  public List<String> listS3ForCustomer(RequestEntity requestEntity) {
    Object dispatchBean = requestEntity.getBody();
    String jobGroup = null;
    List<String> aliases = new ArrayList<String>();

    if (dispatchBean != null && dispatchBean instanceof LinkedHashMap) {
      jobGroup = String.valueOf(((LinkedHashMap) dispatchBean).get(JOBGROUP));
      ObjectMapper jsonMapper = new ObjectMapper();
      try {
        String normalizedS3DetailsFile = SipCommonUtils.normalizePath(s3DetailsFile);
        File f = new File(normalizedS3DetailsFile);
        if (f.exists() && !f.isDirectory()) {
          S3Customer obj = jsonMapper.readValue(f, S3Customer.class);
          for (S3Details alias : obj.getS3List()) {
            if (alias.getCustomerCode().equals(jobGroup)) {
              aliases.add(alias.getAlias());
            }
          }
        }
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }
    return aliases;
  }

  /**
   * Method to dispatch report with mails, this operation performed asynchronously
   *
   * @param executionId executionId to generate report data
   * @param analysisType analysisType type of analysis to fetch the data
   * @param exportBean exportBean bean have all the required filed to build sheet
   * @param recipients recipients mail to be sent
   * @param zip Boolean flag if zip checkbox selected from UI
   * @return True if the mail dispatched successfully else false
   */
  public boolean dispatchToMail(
      String executionId,
      String analysisType,
      ExportBean exportBean,
      String recipients,
      boolean zip) {
    logger.debug("Inside dispatch mail for fileType :{}", exportBean.getFileType());
    try {
      if (exportBean.getFileType().equalsIgnoreCase(DEFAULT_FILE_TYPE)) {
        prepareFileWithSize(executionId, analysisType, emailExportSize, exportBean);
        dispatchMail(exportBean, zip, recipients);

      } else {
        streamToXlsxReport(
            exportBean.getAnalysis(),
            executionId,
            analysisType,
            Long.valueOf(emailExportSize),
            exportBean);
        dispatchMail(exportBean, zip, recipients);
      }
    } catch (Exception e) {
      logger.error("Exception occurred while dispatching email  report:{}", e);
      return false;
    }
    return true;
  }

  Boolean dispatchMail(ExportBean exportBean, boolean zip, String recipients) {

    try {
      MailSenderUtil MailSender = new MailSenderUtil(appContext.getBean(JavaMailSender.class));
      String normalizedPath = SipCommonUtils.normalizePath(exportBean.getFileName());
      File file = new File(normalizedPath);
      final String emailSubject = exportBean.getReportName() + " | " + exportBean.getPublishDate();
      String emailBody = serviceUtils.prepareMailBody(exportBean, mailBody);
      if (zip) {
        String zipFileName = ExportUtils.buildZipFile(exportBean, file);
        MailSender.sendMail(recipients, emailSubject, emailBody, zipFileName);
        logger.info("Email sent successfully");

        logger.trace(DELETE_EXPORT_FILE_CONST);
        try {
          logger.trace(
              "ExportBean.getFileName() to delete -  mail : {} , {} ",
              zipFileName, exportBean.getFileName());
          ExportUtils.deleteDispatchedFile(zipFileName, serviceUtils);
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      } else {
        MailSender.sendMail(recipients, emailSubject, emailBody, exportBean.getFileName());
        logger.info("Email sent successfully");
      }
    } catch (IOException e) {
      logger.error(
          "Exception occurred while dispatching email reports : {}", this.getClass().getName());
    }
    return true;
  }

  public void dispatchToS3(
      String executionId,
      String analysisType,
      ExportBean exportBean,
      String finalS3,
      boolean zip,
      String finalJobGroup) {
    logger.debug("Inside dispatch S3 for fileType :{}", exportBean.getFileType());
    try {
      if (exportBean.getFileType().equalsIgnoreCase(DEFAULT_FILE_TYPE)) {
        prepareFileWithSize(executionId, analysisType, s3ExportSize, exportBean);
        dispatchFileToS3(exportBean, finalS3, finalJobGroup, zip);
      } else {
        streamToXlsxReport(
            exportBean.getAnalysis(),
            executionId,
            analysisType,
            Long.valueOf(s3ExportSize),
            exportBean);
        dispatchFileToS3(exportBean, finalS3, finalJobGroup, zip);
      }
    } catch (Exception e) {
      logger.error("Exception ocurred while dispatching S3  report:{}", e);
    }
  }

  void dispatchFileToS3(ExportBean exportBean, String finalS3, String finalJobGroup, boolean zip) {
    String normalizedPath=SipCommonUtils.normalizePath(exportBean.getFileName());
    File cfile = new File(normalizedPath);
    logger.debug("Final S3 = {}", finalS3);

    if (zip) {

      logger.debug("S3 - zip = true!!");
      try {
        String zipFileName = ExportUtils.buildZipFile(exportBean, cfile);
        s3DispatchExecutor(finalS3, finalJobGroup, new File(zipFileName));

        logger.debug("ExportBean.getFileName() - to delete in S3 : {}", exportBean.getFileName());
        ExportUtils.deleteDispatchedFile(zipFileName, serviceUtils);
        logger.debug("ExportBean.getFileName() - to delete in S3 : {}", zipFileName);

      } catch (Exception e) {
        logger.error("Error writing to zip!!");
      }
    } else {
      s3DispatchExecutor(finalS3, finalJobGroup, cfile);
      logger.debug("ExportBean.getFileName() - to delete in S3 : {}", exportBean.getFileName());
    }
  }

  public void s3DispatchExecutor(String finalS3, String finalJobGroup, File file) {
    for (String aliasTemp : finalS3.split(",")) {
      logger.trace("AliasTemp : {}", aliasTemp);
      ObjectMapper jsonMapper = new ObjectMapper();
      try {
        String normalizedS3DetailsFile = SipCommonUtils.normalizePath(s3DetailsFile);
        S3Customer s3Customer = jsonMapper.readValue(new File(normalizedS3DetailsFile), S3Customer.class);
        for (S3Details alias : s3Customer.getS3List()) {
          if (alias.getCustomerCode().equals(finalJobGroup) && aliasTemp.equals(alias.getAlias())) {
            logger
                .trace("BucketName : {}, Region : {}, Output Location : {}", alias.getBucketName(),
                    alias.getRegion(), alias.getOutputLocation());
            S3Config s3Config =
                new S3Config(
                    alias.getBucketName(),
                    alias.getAccessKey(),
                    alias.getSecretKey(),
                    alias.getRegion(),
                    alias.getOutputLocation(),
                    alias.getCannedAcl());

            AmazonS3Handler s3Handler = new AmazonS3Handler(s3Config);
            s3Handler.uploadObject(file.getAbsoluteFile());
          }
        }
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }
  }

  public void dispatchReport(
      String analysisId,
      String executionId,
      String analysisType,
      ExportBean exportBean,
      String recipients,
      String s3,
      String ftp,
      boolean zip,
      String jobGroup) {
    String userFileName = exportBean.getFileName();
    // create a directory with unique name in published location.
    final String dispatchFileName = filePath(null, userFileName);
    exportBean.setFileName(dispatchFileName);
    Map<DispatchType, Long> sizeMap = new HashMap<>();

    if (!StringUtils.isEmpty(recipients)) {
      sizeMap.put(DispatchType.MAIL, Long.valueOf(emailExportSize));
    }
    if (!StringUtils.isEmpty(ftp)) {
      sizeMap.put(DispatchType.FTP, Long.valueOf(ftpExportSize));
    }

    if (!StringUtils.isEmpty(s3)) {
      sizeMap.put(DispatchType.S3, Long.valueOf(s3ExportSize));
    }

    /* Here we are maintaing the methods in a map and sorting the the map based on the dispatch limit because
     * for csv format we are reusing the same file across all types of dispatch(mail,ftp,s3), so because of that while preparig
     * the csv we should prepare lower size data first and we should append the data incrementally.*/
    LinkedHashMap<DispatchType, Long> finalSortedMap = new LinkedHashMap<>();

    sizeMap.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .forEachOrdered(x -> finalSortedMap.put(x.getKey(), x.getValue()));
    sizeMap = null;
    logger.debug("sorted map:{}", finalSortedMap);
    try {
      finalSortedMap.forEach(
          (key, vaue) -> {
            switch (key) {
              case MAIL:
                {
                  Instant start = Instant.now();
                  dispatchToMail(executionId, analysisType, exportBean, recipients, zip);
                  Instant finish = Instant.now();
                  long timeElapsed = Duration.between(start, finish).toMillis();
                  logger.trace("time taken for email dispatch:{}", timeElapsed);
                  break;
                }
              case FTP:
                {
                  Instant start = Instant.now();
                  dispatchToFtp(executionId, analysisType, exportBean, ftp, zip, jobGroup);
                  Instant finish = Instant.now();
                  long timeElapsed = Duration.between(start, finish).toMillis();
                  logger.trace("time taken for FTP dispatch:{}", timeElapsed);
                  break;
                }
              case S3:
                {
                  Instant start = Instant.now();
                  dispatchToS3(executionId, analysisType, exportBean, s3, zip, jobGroup);
                  Instant finish = Instant.now();
                  long timeElapsed = Duration.between(start, finish).toMillis();
                  logger.trace("time taken for S3 dispatch:{}", timeElapsed);
                  break;
                }
            }
          });
    } catch (Exception e) {
      logger.error("error occured while dispatching report:{}", e);
    } finally {
      logger.info(DELETE_EXPORT_FILE_CONST);
      try {
        logger.debug("ExportBean.getFileName() to delete : {}",exportBean.getFileName());
        ExportUtils.deleteDispatchedFile(exportBean.getFileName(), serviceUtils);
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    }
  }

  public void prepareFileWithSize(
      String executionId, String analysisType, String exportSize, ExportBean exportBean) {
    logger.trace("Preparing file with size = {}", exportSize);
    Long lastExportedSize = exportBean.getLastExportedSize();
    logger.trace("lastExportedSize = {}", lastExportedSize);
    Long lastExportLimit = exportBean.getLastExportLimit();
    logger.trace("lastExportLimit = {}", lastExportLimit);
    Integer currentPage = exportBean.getPageNo();
    currentPage = currentPage == null ? 1 : currentPage;
    logger.trace("currentPage = {}", currentPage);
    if (!(lastExportedSize != null
        && lastExportLimit != null
        && (lastExportedSize < lastExportLimit))) {
      long limitPerPage = Long.parseLong(exportChunkSize);
      long page = 0;
      double noOfPages = Math.ceil(Long.valueOf(exportSize) / limitPerPage);
      boolean flag = true;
      long totalRowCount = 0;
      Long recordsToSkip = 0l;
      Long recordsTolimit = limitPerPage;
      if (lastExportedSize != null)
        recordsToSkip = lastExportedSize - ((currentPage - 1) * limitPerPage);
      logger.trace("recordsToSkip = {}",recordsToSkip);
      ResponseEntity<DataResponse> entity = null;
      for (page = currentPage; page <= noOfPages; page += 1) {
        recordsTolimit = limitPerPage;
        entity =
            getExecutionData(executionId, analysisType, page, limitPerPage, DataResponse.class);
        totalRowCount = entity.getBody().getTotalRows();
        logger.trace("Total row count = {}",totalRowCount);
        if (totalRowCount <= Double.parseDouble(exportSize) && flag) {
          noOfPages = Math.ceil(totalRowCount / limitPerPage);
        }
        flag = false;
        streamResponseToFile(exportBean, entity, recordsToSkip, recordsTolimit);
        recordsToSkip = 0l;
        currentPage++;
      }
      exportBean.setPageNo(currentPage);
      // final rows to process
      long leftOutRows = 0;
      if (totalRowCount != 0 && totalRowCount <= Double.parseDouble(exportSize)) {
        leftOutRows = totalRowCount - (currentPage - 1) * limitPerPage;
      } else {
        leftOutRows = Long.parseLong(exportSize) - (currentPage - 1) * limitPerPage;
      }
      logger.trace("left out rows : {}",leftOutRows);

      if (leftOutRows > 0) {
        recordsTolimit = leftOutRows - recordsToSkip;
        entity =
            getExecutionData(executionId, analysisType, page, limitPerPage, DataResponse.class);
        streamResponseToFile(exportBean, entity, recordsToSkip, recordsTolimit);
      }
      Long lastExportSize = (currentPage - 1) * limitPerPage + leftOutRows;
      logger.trace("setting  lastExportSize  :{}", lastExportSize);
      exportBean.setLastExportedSize(lastExportSize);
      logger.debug("File created");
    }
    logger.trace("setting  lastExportLimit :{}", exportSize);
    exportBean.setLastExportLimit(Long.valueOf(exportSize));
  }

  /**
   * Dispatch files to FTP
   *
   * @param executionId
   * @param analysisType
   * @param exportBean
   * @param finalFtp
   * @param zip
   * @param finalJobGroup
   */
  public void dispatchToFtp(
      String executionId,
      String analysisType,
      ExportBean exportBean,
      String finalFtp,
      boolean zip,
      String finalJobGroup) {
    logger.trace("Inside dispatch FTP for fileType :{}", exportBean.getFileType());
    try {
      if (exportBean.getFileType().equalsIgnoreCase(DEFAULT_FILE_TYPE)) {
        prepareFileWithSize(executionId, analysisType, ftpExportSize, exportBean);
        createZipForFtp(finalFtp, exportBean, finalJobGroup, zip);
      } else {
        streamToXlsxReport(
            exportBean.getAnalysis(),
            executionId,
            analysisType,
            Long.valueOf(ftpExportSize),
            exportBean);
        createZipForFtp(finalFtp, exportBean, finalJobGroup, zip);
      }
    } catch (Exception e) {
      logger.error("Exception ocurred while dispatching FTP  report:{}", e);
    }
  }

  /**
   * Build zip file if zip was selected from GUI.
   *
   * @param finalFtp
   * @param exportBean
   * @param finalJobGroup
   * @param zip
   */
  public void createZipForFtp(
      String finalFtp, ExportBean exportBean, String finalJobGroup, boolean zip) {
    // zip the contents of the file

    if (!StringUtils.isEmpty(finalFtp)) {
      try {
        String normalizedPath=SipCommonUtils.normalizePath(exportBean.getFileName());
        File file = new File(normalizedPath);
        String fileName = file.getAbsolutePath();
        if (zip) {
          fileName = ExportUtils.buildZipFile(exportBean, file);
          logger.info("ftpFilename: {}",fileName);
        }

        // Dispatch the zipped file to ftp
        FtpDispatcher(finalJobGroup, finalFtp, fileName, zip, file, exportBean.getFileType());

        // deleting the files
          if(zip){
          ExportUtils.deleteDispatchedFile(fileName,serviceUtils);
              }
        logger.debug("ExportBean.getFileName() - to delete file FTP : {}",exportBean.getFileName());
        logger.debug(DELETE_EXPORT_FILE_CONST);
      } catch (Exception e) {
        logger.error("ftp error: {}",e.getMessage());
      }
    }
  }

  public void FtpDispatcher(
      String finalJobGroup,
      String finalFtp,
      String fileName,
      boolean zip,
      File cfile,
      String fileType) {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    LocalDateTime now = LocalDateTime.now();

    for (String aliasTemp : finalFtp.split(",")) {
      ObjectMapper jsonMapper = new ObjectMapper();
      try {
        String destinationFileName;
        String tempFileName = cfile.getName().substring(0, cfile.getName().lastIndexOf('.'));
        if (zip) {
          destinationFileName = tempFileName + dtf.format(now) + "." + fileType + ".zip";
        } else {
          destinationFileName = tempFileName + dtf.format(now) + "." + fileType;
        }
        String normalizedFtpDetailsFile = SipCommonUtils.normalizePath(ftpDetailsFile);
        FtpCustomer obj = jsonMapper.readValue(new File(normalizedFtpDetailsFile), FtpCustomer.class);
        for (FTPDetails alias : obj.getFtpList()) {
          if (alias.getCustomerName().equals(finalJobGroup) && aliasTemp.equals(alias.getAlias())) {
            String privatekeyFile = alias.getPrivatekeyFile();

            String privatekeyPath = null;
            if (privatekeyFile != null) {
              privatekeyPath = privatekeyDir + File.separator + privatekeyFile;
            }

            serviceUtils.uploadToFtp(
                alias.getHost(),
                alias.getPort(),
                alias.getUsername(),
                alias.getPassword(),
                fileName,
                alias.getLocation(),
                destinationFileName,
                alias.getType(),
                privatekeyPath,
                alias.getPassPhrase());
            logger.debug(
                "Uploaded to ftp alias: {} : {}",alias.getCustomerName(),alias.getHost());
          }
        }
        if(zip){
            ExportUtils.deleteDispatchedFile(destinationFileName, serviceUtils);
        }
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }
  }

  /**
   * This will fetch the SIP query from metadata and provide.
   *
   * @param analysisId
   * @return SipQuery
   */
  public Analysis getAnalysis(String analysisId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      String url = metaDataServiceExport + "/dslanalysis/" + analysisId+"?internalCall=true";
      logger.debug("SIP query url for analysis fetch : {}", url);
      AnalysisResponse analysisResponse = restTemplate.getForObject(url, AnalysisResponse.class);
      Analysis analysis = analysisResponse.getAnalysis();

      logger.debug("Fetched SIP query for analysis : {}", analysis);
      return analysis;
    } catch (Exception e) {
      logger.error("Exception occurred while fetching sipQuery:{}",e);
      throw new RuntimeException("Exception occurred while fetching sipQuery");
    }
  }

  /**
   * This method to organize the pivot table structure
   *
   * @param analysis
   * @return
   */
  private List<Field> getPivotFields(Analysis analysis) {
    SipQuery sipQuery = analysis.getSipQuery();
    List<Field> queryFields = sipQuery.getArtifacts().get(0).getFields();
    List<Field> fieldList = new ArrayList<>();
    // set first row fields
    for (Field field : queryFields) {
      if (field != null && "row".equalsIgnoreCase(field.getArea())) {
        fieldList.add(field);
      }
    }
    // set column fields
    for (Field field : queryFields) {
      if (field != null && "column".equalsIgnoreCase(field.getArea())) {
        fieldList.add(field);
      }
    }
    // set data fields
    for (Field field : queryFields) {
      if (field != null && "data".equalsIgnoreCase(field.getArea())) {
        fieldList.add(field);
      }
    }
    return fieldList;
  }

  private String filePath(String type, String fileName) {
    if (type == null) {
      return publishedPath
          + File.separator
          + ExportUtils.generateRandomStringDir()
          + File.separator
          + fileName;
    }
    return publishedPath
        + File.separator
        + type
        + ExportUtils.generateRandomStringDir()
        + File.separator
        + fileName;
  }

  @Override
  @Async
  public void pivotDispatchAsync(String executionId, RequestEntity request, String analysisId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    Object dispatchBean = request.getBody();
    String recipients = null;
    String ftp = null;
    String s3 = null;
    String finalJobGroup = null;
    boolean isZipRequired = false;
    // check beforehand if the request is not null
    if (dispatchBean != null && dispatchBean instanceof LinkedHashMap) {
      Object recipientsObj = ((LinkedHashMap) dispatchBean).get(EMAIL_LIST);
      Object ftpObj = ((LinkedHashMap) dispatchBean).get("ftp");
      Object s3Obj = ((LinkedHashMap) dispatchBean).get("s3");
      if (((LinkedHashMap) dispatchBean).get("zip") != null) {
        isZipRequired = (Boolean) ((LinkedHashMap) dispatchBean).get("zip");
      }

      if (recipientsObj != null) {
        recipients = String.valueOf(recipientsObj);
      }

      if (ftpObj != null) {
        ftp = String.valueOf(ftpObj);
      }

      if (s3Obj != null) {
        s3 = String.valueOf(s3Obj);
      }
      finalJobGroup = String.valueOf(((LinkedHashMap) dispatchBean).get(JOBGROUP));
    }

    logger.debug("recipients: {}", recipients);
    logger.debug("ftp: {}", ftp);
    logger.debug("s3: {}", s3);
    if ( !(StringUtils.isEmpty(recipients))
        || !(StringUtils.isEmpty(s3))
        || !StringUtils.isEmpty(ftp)) {
      ExportBean exportBean = setExportBeanProps(dispatchBean, analysisId);
      String userFileName = exportBean.getFileName();
      logger.trace("File name: {}",userFileName);
      logger.debug("dispatchBean for Pivot: {}", dispatchBean);
      final String dispatchFileName = filePath(null, userFileName);
      exportBean.setFileName(dispatchFileName);
      try {
        Instant startTime = Instant.now();
        streamToXlsxPivot(exportBean.getAnalysis(), executionId, exportBean);
        Instant finishTime = Instant.now();
        long elapsedTime = Duration.between(startTime, finishTime).toMillis();
        logger.trace("time taken for Excel preparation for pivot:{}", elapsedTime);
        if (recipients != null && !recipients.equals("")) {
          logger.debug("mail dispatch started for pivot: ");
          Instant start = Instant.now();
          mailDispatchForPivot(exportBean, recipients, isZipRequired);
          Instant finish = Instant.now();
          long timeElapsed = Duration.between(start, finish).toMillis();
          logger.trace("time taken for mail dispatch for pivot:{}", timeElapsed);
        }
        if (!StringUtils.isEmpty(ftp)) {
          logger.debug("ftp dispatch started for pivot: ");
          Instant start = Instant.now();
          ftpDispatchForPivot(exportBean, isZipRequired, finalJobGroup, ftp);
          Instant finish = Instant.now();
          long timeElapsed = Duration.between(start, finish).toMillis();
          logger.trace("time taken for ftp dispatch for pivot:{}", timeElapsed);
        }

        if (!StringUtils.isEmpty(s3)) {
          logger.debug("S3 dispatch started for pivot");
          Instant start = Instant.now();
          s3DispatchForPivot(exportBean, isZipRequired, finalJobGroup, s3);
          Instant finish = Instant.now();
          long timeElapsed = Duration.between(start, finish).toMillis();
          logger.trace("time taken for s3 dispatch for pivot:{}", timeElapsed);
        }
      } catch (Exception e) {
        logger.error("Exception occured in dispatching pivot:{}", e);
      } finally {
        ExportUtils.deleteDispatchedFile(exportBean.getFileName(), serviceUtils);
      }
    }
  }

  public Boolean streamToXlsxPivot(
      Analysis analysis, String executionId, ExportBean exportBean) {
    long batchSize = exportChunkSize != null ? Long.valueOf(exportChunkSize) : 0l;
    long totalNumberOfBatch = 0;
    String normalizedPath=SipCommonUtils.normalizePath(exportBean.getFileName());
    File file = new File(normalizedPath);
    file.getParentFile().mkdir();
    boolean flag = true;
    long pageNo = 1, totalRowsCount = 0, rowCount = 1;
    XlsxExporter xlsxExporter = new XlsxExporter();
    Workbook workBook = new XSSFWorkbook();
    String sheetName = ExportUtils.prepareExcelSheetName(exportBean.getReportName());
    XSSFSheet sheet = (XSSFSheet) workBook.createSheet(sheetName);
    List<Field> fieldList = getPivotFields(analysis);
    ElasticSearchAggregationParser responseParser = new ElasticSearchAggregationParser(fieldList);
    ResponseEntity<JsonNode> entity = null;
    responseParser.setColumnDataType(exportBean);
    do {
      entity = getExecutionData(executionId, PIVOT_ANALYSIS_TYPE, pageNo, batchSize, JsonNode.class);
      JsonNode jsonDataNode = entity.getBody().get("data");
      if (jsonDataNode == null) {
        logger.debug("Cannot process the dispatch for pivot as data is null");
        return false;
      }
      List<Object> dataObj = responseParser.parsePivotData(jsonDataNode);
      rowCount = flag ? rowCount : rowCount + batchSize;
      logger.trace("Data size = {}", dataObj.size());
      totalRowsCount = entity.getBody().get("totalRows").asLong();
      if (flag) {
        long count = totalRowsCount / batchSize;
        totalNumberOfBatch = totalRowsCount % batchSize != 0 ? count + 1 : count;
        logger.trace("total no of batches:{}", totalNumberOfBatch);
      }
      flag = false;
      xlsxExporter.addxlsxRows(exportBean, workBook, sheet, dataObj, rowCount);
      pageNo++;

    } while (pageNo <= totalNumberOfBatch);
    logger.trace("total no of pages:{}", pageNo);
    logger.debug("Creating pivot table: ");
    CreatePivotTable createPivotTable = new CreatePivotTable();
    createPivotTable.createPivot(workBook, file, fieldList);
    logger.debug(" pivot table created ");
    return true;
  }

  void mailDispatchForPivot(
      ExportBean exportBean,
      String recipients,
      boolean zip) {
    try {
      dispatchMail(exportBean, zip, recipients);
    } catch (Exception e) {
      logger.error("Exception occurred while dispatching Email for pivot:{}", e);
    }
  }

  void ftpDispatchForPivot(
      ExportBean exportBean,
      boolean zip,
      String finalJobGroup,
      String finalFtp) {
    try {
      createZipForFtp(finalFtp, exportBean, finalJobGroup, zip);
    } catch (Exception e) {
      logger.error("Exception occurred while dispatching FTP for pivot:{}", e);
    }
  }

  void s3DispatchForPivot(
      ExportBean exportBean,
      boolean zip,
      String finalJobGroup,
      String finalS3) {
    try {
      dispatchFileToS3(exportBean, finalS3, finalJobGroup, zip);
    } catch (Exception e) {
      logger.error("Exception occurred while dispatching S3 for pivot:{}", e);
    }
  }

  private <T> ResponseEntity<T> getExecutionData(
      String executionId, String analysisType, long pageNo, long batchSize, Class<T> classType) {
    String proxyEndPoint =
        "/internal/proxy/storage/%s/executions/data?page=%s&pageSize=%s&executionType=scheduled&analysisType=%s&internalCall=true";
    String url =
        storageProxyUrl.concat(
            String.format(proxyEndPoint, executionId, pageNo, batchSize, analysisType));
    ResponseEntity<T> entity = null;
      Instant startTime = Instant.now();
      try {
      logger.debug("returning Execution data");
      ResponseEntity<T> responseEntity = restTemplate.getForEntity(url, classType);
      Instant finishTime = Instant.now();
      long elapsedTime = Duration.between(startTime, finishTime).toMillis();
      logger.trace("Time taken for getting Executions data:{}", elapsedTime);
      return responseEntity;
    } catch (Exception e) {
      Instant finishTime = Instant.now();
      long elapsedTime = Duration.between(startTime, finishTime).toMillis();
      logger.trace("Time taken for waiting the response befor exception:{}", elapsedTime);
      logger.error(
          "Exception occured while fetching the execution data for pageNo:{} and batchSize:{} with exception:{}",
          pageNo,
          batchSize,
          e);
    }
    return entity;
  }
}
