package com.synchronoss.saw.workbench.service;

import org.springframework.web.multipart.MultipartFile;

import com.synchronoss.saw.workbench.model.Inspect;
import com.synchronoss.saw.workbench.model.Project;

public interface SAWWorkbenchService {
  
  public Project readDirectoriesByProjectId(Project project) throws Exception;
  public Project createDirectoryProjectId(Project project)throws Exception;
  public Project uploadFilesDirectoryProjectId(Project project, MultipartFile[] uploadfiles)throws Exception;
  public Project uploadFilesDirectoryProjectIdAsync(Project project, MultipartFile[] uploadfiles)throws Exception;
  public Project previewFromProjectDirectoybyId(Project project)throws Exception;
  public Inspect inspectFromProjectDirectoybyId(Inspect inspect)throws Exception;
  public Project readSubDirectoriesByProjectId(Project project) throws Exception;
}
