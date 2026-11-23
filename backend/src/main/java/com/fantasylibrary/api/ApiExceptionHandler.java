package com.fantasylibrary.api;

import com.fantasylibrary.account.AccountProblem;
import com.fantasylibrary.api.ApiResponse.Code;
import com.fantasylibrary.api.ApiResponse.FieldIssue;
import com.fantasylibrary.catalog.model.CatalogProblem;
import com.fantasylibrary.community.CommunityProblem;
import com.fantasylibrary.media.MediaStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MediaStoreException.class)
    public ResponseEntity<ApiResponse<Void>> mediaStoreError(MediaStoreException problem) {
        return problem.isMissing()
                ? error(HttpStatus.NOT_FOUND, Code.MEDIA_NOT_FOUND, problem.getMessage())
                : error(HttpStatus.SERVICE_UNAVAILABLE, Code.MEDIA_STORAGE_UNAVAILABLE, problem.getMessage());
    }

    @ExceptionHandler(CommunityProblem.class)
    public ResponseEntity<ApiResponse<Void>> communityProblem(CommunityProblem problem) {
        return error(problem.status(), problem.code(), problem.getMessage());
    }

    @ExceptionHandler(AccountProblem.class)
    public ResponseEntity<ApiResponse<Void>> accountProblem(AccountProblem problem) {
        return error(problem.status(), problem.code(), problem.getMessage());
    }

    @ExceptionHandler(CatalogProblem.class)
    public ResponseEntity<ApiResponse<Void>> catalogProblem(CatalogProblem problem) {
        return switch (problem.reason()) {
            case WORK_NOT_FOUND -> error(HttpStatus.NOT_FOUND, Code.WORK_NOT_FOUND, "未找到该作品");
            case DOWNLOAD_NOT_FOUND -> error(HttpStatus.NOT_FOUND, Code.DOWNLOAD_NOT_FOUND, "下载文件暂不可用");
            case PLAYBACK_NOT_FOUND -> error(HttpStatus.NOT_FOUND, Code.PLAYBACK_NOT_FOUND, "该播放资源已不可用");
            case DATABASE_UNAVAILABLE -> error(HttpStatus.SERVICE_UNAVAILABLE, Code.DATABASE_UNAVAILABLE,
                    "馆藏数据库尚未配置");
        };
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> databaseError(DataAccessException exception) {
        int oracleCode = oracleErrorCode(exception);
        if (oracleCode == 942) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, Code.CATALOG_NOT_INITIALIZED,
                    "馆藏表尚未初始化，或当前账号无权访问");
        }
        if (exception instanceof DataAccessResourceFailureException ||
                exception instanceof TransientDataAccessException ||
                exception instanceof RecoverableDataAccessException) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, Code.DATABASE_UNAVAILABLE, "馆藏数据库暂时不可用");
        }
        LOG.error("Catalog database query failed (type={}, code={})", exception.getClass().getSimpleName(), oracleCode);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, Code.INTERNAL_ERROR, "服务暂时无法完成请求");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpectedError(Exception exception) {
        LOG.error("HTTP request failed (type={})", exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, Code.INTERNAL_ERROR, "服务暂时无法完成请求");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                              HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            LOG.error("HTTP request failed (type={}, status={})", exception.getClass().getSimpleName(), status.value());
            return super.handleExceptionInternal(exception,
                    ApiResponse.error(Code.INTERNAL_ERROR, "服务暂时无法完成请求", List.of()), headers, status, request);
        }
        ApiResponse<Void> response = switch (status.value()) {
            case 400 -> ApiResponse.error(Code.INVALID_ARGUMENT, "请求参数不正确", fieldIssues(exception));
            case 404 -> ApiResponse.error(Code.ROUTE_NOT_FOUND, "请求地址不存在", List.of());
            case 413 -> ApiResponse.error(Code.INVALID_ARGUMENT, "图片过大。每张最多 5 MB，每帖最多 4 张", List.of());
            case 405 -> ApiResponse.error(Code.METHOD_NOT_ALLOWED, "此地址不支持该请求方法", List.of());
            default -> ApiResponse.error(Code.REQUEST_ERROR, "请求无法处理", List.of());
        };
        return super.handleExceptionInternal(exception, response, headers, status, request);
    }

    private static int oracleErrorCode(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() != 0) {
                return sql.getErrorCode();
            }
        }
        return 0;
    }

    private static List<FieldIssue> fieldIssues(Exception exception) {
        return switch (exception) {
            case MethodArgumentNotValidException binding -> binding.getBindingResult().getFieldErrors().stream()
                    .map(error -> new FieldIssue(error.getField(), error.isBindingFailure()
                            ? "参数类型不正确" : error.getDefaultMessage()))
                    .toList();
            case HandlerMethodValidationException validation -> validation.getParameterValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream().map(error ->
                            new FieldIssue(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                    .toList();
            case MethodArgumentTypeMismatchException mismatch ->
                    List.of(new FieldIssue(mismatch.getName(), "参数类型不正确"));
            default -> List.of();
        };
    }

    private static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, Code code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message, List.of()));
    }
}
