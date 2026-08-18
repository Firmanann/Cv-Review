package com.mann.cvreview.util.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Auth & User Errors
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email sudah terdaftar"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User tidak ditemukan"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email atau password salah"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Akses tidak diizinkan, silakan login terlebih dahulu"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk mengakses resource ini"),

    // Validation Errors
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validasi data gagal"),
    USERNAME_REQUIRED(HttpStatus.BAD_REQUEST, "Nama wajib diisi"),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "Email wajib diisi"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "Password wajib diisi"),
    EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "Format email tidak valid"),
    PASSWORD_SIZE(HttpStatus.BAD_REQUEST, "Password minimal 8 karakter"),

    // Rate Limit & Analysis Errors
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Terlalu banyak permintaan, silakan coba beberapa saat lagi"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Format input atau file tidak valid"),
    FILE_EXTRACTION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Gagal mengekstrak konten teks dari file"),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "Layanan analisis AI mengalami gangguan, silakan coba lagi"),

    // System Errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan internal pada server");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
