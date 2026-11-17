package com.terraledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "land_records")
public class LandRecord {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "uploaded_by", length = 36, nullable = false)
    private String uploadedBy;

    @Column(name = "survey_no", length = 100)
    private String surveyNo;

    @Column(name = "owner_name", length = 255)
    private String ownerName;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "boundaries", columnDefinition = "TEXT")
    private String boundaries;

    @Column(name = "deed_type", length = 100)
    private String deedType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RecordStatus status = RecordStatus.PENDING_REVIEW;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_content_type", length = 100, nullable = false)
    private String fileContentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256_hash", length = 64, nullable = false)
    private String sha256Hash;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "ipfs_cid", length = 100)
    private String ipfsCid;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OcrExtraction> extractions = new ArrayList<>();

    public LandRecord() {
        this.id = UUID.randomUUID().toString();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
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

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getIpfsCid() { return ipfsCid; }
    public void setIpfsCid(String ipfsCid) { this.ipfsCid = ipfsCid; }

    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<OcrExtraction> getExtractions() { return extractions; }
    public void setExtractions(List<OcrExtraction> extractions) { this.extractions = extractions; }

    public void addExtraction(OcrExtraction extraction) {
        extractions.add(extraction);
        extraction.setRecord(this);
    }
}
