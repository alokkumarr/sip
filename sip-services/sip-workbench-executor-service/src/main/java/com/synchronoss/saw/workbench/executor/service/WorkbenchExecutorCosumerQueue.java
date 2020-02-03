package com.synchronoss.saw.workbench.executor.service;

import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapr.streams.Admin;
import com.mapr.streams.StreamDescriptor;
import com.mapr.streams.Streams;
import com.synchronoss.saw.workbench.executor.SparkConfig;
import com.synchronoss.saw.workbench.executor.listener.WorkbenchExecutorListener;
import com.synchronoss.saw.workbench.executor.listener.WorkbenchExecutorListenerImpl;

import sncr.bda.core.file.HFileOperations;

public class WorkbenchExecutorCosumerQueue {

	private static final Logger logger = LoggerFactory.getLogger(WorkbenchExecutorCosumerQueue.class);
	private String streamBasePath = "";
	private String project = "";
	private String name = "";
	private String component = "";
	private String cfg = "";
	private String topic = "";
	private String workbenchExecutorStream = "";
	KafkaProducer<String, String> producer = null;
	WorkbenchExecutorListener listener = new WorkbenchExecutorListenerImpl();

	public void init() {
		logger.debug("#### Project root ###"+ SparkConfig.configParams.get("workbench.project-root"));
		logger.info("#### Post construct of WorkbenchQueue Manager");
		String sipBasePath = "";

		this.streamBasePath = sipBasePath + File.separator + "services/workbench/executor";
		this.workbenchExecutorStream = this.streamBasePath + File.separator + "sip-workbench-executor";
		this.topic = workbenchExecutorStream + ":executions";

		try {
			createIfNotExists(10);
		} catch (Exception e) {
			logger.error("unable to create path for alert stream : " + this.streamBasePath);
		}
	}

	/**
	 * Create required MapR streams if they do not exist.
	 *
	 * @param retries number of retries.
	 * @throws Exception when unable to create stream path.
	 */
	private void createIfNotExists(int retries) throws Exception {
		try {
			HFileOperations.createDir(streamBasePath);
		} catch (Exception e) {
			if (retries == 0) {
				logger.error("unable to create path for alert stream for path : " + streamBasePath);
				throw e;
			}
			Thread.sleep(5 * 1000);
			createIfNotExists(retries - 1);
		}
		Configuration conf = new Configuration();
		Admin streamAdmin = Streams.newAdmin(conf);
		if (!streamAdmin.streamExists(workbenchExecutorStream)) {
			StreamDescriptor streamDescriptor = Streams.newStreamDescriptor();
			try {
				logger.debug("####Stream not exists. Creating stream ####");
				streamAdmin.createStream(workbenchExecutorStream, streamDescriptor);
				logger.debug("####Stream created Successfully!! ####");
			} catch (Exception e) {

				if (retries == 0) {
					logger.error("Error unable to create the alert stream no reties left: " + e);
					throw e;
				}
				logger.warn("unable to create the alert stream leftover reties : " + retries);
				Thread.sleep(5 * 1000);
				createIfNotExists(retries - 1);
			} finally {
				streamAdmin.close();
			}
		}
		logger.info("####### Starting workbench consumer thread....");

		/*
		 * if(producer == null) { Properties properties = new Properties();
		 * properties.setProperty( "key.serializer",
		 * "org.apache.kafka.common.serialization.StringSerializer");
		 * properties.setProperty( "value.serializer",
		 * "org.apache.kafka.common.serialization.StringSerializer");
		 * 
		 * producer = new KafkaProducer(properties); }
		 */

	}

	/*
	 * public boolean sendWorkbenchMessageToStream(String recordContent) {
	 * 
	 * 
	 * logger.debug("######## Sending record content to kafka Queue ######" );
	 * 
	 * ProducerRecord<String, String> record = new ProducerRecord<>(this.topic,
	 * recordContent); logger.debug("#### Sending to topic ####"+ this.topic);
	 * 
	 * logger.debug("Writing data to stream " + record); producer.send(record);
	 * producer.flush();
	 * logger.debug("######## Sent record content to kafka Queue ######" ); return
	 * true; }
	 */

	public void lauchConsumer() {
		Runnable r = () -> {
			try {

				listener.runWorkbenchConsumer();
			} catch (Exception e) {
				logger.error("Error occurred while running the stream consumer : " + e);
			}
		};
		new Thread(r).start();
		logger.info("#######Workbench consumer thread started");
	}
}
