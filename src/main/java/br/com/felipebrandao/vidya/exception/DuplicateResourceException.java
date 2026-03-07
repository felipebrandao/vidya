package br.com.felipebrandao.vidya.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resource, String field, Object value) {
        super(resource + " já cadastrado com " + field + ": " + value);
    }
}

