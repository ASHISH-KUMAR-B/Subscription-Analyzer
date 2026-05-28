package com.yourorg.Subscriptions;

import com.yourorg.Storage.CloudinaryStorageService;
import com.yourorg.Subscriptions.dto.SubscriptionRequest;
import com.yourorg.Subscriptions.dto.SubscriptionResponse;
import com.yourorg.Subscriptions.dto.UploadCsvResponse;
import com.yourorg.Users.CurrentUserService;
import com.yourorg.Users.User;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserService currentUserService;
    private final CloudinaryStorageService cloudinaryStorageService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            CurrentUserService currentUserService,
            CloudinaryStorageService cloudinaryStorageService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.currentUserService = currentUserService;
        this.cloudinaryStorageService = cloudinaryStorageService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getAll() {
        User user = currentUserService.getCurrentUser();
        return subscriptionRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getById(Long id) {
        User user = currentUserService.getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getUser().getId() == user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        validateRequest(request);
        User user = currentUserService.getCurrentUser();

        Subscription subscription = new Subscription(
                user,
                request.providerName().trim(),
                parseCategory(request.category()),
                request.startDate(),
                parseRenewalCycle(request.renewalCycle()),
                request.renewalDate(),
                request.price(),
                normalizeCurrency(request.currency())
        );

        if (request.status() != null && !request.status().isBlank()) {
            subscription.setStatus(parseStatus(request.status()));
        }

        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        validateRequest(request);
        User user = currentUserService.getCurrentUser();

        Subscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getUser().getId() == user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        subscription.setProviderName(request.providerName().trim());
        subscription.setCategory(parseCategory(request.category()));
        subscription.setStartDate(request.startDate());
        subscription.setRenewalCycle(parseRenewalCycle(request.renewalCycle()));
        subscription.setRenewalDate(request.renewalDate());
        subscription.setPrice(request.price());
        subscription.setCurrency(normalizeCurrency(request.currency()));

        if (request.status() != null && !request.status().isBlank()) {
            subscription.setStatus(parseStatus(request.status()));
        }

        subscription.setUpdatedAt(Instant.now());

        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserService.getCurrentUser();
        Subscription subscription = subscriptionRepository.findById(id)
                .filter(s -> s.getUser().getId() == user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
        subscriptionRepository.delete(subscription);
    }

    @Transactional
    public UploadCsvResponse uploadCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV file is required");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .csv files are supported");
        }

        User user = currentUserService.getCurrentUser();
        String fileUrl = cloudinaryStorageService.uploadCsv(file);

        List<UploadCsvResponse.DetectedItem> detected = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int imported = 0;

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            for (CSVRecord row : parser) {
                try {
                    String date = read(row, "date");
                    String description = read(row, "description");
                    String amountRaw = read(row, "amount");
                    String merchant = read(row, "merchant");

                    String providerName = normalizeProviderName(merchant, description);
                    Category category = guessCategory(providerName + " " + description);
                    BigDecimal amount = new BigDecimal(amountRaw.replace(",", "").replace("$", "").replace("-", "")).abs();

                    boolean isRecurringCandidate = amount.signum() > 0 && !providerName.isBlank();
                    boolean alreadyExists = subscriptionRepository
                            .findByUserIdAndProviderNameIgnoreCase(user.getId(), providerName)
                            .isPresent();

                    boolean rowImported = false;
                    if (isRecurringCandidate && !alreadyExists) {
                        LocalDate startDate = LocalDate.parse(date);
                        LocalDate renewalDate = startDate.plusMonths(1);

                        Subscription created = new Subscription(
                                user,
                                providerName,
                                category,
                                startDate,
                                Renewal_Cycle.MONTHLY,
                                renewalDate,
                                amount,
                                "USD"
                        );
                        created.setStatus(SubscriptionStatus.Active);
                        subscriptionRepository.save(created);
                        imported++;
                        rowImported = true;
                    }

                    detected.add(new UploadCsvResponse.DetectedItem(
                            providerName,
                            merchant,
                            description,
                            category.name(),
                            date,
                            amount,
                            rowImported
                    ));
                } catch (Exception rowEx) {
                    errors.add("Row " + row.getRecordNumber() + ": " + rowEx.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read CSV file", ex);
        }

        return new UploadCsvResponse(detected, imported, errors, fileUrl);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getProviderName(),
                subscription.getCategory().name(),
                subscription.getStartDate(),
                subscription.getRenewalCycle().name(),
                subscription.getRenewalDate(),
                subscription.getPrice(),
                subscription.getCurrency(),
                subscription.getStatus().name(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }

    private void validateRequest(SubscriptionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.providerName() == null || request.providerName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "providerName is required");
        }
        if (request.price() == null || request.price().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be greater than 0");
        }
        if (request.startDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate is required");
        }
        if (request.renewalDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "renewalDate is required");
        }
    }

    private Category parseCategory(String raw) {
        try {
            return Category.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category: " + raw);
        }
    }

    private Renewal_Cycle parseRenewalCycle(String raw) {
        try {
            return Renewal_Cycle.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid renewalCycle: " + raw);
        }
    }

    private SubscriptionStatus parseStatus(String raw) {
        try {
            String value = raw == null ? "" : raw.trim();
            value = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
            return SubscriptionStatus.valueOf(value);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + raw);
        }
    }

    private String normalizeCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency is required");
        }
        return raw.trim().toUpperCase();
    }

    private String read(CSVRecord row, String key) {
        if (!row.isMapped(key)) {
            throw new IllegalArgumentException("Missing required column: " + key);
        }
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing value for column: " + key);
        }
        return value.trim();
    }

    private String normalizeProviderName(String merchant, String description) {
        String source = merchant != null && !merchant.isBlank() ? merchant : description;
        if (source == null) {
            return "Unknown";
        }
        return source.replaceAll("\\.COM$", "")
                .replaceAll("\\s+PREMIUM$", "")
                .trim();
    }

    private Category guessCategory(String text) {
        String normalized = text == null ? "" : text.toUpperCase(Locale.ROOT);
        for (Map.Entry<String, Category> entry : Map.ofEntries(
                Map.entry("NETFLIX", Category.STREAMING),
                Map.entry("SPOTIFY", Category.MUSIC),
                Map.entry("YOUTUBE", Category.STREAMING),
                Map.entry("ADOBE", Category.PRODUCTIVITY),
                Map.entry("MICROSOFT", Category.PRODUCTIVITY),
                Map.entry("GOOGLE", Category.CLOUD),
                Map.entry("AWS", Category.CLOUD),
                Map.entry("XBOX", Category.GAMING),
                Map.entry("PLAYSTATION", Category.GAMING),
                Map.entry("GYM", Category.FITNESS),
                Map.entry("NEWS", Category.NEWS)
        ).entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return Category.OTHER;
    }
}
