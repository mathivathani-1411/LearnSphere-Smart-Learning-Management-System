package com.lms.service;

import com.lms.util.ValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/**
 * Handles secure file uploads (assignment submissions and lesson materials).
 *
 * Files are saved to  <uploadDir>/submissions/<safeFilename>
 * The upload root defaults to  ${catalina.home}/uploads  or the value of
 * the env var  UPLOAD_DIR.
 */
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long   MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    private final Path uploadRoot;

    public FileStorageService() {
        String dir = System.getenv("UPLOAD_DIR");
        if (dir == null) {
            dir = System.getProperty("catalina.home", System.getProperty("user.home")) + "/lms-uploads";
        }
        this.uploadRoot = Paths.get(dir);
    }

    /**
     * Save an uploaded Part to disk and return the relative file path.
     *
     * @param part         the multipart upload
     * @param subFolder    sub-directory to place the file in (e.g. "submissions")
     * @return relative path string (stored in the database)
     */
    public String save(Part part, String subFolder) throws IOException {
        String originalName = getSubmittedFileName(part);

        // Security validations
        if (!ValidationUtil.isAllowedExtension(originalName)) {
            throw new IllegalArgumentException("File type not allowed. Use PDF, DOCX, DOC, ZIP, or TXT.");
        }
        if (part.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File size must not exceed 10 MB.");
        }
        if (part.getSize() == 0) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        // Generate a safe, unique filename to prevent path traversal
        String ext = originalName.substring(originalName.lastIndexOf('.'));
        String safeFilename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path targetDir = uploadRoot.resolve(Paths.get(subFolder)).normalize();
        if (!targetDir.startsWith(uploadRoot)) {
            throw new SecurityException("Invalid upload sub-folder.");
        }
        Files.createDirectories(targetDir);

        Path target = targetDir.resolve(safeFilename);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String relPath = subFolder + "/" + safeFilename;
        log.info("Saved upload: {} -> {}", originalName, target);
        return relPath;
    }

    /**
     * Resolve a stored relative path to an absolute Path.
     * Validates that the resolved path is still inside the upload root.
     */
    public Path resolve(String relPath) {
        Path resolved = uploadRoot.resolve(Paths.get(relPath)).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new SecurityException("Illegal file path.");
        }
        return resolved;
    }

    private String getSubmittedFileName(Part part) {
        String header = part.getHeader("content-disposition");
        if (header == null) return "upload.bin";
        for (String token : header.split(";")) {
            token = token.trim();
            if (token.startsWith("filename")) {
                String name = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                // Strip directory components
                return Paths.get(name).getFileName().toString();
            }
        }
        return "upload.bin";
    }
}
