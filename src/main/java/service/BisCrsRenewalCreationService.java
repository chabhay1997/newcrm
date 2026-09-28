package service;

import dto.BisCrsCreateRequest;
import dto.BisCrsCreateResponse;
import model.BisCrsRenewal;
import model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.BisCrsRenewalRepository;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
public class BisCrsRenewalCreationService {
    private static final int FIELD_LIMIT = 255;
    private static final int ADDRESS_LIMIT = 10_000;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final BisCrsRenewalRepository renewals;

    public BisCrsRenewalCreationService(BisCrsRenewalRepository renewals) {
        this.renewals = renewals;
    }

    @Transactional
    public BisCrsCreateResponse create(User user, BisCrsCreateRequest request) {
        if (request == null) throw new IllegalArgumentException("Request body is required");
        String manufacturer = required(request.manufacturerName(), "Manufacturer Name");
        String product = field(request.productName(), "Product Name");
        String licence = field(request.licenceNumber(), "License No.");
        String standard = field(request.isStandard(), "IS Standard");
        String brand = field(request.brandName(), "Brand Name");
        String airName = field(request.airName(), "AIR Name");
        String address = limited(request.airAddress(), "AIR Address", ADDRESS_LIMIT);
        String email = field(request.airEmailId(), "AIR Email Id");
        String contact = field(request.airContactNumber(), "AIR Contact No.");
        String status = required(request.currentStatus(), "Current Status");
        if (!status.equals("Active") && !status.equals("Differed")
                && !status.equals("Registered") && !status.equals("Expired"))
            throw new IllegalArgumentException("Current Status must be Active, Differed, Registered or Expired");
        LocalDate notifyDate = request.notifyDate();
        if (notifyDate == null) throw new IllegalArgumentException("Notify Date is required");
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) throw new IllegalArgumentException("AIR Email Id is invalid");
        if (request.licenceDate() != null && request.expiryDate() != null && request.expiryDate().isBefore(request.licenceDate()))
            throw new IllegalArgumentException("Expire Date of License cannot be before Date of License");
        if (renewals.existsExactRecord(licence, manufacturer, product, airName, brand, request.licenceDate(), status))
            throw new DuplicateBisCrsRenewalException("This BIS CRS record already exists");

        BisCrsRenewal record = new BisCrsRenewal();
        record.setCreatedBy(user.getId());
        record.setManufacturerName(manufacturer);
        record.setProductName(product);
        record.setLicenceNumber(licence);
        record.setIsStandard(standard);
        record.setLicenceDate(request.licenceDate());
        record.setExpiryDate(request.expiryDate());
        record.setNotifyDate(notifyDate);
        record.setBrandName(brand);
        record.setAirName(airName);
        record.setAirAddress(address);
        record.setAirEmailId(email);
        record.setAirContactNumber(contact);
        record.setCurrentStatus(status);
        BisCrsRenewal saved = renewals.saveAndFlush(record);
        return response(saved);
    }

    private BisCrsCreateResponse response(BisCrsRenewal record) {
        return new BisCrsCreateResponse(record.getId(), record.getManufacturerName(), record.getProductName(),
                record.getLicenceNumber(), record.getIsStandard(), record.getLicenceDate(), record.getExpiryDate(),
                record.getNotifyDate(), record.getBrandName(), record.getAirName(), record.getAirAddress(),
                record.getAirEmailId(), record.getAirContactNumber(), record.getCurrentStatus(), record.getCreatedBy(),
                "BIS CRS record created successfully");
    }

    private String required(String value, String name) {
        String cleaned = field(value, name);
        if (cleaned.isEmpty()) throw new IllegalArgumentException(name + " is required");
        return cleaned;
    }

    private String field(String value, String name) { return limited(value, name, FIELD_LIMIT); }
    private String limited(String value, String name, int limit) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() > limit) throw new IllegalArgumentException(name + " must not exceed " + limit + " characters");
        return cleaned;
    }
}
