package com.terraledger.service;

import com.terraledger.domain.LandRecord;
import com.terraledger.domain.OcrExtraction;
import com.terraledger.domain.RecordStatus;
import com.terraledger.dto.FieldUpdateRequest;
import com.terraledger.dto.RecordResponse;
import com.terraledger.dto.ReviewDecisionRequest;
import com.terraledger.repository.LandRecordRepository;
import com.terraledger.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LandRecordService {

    private static final Logger log = LoggerFactory.getLogger(LandRecordService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    private final LandRecordRepository landRecordRepository;
    private final OcrClientService ocrClientService;
    private final AuditService auditService;
    private final Path uploadDir;

    public LandRecordService(LandRecordRepository landRecordRepository,
                             OcrClientService ocrClientService,
                             AuditService auditService,
                             @Value("${app.upload.dir:./uploads}") String uploadDirPath) {
        this.landRecordRepository = landRecordRepository;
        this.ocrClientService = ocrClientService;
        this.auditService = auditService;
        this.uploadDir = Paths.get(uploadDirPath);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.error("Could not create upload directory {}: {}", uploadDirPath, e.getMessage());
        }
    }

    @Transactional
    public RecordResponse createRecord(MultipartFile file, UserPrincipal currentUser) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required and cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename is required");
        }

        // Extension & MIME check
        String ext = getFileExtension(originalFilename).toLowerCase();
        String contentType = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(ext) && (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid file type. Only PDF, PNG, and JPG/JPEG files are accepted.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            String sha256Hash = computeSha256(fileBytes);

            LandRecord record = new LandRecord();
            record.setUploadedBy(currentUser.getId());
            record.setFileName(originalFilename);
            record.setFileContentType(contentType != null ? contentType : "application/octet-stream");
            record.setFileSize(file.getSize());
            record.setSha256Hash(sha256Hash);
            record.setStatus(RecordStatus.PENDING_REVIEW);

            // Save file to storage
            String storageFileName = record.getId() + "_" + originalFilename;
            Path targetPath = this.uploadDir.resolve(storageFileName);
            Files.write(targetPath, fileBytes);
            record.setStoragePath(targetPath.toAbsolutePath().toString());

            // Run OCR extraction
            var ocrResult = ocrClientService.extract(fileBytes, originalFilename);
            Map<String, String> fields = ocrResult.fields();
            Double confidence = ocrResult.confidence();

            record.setDeedType(fields.getOrDefault("deed_type", ""));
            record.setOwnerName(fields.getOrDefault("owner_name", ""));
            record.setSurveyNo(fields.getOrDefault("survey_no", ""));
            record.setArea(fields.getOrDefault("area", ""));
            record.setBoundaries(fields.getOrDefault("boundaries", ""));

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                record.addExtraction(new OcrExtraction(record, entry.getKey(), entry.getValue(), confidence));
            }

            LandRecord saved = landRecordRepository.save(record);
            log.info("Uploaded record {} by user {}, surveyNo={}", saved.getId(), currentUser.getId(), saved.getSurveyNo());

            auditService.logEvent(
                    currentUser.getEmail(),
                    "UPLOAD",
                    "LandRecord",
                    saved.getId(),
                    "Record uploaded: filename=" + originalFilename + ", sha256=" + sha256Hash
            );

            return RecordResponse.fromEntity(saved);

        } catch (IOException e) {
            log.error("Failed to store file {}: {}", originalFilename, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File storage failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public RecordResponse getRecord(String id, UserPrincipal currentUser) {
        LandRecord record = landRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: " + id));

        if (!currentUser.isOfficerOrAdmin() && !record.getUploadedBy().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You are not authorized to view this record");
        }

        return RecordResponse.fromEntity(record);
    }

    @Transactional(readOnly = true)
    public List<RecordResponse> listRecords(RecordStatus statusFilter, UserPrincipal currentUser) {
        List<LandRecord> records;
        if (currentUser.isOfficerOrAdmin()) {
            records = (statusFilter != null)
                    ? landRecordRepository.findByStatusOrderByCreatedAtDesc(statusFilter)
                    : landRecordRepository.findAllByOrderByCreatedAtDesc();
        } else {
            records = (statusFilter != null)
                    ? landRecordRepository.findByUploadedByAndStatusOrderByCreatedAtDesc(currentUser.getId(), statusFilter)
                    : landRecordRepository.findByUploadedByOrderByCreatedAtDesc(currentUser.getId());
        }

        return records.stream().map(RecordResponse::fromEntity).toList();
    }

    @Transactional
    public RecordResponse updateRecordFields(String id, FieldUpdateRequest request, UserPrincipal currentUser) {
        LandRecord record = landRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: " + id));

        if (!currentUser.isOfficerOrAdmin() && !record.getUploadedBy().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You are not authorized to update this record");
        }

        if (request != null && request.getFields() != null) {
            for (Map.Entry<String, String> entry : request.getFields().entrySet()) {
                String fieldKey = entry.getKey();
                String newVal = entry.getValue();

                // Update entity level fields
                switch (fieldKey) {
                    case "survey_no", "surveyNo" -> record.setSurveyNo(newVal);
                    case "owner_name", "ownerName" -> record.setOwnerName(newVal);
                    case "area" -> record.setArea(newVal);
                    case "boundaries" -> record.setBoundaries(newVal);
                    case "deed_type", "deedType" -> record.setDeedType(newVal);
                }

                // Update or create extraction entry
                var existingExt = record.getExtractions().stream()
                        .filter(e -> e.getFieldName().equalsIgnoreCase(fieldKey))
                        .findFirst();

                if (existingExt.isPresent()) {
                    existingExt.get().setCorrectedValue(newVal);
                } else {
                    OcrExtraction newExt = new OcrExtraction(record, fieldKey, "", 1.0);
                    newExt.setCorrectedValue(newVal);
                    record.addExtraction(newExt);
                }
            }
        }

        LandRecord saved = landRecordRepository.save(record);
        log.info("Updated fields for record {} by user {}", id, currentUser.getId());

        auditService.logEvent(
                currentUser.getEmail(),
                "FIELD_UPDATE",
                "LandRecord",
                saved.getId(),
                "Fields updated: " + (request != null ? request.getFields() : "{}")
        );

        return RecordResponse.fromEntity(saved);
    }

    @Transactional
    public RecordResponse approveRecord(String id, ReviewDecisionRequest request, UserPrincipal currentUser) {
        if (!currentUser.isOfficerOrAdmin()) {
            throw new AccessDeniedException("Only officers and administrators can approve records");
        }

        LandRecord record = landRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: " + id));

        if (record.getStatus() != RecordStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot approve record in status " + record.getStatus() + ". Only PENDING_REVIEW records can be approved.");
        }

        record.setStatus(RecordStatus.APPROVED);
        LandRecord saved = landRecordRepository.save(record);

        String remarks = (request != null && request.getRemarks() != null) ? request.getRemarks() : "Approved by officer";
        auditService.logEvent(
                currentUser.getEmail(),
                "APPROVE",
                "LandRecord",
                saved.getId(),
                remarks
        );

        log.info("Record {} approved by officer {}", id, currentUser.getEmail());
        return RecordResponse.fromEntity(saved);
    }

    @Transactional
    public RecordResponse rejectRecord(String id, ReviewDecisionRequest request, UserPrincipal currentUser) {
        if (!currentUser.isOfficerOrAdmin()) {
            throw new AccessDeniedException("Only officers and administrators can reject records");
        }

        LandRecord record = landRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: " + id));

        if (record.getStatus() != RecordStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot reject record in status " + record.getStatus() + ". Only PENDING_REVIEW records can be rejected.");
        }

        record.setStatus(RecordStatus.REJECTED);
        LandRecord saved = landRecordRepository.save(record);

        String remarks = (request != null && request.getRemarks() != null) ? request.getRemarks() : "Rejected by officer";
        auditService.logEvent(
                currentUser.getEmail(),
                "REJECT",
                "LandRecord",
                saved.getId(),
                remarks
        );

        log.info("Record {} rejected by officer {}", id, currentUser.getEmail());
        return RecordResponse.fromEntity(saved);
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0 && dot < filename.length() - 1) ? filename.substring(dot + 1) : "";
    }
}
