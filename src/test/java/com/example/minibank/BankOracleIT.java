package com.example.minibank;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.oracle.OracleContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class BankOracleIT {
 static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-free:23.9-slim-faststart")
     .withUsername("bank_owner").withPassword("TestOwnerPass_123");
 static {
  ORACLE.start();
  try {
   var result=ORACLE.execInContainer("bash","-c","printf '%s\n' 'WHENEVER SQLERROR EXIT SQL.SQLCODE' 'ALTER SESSION SET CONTAINER=FREEPDB1;' 'CREATE USER bank_app IDENTIFIED BY RuntimeTestPass_123;' 'GRANT CREATE SESSION TO bank_app;' 'EXIT;' | sqlplus -s / as sysdba");
   if(result.getExitCode()!=0) throw new IllegalStateException("Cannot create runtime test user: "+result.getStdout());
  } catch(Exception e) { throw new IllegalStateException(e); }
 }
 @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
  r.add("spring.datasource.url", ORACLE::getJdbcUrl);
  r.add("spring.datasource.username", () -> "bank_app");
  r.add("spring.datasource.password", () -> "RuntimeTestPass_123");
  r.add("spring.flyway.user", ORACLE::getUsername);
  r.add("spring.flyway.password", ORACLE::getPassword);
  r.add("spring.jpa.properties.hibernate.default_schema", () -> "BANK_OWNER");
  r.add("spring.flyway.default-schema", () -> "BANK_OWNER");
 }
 @Autowired MockMvc mvc;
 JdbcTemplate sql = new JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(ORACLE.getJdbcUrl(),ORACLE.getUsername(),ORACLE.getPassword()));
 @Autowired ObjectMapper json;
 @Autowired DataSource dataSource;
 @Autowired com.example.minibank.demo.DemoSeeder demoSeeder;
 org.springframework.security.crypto.password.PasswordEncoder passwords = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
 UUID a,b; long an,binh;
 @BeforeEach void seed() {
  for(String t: List.of("AUDIT_EVENT","IDEMPOTENCY_REQUEST","LEDGER_ENTRY","BANK_TRANSFER","ACCOUNT_OPENING","BANK_ACCOUNT","APP_USER","CUSTOMER")) sql.update("DELETE FROM " + t);
  sql.update("INSERT INTO customer VALUES (1,'C1','An','ACTIVE')");
  sql.update("INSERT INTO customer VALUES (2,'C2','Binh','ACTIVE')");
  String hash=passwords.encode("TestPass_123");
  sql.update("INSERT INTO app_user VALUES (1,'an',?,'CUSTOMER',1,1)",hash);
  sql.update("INSERT INTO app_user VALUES (2,'binh',?,'CUSTOMER',1,2)",hash);
  sql.update("INSERT INTO app_user VALUES (3,'staff',?,'STAFF',1,NULL)",hash);
  an=1; binh=2; a=UUID.fromString("10000000-0000-4000-8000-000000000001"); b=UUID.fromString("f0000000-0000-4000-8000-000000000002");
  account(a,1,"A","1000000"); account(b,2,"B","200000");
 }
 void account(UUID id,long owner,String number,String balance) {
  sql.update("INSERT INTO bank_account VALUES (HEXTORAW(?),?,?,'VND',?,?,'ACTIVE')",hex(id),number,owner,new java.math.BigDecimal(balance),new java.math.BigDecimal(balance));
  sql.update("INSERT INTO account_opening VALUES (HEXTORAW(?),?,SYSTIMESTAMP)",hex(id),new java.math.BigDecimal(balance));
 }
 static String hex(UUID id) { return id.toString().replace("-",""); }
 String body(UUID from,UUID to,String amount) throws Exception {
  return json.writeValueAsString(Map.of("sourceAccountId",from,"destinationAccountId",to,"amount",amount,"description","books"));
 }
 org.springframework.mock.web.MockHttpServletResponse send(String user,String key,UUID from,UUID to,String amount) throws Exception {
  return mvc.perform(post("/api/v1/transfers").with(httpBasic(user,"TestPass_123"))
      .header("Idempotency-Key",key).contentType("application/json").content(body(from,to,amount))).andReturn().getResponse();
 }
 String balance(UUID id) { return sql.queryForObject("SELECT balance FROM bank_account WHERE id=HEXTORAW(?)",java.math.BigDecimal.class,hex(id)).toPlainString(); }
 long count(String table) { return sql.queryForObject("SELECT count(*) FROM " + table,Long.class); }
 void reconcile() {
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM bank_account a WHERE a.balance <> a.opening_balance + COALESCE((SELECT SUM(CASE WHEN e.direction='CREDIT' THEN e.amount ELSE -e.amount END) FROM ledger_entry e WHERE e.account_id=a.id),0)",Long.class)).isZero();
 }
 @Test void transfersAndReplaysWithoutSpendingTwice() throws Exception {
  var first=send("an","key1",a,b,"300000"); assertThat(first.getStatus()).isEqualTo(201);
  assertThat(balance(a)).isEqualTo("700000"); assertThat(balance(b)).isEqualTo("500000");
  assertThat(count("bank_transfer")).isEqualTo(1); assertThat(count("ledger_entry")).isEqualTo(2);
  assertThat(count("idempotency_request")).isEqualTo(1); assertThat(count("audit_event")).isEqualTo(1);
  assertThat(send("an","key1",a,b,"300001").getStatus()).isEqualTo(409);
  sql.update("UPDATE bank_account SET status='BLOCKED' WHERE id=HEXTORAW(?)",hex(a));
  var replay=send("an","key1",a,b,"300000"); assertThat(replay.getStatus()).isEqualTo(200);
  assertThat(json.readTree(replay.getContentAsString()).get("id")).isEqualTo(json.readTree(first.getContentAsString()).get("id"));
  assertThat(count("bank_transfer")).isEqualTo(1); reconcile();
 }
 @Test void enforcesOwnershipStatusAndMoneyLimits() throws Exception {
  assertThat(send("binh","k1",a,b,"1").getStatus()).isEqualTo(404);
  assertThat(send("staff","k2",a,b,"1").getStatus()).isEqualTo(403);
  assertThat(send("an","k3",a,b,"1000001").getStatus()).isEqualTo(422);
  assertThat(send("an","k4",a,a,"1").getStatus()).isEqualTo(400);
  sql.update("UPDATE bank_account SET balance=9999999999999999999 WHERE id=HEXTORAW(?)",hex(b));
  assertThat(send("an","k5",a,b,"1").getStatus()).isEqualTo(422);
  assertThat(balance(a)).isEqualTo("1000000"); assertThat(count("bank_transfer")).isZero();
 }
 @Test void invalidPayloadsAre400AndDoNotLeakDetails() throws Exception {
  for(String amount:List.of("0","-1","1.0","1e3","10000000000000000000")) assertThat(send("an","valid",a,b,amount).getStatus()).isEqualTo(400);
  String numberBody=body(a,b,"1").replace("\"amount\":\"1\"","\"amount\":1");
  assertThat(mvc.perform(post("/api/v1/transfers").with(httpBasic("an","TestPass_123")).header("Idempotency-Key","key").contentType("application/json").content(numberBody)).andReturn().getResponse().getStatus()).isEqualTo(400);
  assertThat(send("an","bad key",a,b,"1").getStatus()).isEqualTo(400);
  var bad=mvc.perform(get("/api/v1/accounts/not-a-uuid").with(httpBasic("an","TestPass_123"))).andReturn().getResponse();
  assertThat(bad.getStatus()).isEqualTo(400); assertThat(bad.getContentAsString()).contains("requestId").doesNotContain("SQLException","stackTrace");
 }
 @Test void securityAndAccountLifecycle() throws Exception {
  var unauth=mvc.perform(get("/api/v1/accounts/"+a)).andReturn().getResponse();
  assertThat(unauth.getStatus()).isEqualTo(401); assertThat(unauth.getContentAsString()).contains("requestId");
  assertThat(mvc.perform(get("/api/v1/accounts/"+a).with(httpBasic("binh","TestPass_123"))).andReturn().getResponse().getStatus()).isEqualTo(404);
  var view=mvc.perform(get("/api/v1/accounts/"+a).with(httpBasic("an","TestPass_123")).header("X-Request-ID","bad id")).andReturn().getResponse();
  assertThat(view.getStatus()).isEqualTo(200); assertThat(json.readTree(view.getContentAsString()).get("id").asText()).isEqualTo(a.toString());
  assertThat(json.readTree(view.getContentAsString()).get("balance").isTextual()).isTrue();
  assertThat(view.getHeader("X-Request-ID")).matches("[A-Za-z0-9_-]{1,64}");
  for(String q:List.of("?size=101","?page=-1","?size=0")) assertThat(mvc.perform(get("/api/v1/accounts/"+a+"/entries"+q).with(httpBasic("an","TestPass_123"))).andReturn().getResponse().getStatus()).isEqualTo(400);
  assertThat(mvc.perform(patch("/api/v1/accounts/"+a+"/status").with(httpBasic("an","TestPass_123")).contentType("application/json").content("{\"status\":\"BLOCKED\"}")).andReturn().getResponse().getStatus()).isEqualTo(403);
  assertThat(mvc.perform(patch("/api/v1/accounts/"+a+"/status").with(httpBasic("staff","TestPass_123")).contentType("application/json").content("{\"status\":\"BLOCKED\"}")).andReturn().getResponse().getStatus()).isEqualTo(200);
  assertThat(send("an","blocked",a,b,"1").getStatus()).isEqualTo(422);
  sql.update("UPDATE app_user SET enabled=0 WHERE id=1");
  assertThat(mvc.perform(get("/api/v1/accounts/"+a).with(httpBasic("an","TestPass_123"))).andReturn().getResponse().getStatus()).isEqualTo(401);
 }
 @Test void staffCreatesCustomerAndZeroOpeningAccount() throws Exception {
  String request="{\"name\":\"Chi\"}";
  assertThat(mvc.perform(post("/api/v1/customers").with(httpBasic("an","TestPass_123")).contentType("application/json").content(request)).andReturn().getResponse().getStatus()).isEqualTo(403);
  var c=mvc.perform(post("/api/v1/customers").with(httpBasic("staff","TestPass_123")).contentType("application/json").content(request)).andReturn().getResponse();
  assertThat(c.getStatus()).isEqualTo(201); long id=json.readTree(c.getContentAsString()).get("id").asLong();
  var acc=mvc.perform(post("/api/v1/accounts").with(httpBasic("staff","TestPass_123")).contentType("application/json").content("{\"customerId\":"+id+"}")).andReturn().getResponse();
  assertThat(acc.getStatus()).isEqualTo(201); JsonNode node=json.readTree(acc.getContentAsString()); UUID uid=UUID.fromString(node.get("id").asText());
  assertThat(uid.version()).isEqualTo(4); assertThat(node.get("balance").asText()).isEqualTo("0");
  assertThat(sql.queryForObject("SELECT amount FROM account_opening WHERE account_id=HEXTORAW(?)",java.math.BigDecimal.class,hex(uid))).isEqualByComparingTo("0");
  sql.update("UPDATE customer SET status='INACTIVE' WHERE id=?",id);
  assertThat(mvc.perform(post("/api/v1/accounts").with(httpBasic("staff","TestPass_123")).contentType("application/json").content("{\"customerId\":"+id+"}")).andReturn().getResponse().getStatus()).isEqualTo(422);
 }
 @Test void databaseConstraintsProtectData() {
  assertThatThrownBy(()->sql.update("UPDATE bank_account SET balance=-1 WHERE id=HEXTORAW(?)",hex(a))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  assertThatThrownBy(()->account(a,1,"OTHER","0")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  assertThatThrownBy(()->account(UUID.randomUUID(),999,"ORPHAN","0")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
 }
 @Test void failureAfterBalanceUpdatesRollsBackEverything() throws Exception {
  sql.execute("CREATE OR REPLACE TRIGGER test_fail_ledger BEFORE INSERT ON ledger_entry BEGIN raise_application_error(-20001,'TEST FAILURE'); END;");
  try { assertThat(send("an","rollback",a,b,"300000").getStatus()).isEqualTo(500); }
  finally { sql.execute("DROP TRIGGER test_fail_ledger"); }
  assertThat(balance(a)).isEqualTo("1000000"); assertThat(balance(b)).isEqualTo("200000");
  for(String t:List.of("bank_transfer","ledger_entry","idempotency_request","audit_event")) assertThat(count(t)).isZero(); reconcile();
 }
 List<Integer> concurrent(Callable<Integer> x,Callable<Integer> y) throws Exception {
  try(var pool=Executors.newFixedThreadPool(2)) {
   var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
   Callable<Integer> first=()->{ready.countDown();start.await();return x.call();};
   Callable<Integer> second=()->{ready.countDown();start.await();return y.call();};
   Future<Integer> fx=pool.submit(first), fy=pool.submit(second);
   assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); start.countDown();
   return List.of(fx.get(20,TimeUnit.SECONDS),fy.get(20,TimeUnit.SECONDS));
  }
 }
 @Test void simultaneousDuplicateCommitsOnce() throws Exception {
  assertThat(concurrent(()->send("an","same",a,b,"300000").getStatus(),()->send("an","same",a,b,"300000").getStatus())).containsExactlyInAnyOrder(201,200);
  assertThat(count("bank_transfer")).isEqualTo(1); assertThat(balance(a)).isEqualTo("700000"); reconcile();
 }
 @Test void simultaneousSpendingNeverOverdraws() throws Exception {
  assertThat(concurrent(()->send("an","x",a,b,"700000").getStatus(),()->send("an","y",a,b,"600000").getStatus())).containsExactlyInAnyOrder(201,422);
  assertThat(count("bank_transfer")).isEqualTo(1); assertThat(new java.math.BigDecimal(balance(a))).isGreaterThanOrEqualTo(java.math.BigDecimal.ZERO); reconcile();
 }
 @Test void reverseTransfersDoNotDeadlock() throws Exception {
  assertThat(concurrent(()->send("an","x",a,b,"100000").getStatus(),()->send("binh","y",b,a,"50000").getStatus())).containsOnly(201);
  assertThat(balance(a)).isEqualTo("950000"); assertThat(balance(b)).isEqualTo("250000"); reconcile();
 }
 @Test void accountAndUserLocksHaveFiniteWait() throws Exception {
  for(String query:List.of("SELECT id FROM bank_account WHERE id=HEXTORAW('"+hex(a)+"') FOR UPDATE", "SELECT id FROM app_user WHERE id=1 FOR UPDATE")) {
   try(Connection holder=dataSource.getConnection()) {
    holder.setAutoCommit(false); try(var stmt=holder.createStatement()) { stmt.executeQuery(query).close(); }
    long start=System.nanoTime(); var response=send("an",UUID.randomUUID().toString(),a,b,"1");
    double seconds=(System.nanoTime()-start)/1e9;
    assertThat(response.getStatus()).isEqualTo(503); assertThat(seconds).isBetween(3.0,12.0); holder.rollback();
   }
  }
  assertThat(count("bank_transfer")).isZero();
 }
 @Test void ledgerHistoryAndTransferVisibility() throws Exception {
  var created=send("an","hist",a,b,"1");long id=json.readTree(created.getContentAsString()).get("id").asLong();
  assertThat(mvc.perform(get("/api/v1/transfers/"+id).with(httpBasic("binh","TestPass_123"))).andReturn().getResponse().getStatus()).isEqualTo(200);
  var entries=mvc.perform(get("/api/v1/accounts/"+a+"/entries?size=1").with(httpBasic("an","TestPass_123"))).andReturn().getResponse();
  assertThat(entries.getStatus()).isEqualTo(200); assertThat(json.readTree(entries.getContentAsString()).get("content").get(0).get("direction").asText()).isEqualTo("DEBIT");
 }
 @Test void demoSeedIsAtomicAndIdempotent() {
  for(String t:List.of("AUDIT_EVENT","IDEMPOTENCY_REQUEST","LEDGER_ENTRY","BANK_TRANSFER","ACCOUNT_OPENING","BANK_ACCOUNT","APP_USER","CUSTOMER")) sql.update("DELETE FROM " + t);
  assertThatThrownBy(()->demoSeeder.seed("", "LearningPass_123", "LearningPass_123")).isInstanceOf(IllegalArgumentException.class);
  assertThat(count("app_user")).isZero();
  var first=demoSeeder.seed("LearningPass_123","LearningPass_123","LearningPass_123");
  var second=demoSeeder.seed("LearningPass_123","LearningPass_123","LearningPass_123");
  assertThat(second).isEqualTo(first);assertThat(count("app_user")).isEqualTo(3);
  assertThat(count("bank_account")).isEqualTo(2);assertThat(count("account_opening")).isEqualTo(2);
  assertThat(balance(first.anAccountId())).isEqualTo("1000000");
  assertThat(balance(first.binhAccountId())).isEqualTo("200000");reconcile();
 }

 @Test void concurrentIncomingTransfersPreserveBothCredits() throws Exception {
  UUID destination=UUID.fromString("a0000000-0000-4000-8000-000000000003");
  sql.update("INSERT INTO customer VALUES (3,'C3','Chi','ACTIVE')");account(destination,3,"C","0");
  assertThat(concurrent(()->send("an","incoming-an",a,destination,"100000").getStatus(),()->send("binh","incoming-binh",b,destination,"100000").getStatus())).containsOnly(201);
  assertThat(balance(destination)).isEqualTo("200000");assertThat(count("ledger_entry")).isEqualTo(4);reconcile();
 }
 @Test void statusChangesSerializeWithTransfers() throws Exception {
  var statuses=concurrent(()->send("an","status-race",a,b,"100000").getStatus(),()->mvc.perform(patch("/api/v1/accounts/"+a+"/status").with(httpBasic("staff","TestPass_123")).contentType("application/json").content("{\"status\":\"BLOCKED\"}")).andReturn().getResponse().getStatus());
  assertThat(statuses.get(1)).isEqualTo(200);assertThat(statuses.get(0)).isIn(201,422);
  assertThat(sql.queryForObject("SELECT status FROM bank_account WHERE id=HEXTORAW(?)",String.class,hex(a))).isEqualTo("BLOCKED");
  assertThat(balance(a)).isIn("1000000","900000");reconcile();
 }
 @Test void runtimeHasNoDdlOrLedgerMutationPrivileges() throws Exception {
  try(Connection runtime=dataSource.getConnection();var statement=runtime.createStatement()) {
   assertThatThrownBy(()->statement.execute("CREATE TABLE forbidden_table (id NUMBER)")).isInstanceOf(java.sql.SQLException.class);
   assertThatThrownBy(()->statement.execute("DELETE FROM BANK_OWNER.ledger_entry")).isInstanceOf(java.sql.SQLException.class);
  }
 }

 @Test void httpProtocolErrorsStayClientErrors() throws Exception {
  assertThat(mvc.perform(get("/api/v1/unknown").with(httpBasic("an","TestPass_123"))).andReturn().getResponse().getStatus()).isEqualTo(404);
  var wrongMethod=mvc.perform(put("/api/v1/accounts/"+a).with(httpBasic("an","TestPass_123"))).andReturn().getResponse();
  assertThat(wrongMethod.getStatus()).isEqualTo(405);assertThat(wrongMethod.getHeader("Allow")).contains("GET");
  assertThat(mvc.perform(post("/api/v1/transfers").with(httpBasic("an","TestPass_123")).header("Idempotency-Key","format").contentType("text/plain").content("hello")).andReturn().getResponse().getStatus()).isEqualTo(415);
 }
 @Test void excessiveOffsetIsRejected() throws Exception {
  assertThat(mvc.perform(get("/api/v1/accounts/"+a+"/entries?page=2147483647&size=100").with(httpBasic("an","TestPass_123"))).andReturn().getResponse().getStatus()).isEqualTo(400);
 }
 @Test void replayReturnsExactlySameResponse() throws Exception {
  String payload=json.writeValueAsString(Map.of("sourceAccountId",a,"destinationAccountId",b,"amount","100"));
  var first=mvc.perform(post("/api/v1/transfers").with(httpBasic("an","TestPass_123")).header("Idempotency-Key","exact").contentType("application/json").content(payload)).andReturn().getResponse();
  var replay=mvc.perform(post("/api/v1/transfers").with(httpBasic("an","TestPass_123")).header("Idempotency-Key","exact").contentType("application/json").content(payload)).andReturn().getResponse();
  assertThat(first.getStatus()).isEqualTo(201);assertThat(replay.getStatus()).isEqualTo(200);
  assertThat(json.readTree(replay.getContentAsString())).isEqualTo(json.readTree(first.getContentAsString()));
  long id=json.readTree(first.getContentAsString()).get("id").asLong();
  var get=mvc.perform(get("/api/v1/transfers/"+id).with(httpBasic("an","TestPass_123"))).andReturn().getResponse();
  assertThat(json.readTree(get.getContentAsString())).isEqualTo(json.readTree(first.getContentAsString()));
 }
 @Test void fractionalCustomerIdCannotOpenAnotherCustomersAccount() throws Exception {
  long before=count("bank_account"), openings=count("account_opening"),events=count("audit_event");
  assertThat(mvc.perform(post("/api/v1/accounts").with(httpBasic("staff","TestPass_123")).contentType("application/json").content("{\"customerId\":1.9}")).andReturn().getResponse().getStatus()).isEqualTo(400);
  assertThat(count("bank_account")).isEqualTo(before);assertThat(count("account_opening")).isEqualTo(openings);assertThat(count("audit_event")).isEqualTo(events);
 }

}
