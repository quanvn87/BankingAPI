package com.example.minibank.common;
import jakarta.servlet.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ProblemDetail;
import java.io.IOException;
public final class ApiProblems {
 private ApiProblems() {}
 public static ProblemDetail create(HttpServletRequest request,int status,String code,String detail) {
  ProblemDetail p=ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(status),detail);
  p.setProperty("code",code);p.setProperty("requestId",request.getAttribute(RequestIdFilter.ATTRIBUTE));return p;
 }
 public static void write(ObjectMapper mapper,HttpServletRequest request,HttpServletResponse response,int status,String code,String detail) throws IOException {
  response.setStatus(status);response.setContentType("application/problem+json");response.setCharacterEncoding("UTF-8");
  mapper.writeValue(response.getOutputStream(),create(request,status,code,detail));
 }
}
