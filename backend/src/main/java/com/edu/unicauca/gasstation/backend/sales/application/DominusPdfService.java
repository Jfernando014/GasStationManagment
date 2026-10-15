package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException.Reason;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.DominusCashPageRenderer;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

/** Stateless document operation. Has no dependency on journals, cash services or persistence. */
@Service
public class DominusPdfService {
    public static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    private final DominusCashPageRenderer renderer;
    private final Validator validator;

    public DominusPdfService(DominusCashPageRenderer renderer, Validator validator) {
        this.renderer = renderer;
        this.validator = validator;
    }

    public byte[] appendCashComplement(byte[] original, DominusCashComplement complement) {
        if (original == null || original.length == 0) {
            throw new DominusPdfException(Reason.EMPTY, "Original PDF must not be empty");
        }
        if (original.length > MAX_FILE_SIZE_BYTES) {
            throw new DominusPdfException(Reason.TOO_LARGE, "Original PDF exceeds the 25 MB limit");
        }
        if (complement == null || !validator.validate(complement).isEmpty()) {
            throw new DominusPdfException(Reason.INVALID_DATA, "Complement contains missing or invalid fields");
        }
        if (original.length < 5 || !new String(original, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new DominusPdfException(Reason.INVALID_PDF, "Original file is not a valid PDF");
        }

        try (RandomAccessReadBuffer input = new RandomAccessReadBuffer(original);
                PDDocument document = load(input)) {
            if (document.getNumberOfPages() == 0 || document.isEncrypted()) {
                throw new DominusPdfException(Reason.INVALID_PDF, "Original PDF must contain pages and must not be encrypted");
            }
            // Existing page streams, resources and order are retained; only one new page is appended.
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                renderer.appendPage(document, complement);
                document.save(output);
                return output.toByteArray();
            } catch (IOException ex) {
                throw new DominusPdfException(Reason.GENERATION_FAILED, "Could not generate Dominus PDF", ex);
            }
        } catch (IOException ex) {
            throw new DominusPdfException(Reason.INVALID_PDF, "Original PDF could not be read", ex);
        }
    }

    private static PDDocument load(RandomAccessReadBuffer input) throws IOException {
        PDFParser parser = new PDFParser(input);
        return parser.parse(false);
    }
}
