package com.synchronoss.saw.logs.repository;

import com.synchronoss.saw.logs.entities.BisFileLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



@Repository
public interface BisFileLogsRepository extends JpaRepository<BisFileLog, String> {

  BisFileLog findByPid(String pid);

  @Query("SELECT COUNT(pid)>0 from BisFileLog Logs where Logs.fileName = :fileName "
      + "and Logs.mflFileStatus != 'FAILED' ")
  boolean isFileNameExists(@Param("fileName") String fileName);

  List<BisFileLog> findByRouteSysId(long routeSysId);

  @Query("SELECT Logs from BisFileLog Logs where (TIMEDIFF(NOW(), Logs.checkpointDate))/60 "
      + "> :noOfMinutes and (Logs.mflFileStatus != 'SUCCESS' and Logs.mflFileStatus != 'FAILED') ")
  Page<BisFileLog> retryIds(@Param("noOfMinutes") Integer noOfMinutes, Pageable pageable);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE BisFileLog Logs SET Logs.mflFileStatus = :status WHERE Logs.pid = :pid")
  Integer updateBislogsStatus(@Param("status") String status, @Param("pid") String pid);

  @Query("SELECT COUNT(pid) from BisFileLog Logs where (TIMEDIFF(NOW(),Logs.checkpointDate))/60 "
      + " > :noOfMinutes and (Logs.mflFileStatus != 'SUCCESS' and Logs.mflFileStatus != 'FAILED') ")
  Integer countOfRetries(@Param("noOfMinutes") Integer noOfMinutes);


}
