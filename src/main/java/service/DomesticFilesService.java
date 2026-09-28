package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class DomesticFilesService {
    public record FileEntry(int id, String name, boolean available) {}

    private static final List<String> NAMES = List.of(
            "Acceptance of MMF.docx",
            "Acceptance of SIT.doc",
            "APPOINTMENT LETTER.docx",
            "Brand Authorization.docx",
            "Checklist.docx",
            "Form 1.docx",
            "Form 2.docx",
            "List of Machinery.xlsx",
            "List of Raw Materials.docx",
            "LOCATION PLAN.docx",
            "OUTSOURCING MANUFACTURING AGREEMENT.docx",
            "PCTestEquipmentTemplete (8).xlsx",
            "QAP Plan.docx",
            "Relaxation.docx",
            "Revised ISI Checklist .docx",
            "Self evaluation cum verification report.docx",
            "UNDERTAKING.docx"
    );

    private final Path directory;

    public DomesticFilesService(@Value("${operation.domestic-files.directory:domestic-files}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public List<FileEntry> list() {
        return IntStream.range(0, NAMES.size())
                .mapToObj(index -> new FileEntry(index + 1, NAMES.get(index), Files.isRegularFile(path(index + 1))))
                .toList();
    }

    public FileEntry get(int id) {
        Path file = path(id);
        if (!Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document is not available yet");
        return new FileEntry(id, NAMES.get(id - 1), true);
    }

    public Resource resource(int id) {
        get(id);
        return new FileSystemResource(path(id));
    }

    public Resource simplifiedResource() {
        Path file = directory.resolve("simplified.docx");
        if (!Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Simplified document is not available");
        return new FileSystemResource(file);
    }

    private Path path(int id) {
        if (id < 1 || id > NAMES.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return directory.resolve(NAMES.get(id - 1));
    }
}
