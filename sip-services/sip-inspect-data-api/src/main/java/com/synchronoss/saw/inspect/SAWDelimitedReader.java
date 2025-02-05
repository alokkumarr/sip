package com.synchronoss.saw.inspect;
import com.synchronoss.sip.utils.SipCommonUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sncr.bda.core.file.HFileOperations;

public class SAWDelimitedReader {

  private static final org.apache.log4j.Logger logger =
      org.apache.log4j.Logger.getLogger(SAWDelimitedReader.class);

  private String path;
  private boolean localFileSystem =false;
  private List<String> fileLines = new ArrayList<String>();

  public static void main(String[] args) {
    try {
      SAWDelimitedReader parser = new SAWDelimitedReader("/Users/spau0004/Desktop/crime.csv", 4, true);
      String str = parser.toJson();
      System.out.println(str);
      BufferedWriter writer =new BufferedWriter(new FileWriter("/Users/spau0004/Desktop/result.json"));
      writer.write(str);
      writer.close();
    } catch (Exception e) {
      logger.error("Exception while reading the raw file", e);
    }
    System.out.println("Total memory : " + Runtime.getRuntime().totalMemory());
    System.out.println("Free memory  : " + Runtime.getRuntime().freeMemory());
    System.out.println("Max memory   : " + Runtime.getRuntime().maxMemory());
    System.exit(0);
  }

  public SAWDelimitedReader(String root, long sampleSize, boolean localFileSystem) throws Exception {
    this.localFileSystem = localFileSystem;
    String tmp = "";
    if (root != null) {
      if (!this.localFileSystem){
      tmp = root + Path.SEPARATOR;}
      else {
        tmp = root;
      }
    }
    this.path = getFilePath(tmp);
    fileLines = readStream(getReader(this.path),sampleSize);
  }

  private String getFilePath(String path) throws Exception {
    String normalizedPath = SipCommonUtils.normalizePath(path);
    String filePath = null;
    if (!this.localFileSystem) {
      FileSystem fs = HFileOperations.getFileSystem();
      if (fs != null)
        try {
          FileStatus[] plist = fs.globStatus(new Path(normalizedPath));
          for (FileStatus f : plist) {
            if (f.isFile()) {
              filePath = f.getPath().toString();
            }
          }
        } catch (IOException e) {
          logger.error("Exception occured while the reading the files from fileSystem", e);
        }
    } else {
      File file = new File(normalizedPath);
      if (!file.isDirectory() && !file.isFile()) {
        String extension = FilenameUtils.getExtension(normalizedPath);
        String basePath = FilenameUtils.getFullPathNoEndSeparator(normalizedPath);
        File dir = new File(basePath);
        final FilenameFilter filter = new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.endsWith(extension);
          }
        };
        File[] list = dir.listFiles(filter);
        if (list != null && list.length > 0) {
          filePath = list[0].getAbsolutePath();
        }
      } else {
        if (file.isDirectory()) {
          throw new FileNotFoundException(
              "Preview raw data cannot be done at directory. We need file for that");
        }
        if (file.isFile()) {
          filePath = file.getAbsolutePath();
        }
      }
    }
    return filePath;
  }

  private Reader getReader(String path) throws Exception {
    String normalizedPath = SipCommonUtils.normalizePath(path);
    InputStream inputStream = null;
    if (!this.localFileSystem) {
      inputStream = HFileOperations.readFileToInputStream(normalizedPath);
      return new InputStreamReader(inputStream, "UTF-8");
    } else {
      File file = new File(normalizedPath);
      inputStream = new FileInputStream(file);
      return new InputStreamReader(inputStream, "UTF-8");
    }
  }

  public String toJson() throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().setVersion(1.0).create();
    JsonElement formatElement = gson.toJsonTree(fileLines);
    JsonObject object = new JsonObject();
    object.add("samples", formatElement);
    return gson.toJson(object);
  }
  
  public List<String> readFirst(final java.nio.file.Path path, final int numLines)
     throws IOException {
      try (final Stream<String> lines = Files.lines(path)) {
          return lines.limit(numLines).collect(Collectors.toList());
      }
  }
  
  public List<String> readStream(final Reader path, final long numLines)
      throws IOException {
    try (BufferedReader buffer = new BufferedReader(path)) {
      return buffer.lines().limit(numLines).collect(Collectors.toList());
  }
   }
  
  }

