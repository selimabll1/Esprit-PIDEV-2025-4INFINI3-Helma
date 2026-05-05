package tn.esprit.helma.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String code = ex.getReason();
        String message = resolveMessage(code, ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", code != null ? code : "ERROR", "message", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "CONFLICT", "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_ERROR", "message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Erreur interne du serveur.";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", msg));
    }

    private String resolveMessage(String code, String fallback) {
        if (code == null) return fallback != null ? fallback : "Erreur interne.";
        return switch (code) {
            case "PIN_REQUIRED"  -> "Cette transaction nécessite une vérification par PIN.";
            case "PIN_INCORRECT" -> "PIN incorrect. Vérifiez votre code et réessayez.";
            case "PIN_LOCKED"    -> "Compte verrouillé suite à trop de tentatives. Réessayez dans 15 minutes.";
            case "NOT_FOUND"     -> "Ressource introuvable.";
            default              -> fallback != null ? fallback : code;
        };
    }
}
