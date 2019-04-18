package com.synchronoss.saw.batch.plugin;

import com.synchronoss.saw.batch.extensions.SipPluginContract;
import com.synchronoss.saw.ingestion.s3.connections.S3ServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;



@Service
public class SipIngestionPluginFactory {

  @Autowired
  @Qualifier("sftpService")
  private SipPluginContract sftpServiceImpl;

  @Autowired
  S3ServiceImpl s3ConnsService;

  private static final Logger logger = LoggerFactory.getLogger(SipIngestionPluginFactory.class);

  /**
   * Retrive instance based on ingestion type.
   * 
   * @param ingestionType channel type
   * @return pluginContract
   */
  public SipPluginContract getInstance(String ingestionType) {

    SipPluginContract sipConnectionService = null;

    if (ingestionType.toUpperCase().equals(ChannelType.SFTP.name())) {
      sipConnectionService = this.sftpServiceImpl;
    } else if (ingestionType.equals(ChannelType.S3.name())) {
      sipConnectionService = this.s3ConnsService;
    }

    return sipConnectionService;

  }

}
