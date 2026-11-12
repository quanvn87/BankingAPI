package com.example.minibank.common;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
public class RequestIdFilter extends OncePerRequestFilter {
 public static final String ATTRIBUTE="requestId";
 private static final Logger log=LoggerFactory.getLogger(RequestIdFilter.class);
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String id=request.getHeader("X-Request-ID");
  if(id==null || !id.matches("[A-Za-z0-9_-]{1,64}")) id=UUID.randomUUID().toString();
  request.setAttribute(ATTRIBUTE,id);response.setHeader("X-Request-ID",id);MDC.put(ATTRIBUTE,id);
  long start=System.nanoTime();
  try { chain.doFilter(request,response); }
  finally {
   // Do not log URL, body or headers: they may contain personal data/credentials.
   log.info("requestId={} method={} status={} durationMs={}",id,request.getMethod(),response.getStatus(),(System.nanoTime()-start)/1000000);
   MDC.remove(ATTRIBUTE);
  }
 }
}
