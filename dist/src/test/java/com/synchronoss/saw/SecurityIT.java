package com.synchronoss.saw;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.replacePattern;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;

/** Integration test that test the Security Service. */
public class SecurityIT extends BaseIT {
  private final Logger log = LoggerFactory.getLogger(getClass().getName());
  private static final String userName = "sawadmin@synchronoss.com";
  private static final String password = "Sawsyncnewuser1!";
  private static final String invalidPwd = "111";
  private static final String invalidLoginMessage = "Invalid User Credentials";
  private static final String userLockedMessage =
      "Account has been locked!!, Please try after sometime";
  private static final String userAuthenticatedMsg = "User Authenticated Successfully";

  @Test
  public void testAddCategory() throws JsonProcessingException {
    ObjectNode root = mapper.createObjectNode();
    root.put("moduleId", 1);
    root.put("categoryName", "Test");
    root.put("categoryDesc", "Test");
    root.put("productId", 1);
    root.put("customerId", 1);
    root.put("activeStatusInd", 1);
    root.put("masterLoginId", "test@example.com");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(root)
        .when()
        .post("/security/auth/admin/cust/manage/categories/add")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));
  }

  @Test
  public void testModulePrivileges() {
    given(authSpec)
        .when()
        .get("/security/auth/admin/modules/module-privileges")
        .then()
        .assertThat()
        .statusCode(200);

    given(authSpec)
        .when()
        .get("/security/auth/admin/modules/module-privileges/1")
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  public void testSecurityGroup() {
    ObjectNode secGroup = mapper.createObjectNode();
    secGroup.put("description", "TestDesc");
    secGroup.put("securityGroupName", "TestGroup");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(secGroup)
        .when()
        .post("/security/auth/admin/security-groups")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    given(authSpec)
        .when()
        .get("/security/auth/admin/security-groups")
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  public void testDskAttributeValues() {
    ObjectNode secGroup = mapper.createObjectNode();
    secGroup.put("description", "TestDesc1");
    secGroup.put("securityGroupName", "TestGroup1");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(secGroup)
        .when()
        .post("/security/auth/admin/security-groups")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    Response response =
        given(authSpec)
            .when()
            .get("/security/auth/admin/security-groups")
            .then()
            .statusCode(200)
            .extract()
            .response();
    ArrayNode node = response.as(ArrayNode.class);
    JsonNode jsonNode = node.get(0);
    Long groupSysId = jsonNode.get("secGroupSysId").asLong();

    ObjectNode root = mapper.createObjectNode();
    root.put("attributeName", "TestAttr1");
    root.put("value", "TestValue1");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(root)
        .when()
        .post("/security/auth/admin/security-groups/" + groupSysId + "/dsk-attribute-values")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    given(authSpec)
        .when()
        .get("/security/auth/admin/security-groups/" + groupSysId + "/dsk-attribute-values")
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  public void testAssignGroupUser() {
    ObjectNode secGroup = mapper.createObjectNode();
    secGroup.put("description", "TestDesc2");
    secGroup.put("securityGroupName", "TestGroup2");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(secGroup)
        .when()
        .post("/security/auth/admin/security-groups")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    ObjectNode user = mapper.createObjectNode();
    user.put("customerId", "1");
    user.put("roleId", "3");
    user.put("middleName", "");
    user.put("firstName", "TestSipUser");
    user.put("lastName", "dsk");
    user.put("masterLoginId", "TestSipUser.dsk");
    user.put("password", "Sawsyncnewuser1!");
    user.put("email", "Prabhulingappa.AS@synchronoss.com");
    user.put("activeStatusInd", "1");
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(user)
        .when()
        .post("/security/auth/admin/cust/manage/users/add")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    Response response =
        given(authSpec)
            .when()
            .get("/security/auth/admin/user-assignments")
            .then()
            .statusCode(200)
            .extract()
            .response();
    ArrayNode node = response.as(ArrayNode.class);
    Long userSysId = null;
    for (int i = 0; i < node.size(); i++) {
      if (node.get(i).path("firstName").asText().equals("TestSipUser")) {
        userSysId = node.get(i).path("userSysId").asLong();
        break;
      }
    }

    given(authSpec)
        .body("TestGroup2")
        .when()
        .put("/security/auth/admin/users/" + userSysId + "/security-group")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    given(authSpec)
        .body("-1")
        .when()
        .put("/security/auth/admin/users/" + userSysId + "/security-group")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));

    Response secGroupResponse =
        given(authSpec)
            .when()
            .get("/security/auth/admin/security-groups")
            .then()
            .statusCode(200)
            .extract()
            .response();
    ArrayNode groupNode = secGroupResponse.as(ArrayNode.class);
    Long gid = null;
    for (int i = 0; i < groupNode.size(); i++) {
      if (groupNode.get(i).path("securityGroupName").asText().equals("TestGroup2")) {
        gid = groupNode.get(i).path("secGroupSysId").asLong();
        break;
      }
    }

    given(authSpec)
        .body("TestGroup2")
        .when()
        .delete("/security/auth/admin/security-groups/" + gid)
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  public void testLockUserForMaxInvalidAttempts()
      throws JsonProcessingException, InterruptedException {
    log.info("Test for Invalid Login attempts starts: ");
    log.debug("Trying to login with username : {} password : {} ", userName, invalidPwd);
    Response response = testAuthentication(userName, invalidPwd);
    Assert.assertNotNull(response);
    ObjectNode root = response.getBody().as(ObjectNode.class);
    Boolean validity = root.get("validity").asBoolean();
    Assert.assertEquals(validity, false);
    String message = root.get("message").asText();
    Assert.assertEquals(message, invalidLoginMessage);
    log.info("Invalid Credentials = validity : {}, message : {}", validity, invalidLoginMessage);

    log.debug("Trying to login with username : {} password : {} ", userName, invalidPwd);
    response = testAuthentication(userName, invalidPwd);
    Assert.assertNotNull(response);
    root = response.getBody().as(ObjectNode.class);
    validity = root.get("validity").asBoolean();
    message = root.get("message").asText();
    Assert.assertEquals(validity, false);
    Assert.assertEquals(message, userLockedMessage);
    log.info(
        "Max Attempts Reached, user Account Locked = validity : {}, message : {}",
        validity,
        userLockedMessage);

    log.info("Wait for : {} minutes, to login with correct credentials");
    // Wait for two minutes to check user able to login provided he gives correct credentials.
    TimeUnit.MINUTES.sleep(2);
    log.debug("Trying to login with username : {} password : {} ", userName, password);
    Response response1 = testAuthentication(userName, password);
    Assert.assertNotNull(response1);
    ObjectNode root1 = response1.getBody().as(ObjectNode.class);
    Boolean validity1 = root1.get("validity").asBoolean();
    String message1 = root1.get("message").asText();
    Assert.assertEquals(validity1, true);
    Assert.assertEquals(message1, userAuthenticatedMsg);
    log.info(
        "User successfully authenticated = validity : {}, message : {}",
        validity1,
        userAuthenticatedMsg);
  }

  /**
   * Test for /doAuth api by providing different inputs and test the behavior.
   *
   * @param uname Username
   * @param pwd Password
   * @return Response Object
   * @throws JsonProcessingException Exception
   */
  public Response testAuthentication(String uname, String pwd) throws JsonProcessingException {
    ObjectNode node = mapper.createObjectNode();
    node.put("masterLoginId", uname);
    node.put("password", pwd);
    String json = mapper.writeValueAsString(node);
    Response response =
        given(spec)
            .accept("application/json")
            .header("Content-Type", "application/json")
            .body(json)
            .filter(
                document(
                    "authenticate",
                    preprocessRequest(
                        preprocessReplace(userName, "user@example.com"),
                        preprocessReplace(password, "password123"))))
            .when()
            .post("/security/doAuthenticate")
            .then()
            .assertThat()
            .statusCode(200)
            .body("aToken", startsWith(""))
            .extract()
            .response();
    return response;
  }

  @Test
  public void createRolePrivilegesCategory() {

    Response response = given(authSpec)
        .contentType(ContentType.JSON)
        .body(roleCategoryPrivilege())
        .when()
        .post("/security/auth/external/createRoleCategoryPrivilege")
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .response();

    Assert.assertNotNull(response);
    ObjectNode apiResponse = response.getBody().as(ObjectNode.class);
    String moduleName = apiResponse.get("moduleName").asText();
    Assert.assertEquals("OBSERVE", moduleName);
    String productName = apiResponse.get("productName").asText();
    Assert.assertEquals("SAW Demo", productName);
  }

  @Test
  public void fetchRolePrivilegesCategory() {

    Response response = given(authSpec)
        .contentType(ContentType.JSON)
        .body(roleCategoryPrivilege())
        .when()
        .get("/security/auth/external/fetchRoleCategoryPrivilege")
        .then()
        .assertThat()
        .statusCode(200)
        .extract()
        .response();

    Assert.assertNotNull(response);
    ObjectNode apiResponse = response.getBody().as(ObjectNode.class);
    String moduleName = apiResponse.get("moduleName").asText();
    Assert.assertEquals("OBSERVE", moduleName);
    String productName = apiResponse.get("productName").asText();
    Assert.assertEquals("SAW Demo", productName);
  }

  /**
   * preprocessReplace Util for String.
   *
   * @param from from String
   * @param to To String
   * @return OperationPreprocessor Object
   */
  public OperationPreprocessor preprocessReplace(String from, String to) {
    return replacePattern(Pattern.compile(Pattern.quote(from)), to);
  }

  /**
   * Build node of role, category, privileges.
   *
   * @return object Node
   */
  private ObjectNode roleCategoryPrivilege() {
    ObjectNode objectNode = mapper.createObjectNode();
    objectNode.put("moduleName", "OBSERVE");
    objectNode.put("productName", "SAW Demo");

    ObjectNode roleDetails = mapper.createObjectNode();
    roleDetails.put("activeStatusInd", false);
    roleDetails.put("autoCreate", true);
    roleDetails.put("customerCode", "SYNCHRONOSS");
    roleDetails.put("roleDesc", "Admin User");
    roleDetails.put("roleName", "ROLENAME");
    roleDetails.put("roleType", "ADMIN");
    objectNode.set("role", roleDetails);

    ObjectNode categoryDetail = mapper.createObjectNode();
    categoryDetail.put("autoCreate", true);
    categoryDetail.put("categoryDesc", "Category Description");
    categoryDetail.put("categoryName", "NewCategory");
    categoryDetail.put("categoryType", "000121");

    ObjectNode subCategoryDetail = mapper.createObjectNode();
    subCategoryDetail.put("autoCreate", true);

    ArrayNode privileges = subCategoryDetail.putArray("privilege");
    privileges.add("ALL");

    subCategoryDetail.put("subCategoryDesc", "Sub Category Description");
    subCategoryDetail.put("subCategoryName", "New_Sub_Category");

    ArrayNode subCategory = categoryDetail.putArray("subCategories");
    subCategory.add(subCategoryDetail);

    ArrayNode category = objectNode.putArray("categories");
    category.add(categoryDetail);
    return objectNode;
  }

  @Test
  public void testAddUsersWithDsk() {
    ObjectNode secGroup = mapper.createObjectNode();
    secGroup.put("description", "TestDesc");
    secGroup.put("securityGroupName", "TestForSecGrp");
    addSecurityGroup(secGroup);
    ObjectNode users = mapper.createObjectNode();
    users.put("firstName", "Anil");
    users.put("middleName", "A");
    users.put("lastName", "Deshagani");
    users.put("masterLoginId", "Anil.Deshagani");
    users.put("password", "Sawsyncnewuser1!");
    users.put("email", "sawadminq@synchronoss.com");
    users.put("activeStatusInd", 1);
    users.put("customerCode", "synchronoss");
    users.put("roleName", "ADMIN");
    users.put("securityGroupName", "TestForSecGrp");
    Response userRes =
        given(authSpec)
            .contentType(ContentType.JSON)
            .body(users)
            .when()
            .post("/security/auth/admin/cust/manage/external/users/create")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response();
    JsonNode userNode = userRes.as(JsonNode.class);
    Response secGroupResponse =
        given(authSpec)
            .when()
            .get("/security/auth/admin/security-groups")
            .then()
            .statusCode(200)
            .extract()
            .response();
    ArrayNode groupNode = secGroupResponse.as(ArrayNode.class);
    Long gid = null;
    for (int i = 0; i < groupNode.size(); i++) {
      if (groupNode.get(i).path("securityGroupName").asText().equals("TestForSecGrp")) {
        gid = groupNode.get(i).path("secGroupSysId").asLong();
        break;
      }
    }
    deleteSecurityGroup(gid);
    int userId = userNode.path("user").path("userId").asInt();
    int custId = userNode.path("user").path("customerId").asInt();
    String masterLoginId = userNode.path("user").path("masterLoginId").asText();
    ObjectNode deleteUser = mapper.createObjectNode();
    deleteUser.put("userId", userId);
    deleteUser.put("customerId", custId);
    deleteUser.put("masterLoginId", masterLoginId);
    deleteUser(deleteUser);
  }

  /**
   * Adds security group.
   *
   * @param secGroup securitygroup
   */
  public void addSecurityGroup(ObjectNode secGroup) {
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(secGroup)
        .when()
        .post("/security/auth/admin/security-groups")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));
  }

  /**
   * delete security group.
   *
   * @param groupId groupId
   */
  public void deleteSecurityGroup(Long groupId) {
    given(authSpec)
        .when()
        .delete("/security/auth/admin/security-groups/" + groupId)
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  public void fetchUsers() {
    given(authSpec)
        .when()
        .get("/security/auth/admin/cust/manage/external/users/fetch")
        .then()
        .assertThat()
        .statusCode(200)
        .body("valid", equalTo(true));
  }

  /**
   * delete user group.
   *
   * @param user user
   */
  public void deleteUser(ObjectNode user) {
    given(authSpec)
        .contentType(ContentType.JSON)
        .body(user)
        .when()
        .post("/security/auth/admin/cust/manage/users/delete")
        .then()
        .assertThat()
        .statusCode(200);
  }
}