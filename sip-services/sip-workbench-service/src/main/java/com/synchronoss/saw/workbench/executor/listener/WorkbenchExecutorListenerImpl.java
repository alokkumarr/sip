package com.synchronoss.saw.workbench.executor.listener;

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.esotericsoftware.minlog.Log;
import com.google.gson.JsonSyntaxException;
import com.mapr.streams.Admin;
import com.mapr.streams.StreamDescriptor;
import com.mapr.streams.Streams;
import com.synchronoss.saw.workbench.executor.service.WorkbenchExecutionType;
import com.synchronoss.saw.workbench.service.WorkbenchJobService;
import com.synchronoss.saw.workbench.service.WorkbenchJobServiceImpl;

import sncr.bda.core.file.HFileOperations;

@Service
/**
 * 
 * Listener polls for messages coming to queue
 * and process by invoking XDF component or 
 * pipeline.
 *
 */
public class WorkbenchExecutorListenerImpl implements  WorkbenchExecutorListener{
	
	 private static final Logger logger = LoggerFactory.getLogger(WorkbenchExecutorListenerImpl.class);
	 
	  @Value("${workbench.project-root}")
	  @NotNull
	  private String root;


	  @Value("${workbench.stream.base-path}")
	  @NotNull
	  private String streamBasePath;
	  
	  private String workbenchTopics= null;
	  
	  @Autowired
	  WorkbenchJobService service;
	  
	  private String workbenchStream;


	  /**
	   * Init method for listener.
	   *
	   * @throws Exception if unbale to create the stream.
	   */
	  @PostConstruct
	  public void init() {
		logger.debug("#####Inside post construct of listener #####");
		
	     
	      this.workbenchStream = this.streamBasePath
	    		  + File.separator
	    		  + "sip-workbench-executor";
	      this.workbenchTopics = workbenchStream + ":executions";
	  }
	  
	  

	@Override
	public void createIfNotExists(int retries) throws Exception {

		
		 
	    try {
	      HFileOperations.createDir(streamBasePath);
	    } catch (Exception e) {
	      if (retries == 0) {
	        logger.error("unable to create path for workbench executor stream for path : " + streamBasePath);
	        throw e;
	      }
	      Thread.sleep(5 * 1000);
	      createIfNotExists(retries - 1);
	    }
	    Configuration conf = new Configuration();
	    Admin streamAdmin = Streams.newAdmin(conf);
	    if (!streamAdmin.streamExists(workbenchStream)) {
	      StreamDescriptor streamDescriptor = Streams.newStreamDescriptor();
	      try {
	    	logger.debug("####Stream not exists. Creating stream ####" + workbenchStream);
	        streamAdmin.createStream(workbenchStream, streamDescriptor);
	        logger.debug("####Stream created Successfully!! ####"+  workbenchStream);
	      } catch (Exception e) {
	    	  logger.error("unable to create stream ..."+ workbenchStream);
	        if (retries == 0) {
	          logger.error("Error unable to create the workbench stream no reties left: " + e);
	          throw e;
	        }
	        logger.warn("unable to create the workbench stream leftover reties : " + retries);
	        Thread.sleep(5 * 1000);
	        createIfNotExists(retries - 1);
	      } finally {
	        streamAdmin.close();
	      }
	    }
	    
	    
	    
	  		
	}

	@Override
	public void runWorkbenchConsumer() throws Exception {
		
		   try {
		     createIfNotExists(10);
		   } catch (Exception e) {
		    logger.error("Error occurred while initializing the workbench executor stream ", e);
		   }

	      logger.debug("Starting receive:");
	      Properties properties = new Properties();
	      properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "sip-workbench");
	      properties.setProperty(
	          ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
	      properties.setProperty(
	          ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
	      properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
	      
	      logger.debug("######### Creating consumer ######");
	      KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
	      logger.debug("######### Consumer  created ######" + consumer);
	      logger.debug("######### Subscribing to topic  ::"+ this.workbenchTopics);
	      consumer.subscribe(Collections.singletonList(this.workbenchTopics));
	      logger.debug("######### Subscribing completed!!  $##########");
	      receiveMessages(consumer);
	    }

	
	/**
	   * Method to receive consumer messages.
	   *
	   * @param consumer consumer
	   * @throws Exception when unable to process the messages.
	   */
	  private void receiveMessages(KafkaConsumer<String, String> consumer) {
		logger.debug("Inside recieve messages");
		logger.debug("########Consumer ::"+ consumer);
	    long pollTimeout = 60 * 60 * 1000;
	    while (true) {
	    	logger.debug("Inside while loop");
	      ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
	      logger.debug("################Inside polling for  messages");
	      logger.debug("#################Number of records recieved in polling"+ records.count());
	      records.forEach(
	          record-> {
	        	  ExecutorService executor = Executors.newFixedThreadPool(10);
	        	  Future<Long> result = executor.submit(new Callable<Long>() {
						@Override
						public Long call() throws Exception {
							 logger.debug("Consumer processing message....");
							try {
				        		  String[] content =   record.value().split("˜˜");
				        		  
				        		  logger.debug("#### content ::"+ content);
				        		  
				        		  WorkbenchExecutionType executionType = WorkbenchExecutionType.valueOf(content[0]);
				        		  
				        		  
				        		  switch(executionType) {
				        		  case EXECUTE_JOB:
				        			  logger.debug("Processing exeucte job type in consumer ....");

					        		  if(content.length == 5) {
					        			  	String batchID = new DateTime().toString("yyyyMMdd_HHmmssSSS");
					        		  
							        		  String project= content[1];
							    			  String name = content[2];
							    			  String component = content[3];
							    			  String cfg = content[4];
							    			  service.executeJob(root, cfg, project, component, batchID);
							    			  
					        		  }
					        		  break;
					        		  
					        		  /**
					        		   * Any additional future usecases goes here
					        		   */
					        		  
								default:
									break;
				        		  }
				        		  
				        	  } catch (JsonSyntaxException exception) {
								logger.error(exception.getMessage());
							} catch (Exception exception) {
								logger.error(exception.getMessage());
							}
							return pollTimeout;
				          }
	        	  });

					});

	      consumer.commitAsync();
	      logger.debug("####End of while loop one iteration");
	    }
	  }

	
}
