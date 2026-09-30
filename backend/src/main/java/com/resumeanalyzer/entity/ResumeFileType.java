package com.resumeanalyzer.entity;

/**
 * The resume file formats supported for upload and parsing, per the
 * project's fixed technology decisions (PDFBox for PDF, Apache POI for
 * DOCX). No other format is accepted.
 */
public enum ResumeFileType {
    PDF,
    DOCX
}
