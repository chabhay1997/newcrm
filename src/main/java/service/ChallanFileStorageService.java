package service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ChallanFileStorageService {
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private final Path storageRoot = Path.of("uploads", "challans").toAbsolutePath().normalize();

    public List<String> store(List<MultipartFile> uploads) {
        List<MultipartFile> files = uploads == null ? List.of() : uploads.stream()
                .filter(file -> file != null && !file.isEmpty()).toList();
        if (files.size() > MAX_FILES) throw new IllegalArgumentException("A maximum of 5 files can be uploaded.");
        if (files.isEmpty()) return List.of();
        List<String> storedNames = new ArrayList<>();
        try {
            Files.createDirectories(storageRoot);
            for (MultipartFile file : files) {
                String extension = extension(file.getOriginalFilename());
                validate(file, extension);
                String storedName = UUID.randomUUID() + extension;
                Path target = storageRoot.resolve(storedName).normalize();
                if (!target.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid upload filename.");
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                storedNames.add(storedName);
            }
            return storedNames;
        } catch (IOException | RuntimeException exception) {
            delete(storedNames);
            if (exception instanceof IllegalArgumentException invalid) throw invalid;
            throw new IllegalStateException("Unable to store uploaded files.", exception);
        }
    }

    public void delete(List<String> storedNames) {
        for (String storedName : storedNames) {
            try { Files.deleteIfExists(storageRoot.resolve(storedName).normalize()); }
            catch (IOException ignored) { }
        }
    }

    private void validate(MultipartFile file, String extension) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Each upload must be 5 MB or smaller.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PDF, JPG, JPEG, and PNG files are allowed.");
        }
        byte[] header;
        try (var input = file.getInputStream()) { header = input.readNBytes(8); }
        boolean validSignature = switch (extension) {
            case ".pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D});
            case ".png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case ".jpg", ".jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            default -> false;
        };
        if (!validSignature) throw new IllegalArgumentException("An uploaded file does not match its declared format.");
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (value[index] != prefix[index]) return false;
        return true;
    }

    private String extension(String filename) {
        if (filename == null) return "";
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return ".pdf";
        if (lower.endsWith(".png")) return ".png";
        if (lower.endsWith(".jpg")) return ".jpg";
        if (lower.endsWith(".jpeg")) return ".jpeg";
        throw new IllegalArgumentException("Uploaded file extension is not allowed.");
    }
}
