package com.terraledger.dto;

import com.terraledger.domain.LandRecord;
import com.terraledger.domain.RecordStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RecordResponse {
    private String id;
    private String uploadedBy;
    private String surveyNo;
    private String ownerName;
    private String area;
    private String boundaries;
    private String deedType;
    private RecordStatus status;
    private String fileName;
    private String fileContentType;
    private Long fileSize;
    private String sha256Hash;
    private String ipfsCid;
    private Integer currentVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, FieldDetail> fields = new HashMap<>();

    public RecordResponse() {}

    public static RecordResponse fromEntity(LandRecord record) {
        RecordResponse resp = new RecordResponse();
        resp.setId(record.getId());
        resp.setUploadedBy(record.getUploadedBy());
        resp.setSurveyNo(record.getSurveyNo());
        resp.setOwnerName(record.getOwnerName());
        resp.setArea(record.getArea());
        resp.setBoundaries(record.getBoundaries());
        resp.setDeedType(record.getDeedType());
        resp.setStatus(record.getStatus());
        resp.setFileName(record.getFileName());
        resp.setFileContentType(record.getFileContentType());
        resp.setFileSize(record.getFileSize());
        resp.setSha256Hash(record.getSha256Hash());
        resp.setIpfsCid(record.getIpfsCid());
        resp.setCurrentVersion(record.getCurrentVersion());
        resp.setCreatedAt(record.getCreatedAt());
        resp.setUpdatedAt(record.getUpdatedAt());

        if (record.getExtractions() != null) {
            for (var ext : record.getExtractions()) {
                resp.getFields().put(ext.getFieldName(), new FieldDetail(
                    ext.getFieldName(),
                    ext.getExtractedValue(),
                    ext.getCorrectedValue(),
                    ext.getConfidence()
                ));
            }
        }
        return resp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getSurveyNo() { return surveyNo; }
    public void setSurveyNo(String surveyNo) { this.surveyNo = surveyNo; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getBoundaries() { return boundaries; }
    public void setBoundaries(String boundaries) { this.boundaries = boundaries; }

    public String getDeedType() { return deedType; }
    public void setDeedType(String deedType) { this.deedType = deedType; }

    public RecordStatus getStatus() { return status; }
    public void setStatus(RecordStatus status) { this.status = status; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileContentType() { return fileContentType; }
    public void setFileContentType(String fileContentType) { this.fileContentType = fileContentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public String getIpfsCid() { return ipfsCid; }
    public void setIpfsCid(String ipfsCid) { this.ipfsCid = ipfsCid; }

    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, FieldDetail> getFields() { return fields; }
    public void setFields(Map<String, FieldDetail> fields) { this.fields = fields; }
}
