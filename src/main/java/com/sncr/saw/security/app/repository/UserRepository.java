package com.sncr.saw.security.app.repository;

import java.util.List;

import com.sncr.saw.security.common.bean.ResetValid;
import com.sncr.saw.security.common.bean.Role;
import com.sncr.saw.security.common.bean.Ticket;
import com.sncr.saw.security.common.bean.User;
import com.sncr.saw.security.common.bean.Valid;
import com.sncr.saw.security.common.bean.repo.admin.role.RoleDetails;
import com.sncr.saw.security.common.bean.repo.analysis.Analysis;
import com.sncr.saw.security.common.bean.repo.analysis.AnalysisSummaryList;

public interface UserRepository {
	void insertTicketDetails(Ticket ticket) throws Exception;
	boolean[] authenticateUser(String masterLoginId, String password);
	void prepareTicketDetails(User user, Boolean onlyDef);
	void invalidateTicket(String ticketId, String validityMessage);
	String verifyUserCredentials(String masterLoginId, String eMail,
			String firstName);
	String updateUserPass(String masterLoginId, String newPassEncrp);
	Ticket getTicketDetails(String ticketId);
	String changePassword(String loginId, String newPass, String oldPass);
	String rstchangePassword(String loginId, String newPass);
	String getUserEmailId(String userId);
	void insertResetPasswordDtls(String userId, String randomHash,
			Long createdTime, long validUpto);
	ResetValid validateResetPasswordDtls(String randomHash);
	boolean createAnalysis (Analysis analysis);
	boolean updateAnalysis(Analysis analysis);
	boolean deleteAnalysis(Analysis analysis);
	AnalysisSummaryList getAnalysisByFeatureID (Long featureId);
	List<User> getUsers(Long customerId);
	Valid addUser(User user);
	boolean updateUser(User user);
	boolean deleteUser(Long userId, String masterLoginId);
	List<Role> getRolesDropDownList(Long customerId);
	List<RoleDetails> getRoles(Long customerId);
	List<Role> getRoletypesDropDownList();
	Valid addRole(RoleDetails role);
	boolean deleteRole(Long roleId, String masterLoginId);
	boolean updateRole(RoleDetails role);
	boolean checkUserExists(Long roleId);
	boolean checkPrivExists(Long roleId);
}
