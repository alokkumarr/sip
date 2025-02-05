package com.synchronoss.saw.export;

import com.synchronoss.sip.utils.SipCommonUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;

public class SFTPUploader {

    StandardFileSystemManager manager = null;
    String sftpURL = null;

    private static final Logger logger = LoggerFactory.getLogger(SFTPUploader.class);

    public SFTPUploader(String host, int port, String user, String pwd, String privatekeyPath) throws Exception {
        // Username and Password may or may not have spaces so no trimming required
        // URLEncoding them is necessary because of special characters in username / password.
        String username = URLEncoder.encode(user, "UTF-8");
        String password = null;
        if (pwd != null) {
            password= URLEncoder.encode(pwd, "UTF-8");
        }

        boolean privateKeyPresent = privatekeyPath == null ? false: true;

        sftpURL = sftpUrlBuilder(username, password, host, port, privateKeyPresent);
        manager = new StandardFileSystemManager();
    }

    private String sftpUrlBuilder(String username, String password, String host,
        int port, boolean privateKeyPresent) {
        StringBuilder builder =
            new StringBuilder().append("sftp://")
                .append(username);

        if (!privateKeyPresent) {
            builder.append(":").append(password);
        }
        builder.append("@")
            .append(host.trim()).append(":").append(Integer.toString(port))
            .append("/");
        return builder.toString();
    }

    private static FileSystemOptions createFileSystemOptions (final String keyPath,
        final String passPhrase) throws FileSystemException{

        String normalizedKeyPath = SipCommonUtils.normalizePath(keyPath);
        //create options for sftp
        FileSystemOptions options = new FileSystemOptions();
        //ssh key
        SftpFileSystemConfigBuilder sftpBuilder = SftpFileSystemConfigBuilder.getInstance();
        sftpBuilder.setStrictHostKeyChecking(options, "no");
        //set root directory to user home
        sftpBuilder.setUserDirIsRoot(options, false);
        //timeout
        sftpBuilder.setTimeout(options, 600000);

        if (normalizedKeyPath != null) {
            IdentityInfo identityInfo = null;
            if(passPhrase!=null){
                identityInfo = new IdentityInfo(new File(normalizedKeyPath), passPhrase.getBytes());
            }else{
                identityInfo =  new IdentityInfo(new File(normalizedKeyPath));
            }
            SftpFileSystemConfigBuilder.getInstance().setIdentityInfo(options, identityInfo);
        }


        return options;
    }

    public void uploadFile(String localFileFullName, String fileName, String hostDir,
        String privateKeyPath, String passPhrase)
            throws Exception {
        try {
            String normalizedFile= SipCommonUtils.normalizePath(localFileFullName);
            // local zipped
            File file = new File(normalizedFile);
            // URL preparation
            sftpURL = sftpURL + "/" + hostDir.trim() + "/" + fileName.trim();
            manager.init();

            // Filesystem Options
            FileSystemOptions opts = createFileSystemOptions(privateKeyPath, passPhrase);

            // Local and remote file location preparation
            FileObject localFile = manager.resolveFile(file.getAbsolutePath());
            logger.debug(localFile.toString());
            FileObject remoteFile = manager.resolveFile(sftpURL, opts);
            logger.debug(remoteFile.toString());

            logger.debug("Just before uploading it to server");

            // Copy the actual file
            remoteFile.copyFrom(localFile, Selectors.SELECT_SELF);

        } catch (IOException e) {
            logger.error("IO SFTP Error: ", e);
        } catch (Exception e) {
            logger.error("SFTP Error: ", e);
        }
    }

    public void disconnect() {
        if(manager!=null) {
            try {
                manager.close();
                logger.debug("closed sftp connection");
            } catch (Exception e) {
                logger.debug("Exception in SFTP disconnection: " + e.getMessage());
            }
        }
    }
}
