package com.example.minibank.common;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import java.sql.SQLException;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ApiException.class)
 ResponseEntity<ProblemDetail> business(ApiException e,HttpServletRequest req) { return response(req,e.status(),e.code(),e.getMessage()); }
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e,HttpServletRequest req) {
  var p=ApiProblems.create(req,400,"VALIDATION_ERROR","Dữ liệu đầu vào không hợp lệ.");
  p.setProperty("errors",e.getBindingResult().getFieldErrors().stream().map(f->Map.of("field",f.getField(),"message",f.getDefaultMessage()==null?"Invalid value":f.getDefaultMessage())).toList());
  return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(p);
 }
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class})
 ResponseEntity<ProblemDetail> malformed(Exception e,HttpServletRequest req) { return response(req,400,"INVALID_REQUEST","Kiểm tra JSON, UUID, tham số và header bắt buộc."); }
 @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
   org.springframework.web.servlet.NoHandlerFoundException.class,
   org.springframework.web.HttpRequestMethodNotSupportedException.class,
   org.springframework.web.HttpMediaTypeNotSupportedException.class,
   org.springframework.web.HttpMediaTypeNotAcceptableException.class})
 ResponseEntity<ProblemDetail> protocol(Exception e,HttpServletRequest req) {
  var error=(org.springframework.web.ErrorResponse)e;
  return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
    .body(ApiProblems.create(req,error.getStatusCode().value(),"HTTP_ERROR","Đường dẫn, phương thức hoặc định dạng HTTP không được hỗ trợ."));
 }
 @ExceptionHandler(Exception.class)
 ResponseEntity<ProblemDetail> unexpected(Exception e,HttpServletRequest req) {
  boolean temporary=false;
  for(Throwable t=e;t!=null;t=t.getCause()) {
   if(t instanceof org.springframework.dao.CannotAcquireLockException || t instanceof org.springframework.dao.QueryTimeoutException || t instanceof org.springframework.dao.DataAccessResourceFailureException || t instanceof org.springframework.transaction.CannotCreateTransactionException) temporary=true;
   if(t instanceof SQLException s && (s.getErrorCode()==54 || s.getErrorCode()==60 || s.getErrorCode()==30006 || s.getErrorCode()==1013 || s.getErrorCode()==17002)) temporary=true;
  }
  return temporary ? response(req,503,"TEMPORARILY_UNAVAILABLE","Dịch vụ tạm thời bận. Gửi lại yêu cầu chuyển tiền với cùng Idempotency-Key.")
     : response(req,500,"INTERNAL_ERROR","Không thể hoàn tất yêu cầu.");
 }
 private ResponseEntity<ProblemDetail> response(HttpServletRequest req,int status,String code,String detail) {
  return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblems.create(req,status,code,detail));
 }
}
