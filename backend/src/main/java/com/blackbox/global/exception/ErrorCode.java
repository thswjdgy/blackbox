package com.blackbox.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    INVITE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "유효하지 않은 초대 코드입니다."),
    ALREADY_PROJECT_MEMBER(HttpStatus.CONFLICT, "이미 프로젝트 멤버입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "팀장은 프로젝트를 탈퇴할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트 멤버를 찾을 수 없습니다."),

    // Vault
    VAULT_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    FILE_TAMPERED(HttpStatus.CONFLICT, "파일 해시가 일치하지 않습니다. 변조가 감지되었습니다."),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),

    // Meeting
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "회의를 찾을 수 없습니다."),
    CHECKIN_CODE_INVALID(HttpStatus.BAD_REQUEST, "체크인 코드가 올바르지 않습니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "이미 체크인한 회의입니다."),

    // 외부 연동
    ALREADY_MAPPED(HttpStatus.CONFLICT, "이미 매핑된 사용자입니다."),
    INTEGRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "연동 정보를 찾을 수 없습니다."),

    // General
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "태스크를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
