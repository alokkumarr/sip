package com.synchronoss.saw.scheduler.service;

import com.synchronoss.saw.scheduler.modal.SchedulerJobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisServiceImpl.class);

    @Value("${saw-analysis-service-url}")
    private String analysisUrl;

    @Value("${saw-dispatch-service-url}")
    private String dispatchUrl;
    private RestTemplate restTemplate;

    private final Logger log = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    public AnalysisServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder.build();
    }


    public void executeAnalysis(String analysisId) {
        AnalysisExecution execution = ImmutableAnalysisExecution.builder()
            .type("scheduled").build();
        String url = analysisUrl + "/{analysisId}/executions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalysisExecution> entity = new HttpEntity<>(
            execution, headers);
        restTemplate.postForObject(url, entity, String.class, analysisId);
    }

    public void scheduleDispatch(SchedulerJobDetail analysis) {
        if (analysis.getDescription() == null) analysis.setDescription("");

        // in case if reading recipients list raises exception. Don't skip the scheduler processing
        String recipients = null;
        try {
            recipients = prepareStringFromList(analysis.getEmailList());
        } catch (Exception e) {
            logger.error("Error reading recipients list: "+ e.getMessage());
            recipients = "";
        }

        // in case if reading of ftp servers raises exception. Don't skip the scheduler processing
        String ftpServers = null;
        try {
            ftpServers = prepareStringFromList(analysis.getFtp());
        } catch (Exception e) {
            logger.error("Error reading ftp servers list: "+ e.getMessage());
            ftpServers = "";
        }

        ExecutionBean[] executionBeans = fetchExecutionID(analysis.getAnalysisID());
        String[] latestexection = findLatestExecution(executionBeans);
        Date date = new Date(Long.parseLong(latestexection[1]));
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        String formatted = format.format(date);
        DispatchBean execution;

        if (!recipients.equals("") && !ftpServers.equals("")) {
            execution = ImmutableDispatchBean.builder()
                    .emailList(recipients).fileType(analysis.getFileType())
                    .description(analysis.getDescription())
                    .name(analysis.getAnalysisName())
                    .userFullName(analysis.getUserFullName())
                    .metricName(analysis.getMetricName())
                    .ftp(ftpServers)
                    .jobGroup(analysis.getJobGroup())
                    .publishedTime(formatted).build();
        } else if (!recipients.equals("")) {
            execution = ImmutableDispatchBean.builder()
                    .emailList(recipients).fileType(analysis.getFileType())
                    .description(analysis.getDescription())
                    .name(analysis.getAnalysisName())
                    .userFullName(analysis.getUserFullName())
                    .metricName(analysis.getMetricName())
                    .jobGroup(analysis.getJobGroup())
                    .publishedTime(formatted).build();
        } else if (!ftpServers.equals("")) {
            execution = ImmutableDispatchBean.builder()
                    .description(analysis.getDescription())
                    .fileType(analysis.getFileType())
                    .name(analysis.getAnalysisName())
                    .userFullName(analysis.getUserFullName())
                    .metricName(analysis.getMetricName())
                    .ftp(ftpServers)
                    .jobGroup(analysis.getJobGroup())
                    .publishedTime(formatted).build();
        } else {
            execution = ImmutableDispatchBean.builder()
                    .description(analysis.getDescription())
                    .name(analysis.getAnalysisName())
                    .userFullName(analysis.getUserFullName())
                    .metricName(analysis.getMetricName())
                    .jobGroup(analysis.getJobGroup())
                    .publishedTime(formatted).build();
        }
        String[] param = new String[3];
        param[0] = analysis.getAnalysisID();
        param[1] = latestexection[0];
        param[2] = analysis.getType();
        String url = dispatchUrl + "/{analysisId}/executions/{executionId}/dispatch/{type}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DispatchBean> entity = new HttpEntity<>(
                execution, headers);

        if (latestexection[0] != null) {
            restTemplate.postForObject(url, entity, String.class, param);
        }
    }

    private ExecutionBean[] fetchExecutionID(String analysisId)
    {
        String url = analysisUrl + "/{analysisId}/executions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
       return restTemplate.getForObject(url, ExecutionResponse.class, analysisId).executions();

    }

    private String[] findLatestExecution(ExecutionBean[] executionBeans)
    {
        String latestExecutionID = null;
        String latestFinish =null;

        /** TO DO : pivot Analysis does not contains execution status , It may bug in system
         *   consider status by-default as success if execution doesn't contains status
         */

        if (executionBeans.length>0) {
            // Initialize latestExecution.
            latestExecutionID = executionBeans[0].id();
            latestFinish = executionBeans[0].finished();
            for (ExecutionBean executionBean : executionBeans) {
               if (Long.parseLong(executionBean.finished()) > Long.parseLong(latestFinish)
                       && (executionBean.status()==null || executionBean.status().equalsIgnoreCase("Success")))
               {
                   latestExecutionID = executionBean.id();
                   latestFinish = executionBean.finished();
               }
            }
        }
         String[] val = new String [2];
        val[0] = latestExecutionID;
        val[1] = latestFinish;
        return val;
    }

    private String prepareStringFromList(List<String> source) {
        return String.join(",", source);
    }

}
