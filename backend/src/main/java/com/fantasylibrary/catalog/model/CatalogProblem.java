package com.fantasylibrary.catalog.model;

public class CatalogProblem extends RuntimeException {

    public enum Reason { WORK_NOT_FOUND, DOWNLOAD_NOT_FOUND, PLAYBACK_NOT_FOUND, DATABASE_UNAVAILABLE }

    private final Reason reason;

    public CatalogProblem(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
