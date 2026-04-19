package com.senim.backend.config;

import com.senim.backend.domain.Agency;
import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.ChecklistStatus;
import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.Notification;
import com.senim.backend.domain.NotificationType;
import com.senim.backend.domain.RiskLevel;
import com.senim.backend.domain.Role;
import com.senim.backend.domain.SubscriptionTier;
import com.senim.backend.domain.User;
import com.senim.backend.repository.AgencyRepository;
import com.senim.backend.repository.ChecklistItemRepository;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.repository.NotificationRepository;
import com.senim.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            AgencyRepository agencyRepository,
            UserRepository userRepository,
            DealRepository dealRepository,
            ChecklistItemRepository checklistItemRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.dealRepository = dealRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final List<Object[]> DEFAULT_CHECKLIST = List.of(
            new Object[]{"passport",             "Паспорт / Удостоверение личности",  1},
            new Object[]{"income_proof",         "Справка о доходах",                 2},
            new Object[]{"employment_letter",    "Справка с места работы",            3},
            new Object[]{"property_valuation",   "Отчёт об оценке недвижимости",      4},
            new Object[]{"property_title",       "Правоустанавливающий документ",     5},
            new Object[]{"bank_statement",       "Выписка по счёту (6 месяцев)",      6},
            new Object[]{"marriage_certificate", "Свидетельство о браке (если есть)", 7}
    );

    private static final String[] CLIENT_NAMES = {
            "Алишер Жумабаев", "Дария Сарсенова", "Нурлан Тулепов", "Айгуль Сапарова",
            "Ержан Абенов", "Камила Дюсенова", "Марат Исмагулов", "Асель Нуржанова",
            "Тимур Калиев", "Жанара Досанова", "Бахыт Касымов", "Айнур Серикова",
            "Ержан Муратов", "Гульнара Алимова", "Рустам Омаров", "Сауле Ахметова",
            "Дамир Искаков", "Алия Байсеитова", "Канат Сулейменов", "Айдана Ережепова",
            "Азамат Токтаров", "Меруерт Жаксылыкова", "Санжар Бейсенов", "Диас Есенов",
            "Мадина Турсунова"
    };

    private static final String[] ADDRESSES = {
            "г. Алматы, ул. Абая 150, кв. 45",
            "г. Астана, пр. Кабанбай батыра 11, кв. 203",
            "г. Алматы, ЖК «Esentai Apartments», кв. 812",
            "г. Астана, ул. Сыганак 18, кв. 77",
            "г. Алматы, мкр. Самал-2, д. 55, кв. 9",
            "г. Шымкент, ул. Жибек Жолы 45, кв. 120",
            "г. Астана, ЖК «Хайвилл», блок 3, кв. 401",
            "г. Алматы, ул. Достык 132, кв. 21",
            "г. Астана, пр. Туран 37, кв. 505",
            "г. Алматы, ЖК «Bayan-Auyl», кв. 14",
            "г. Астана, ЖК «Green Residence», кв. 308",
            "г. Алматы, мкр. Коктем-3, д. 18, кв. 62",
            "г. Шымкент, ЖК «Аккент», кв. 204",
            "г. Астана, ул. Достык 8, кв. 1201",
            "г. Алматы, пр. Аль-Фараби 77, кв. 39",
            "г. Атырау, ул. Абая 10, кв. 55",
            "г. Караганда, ул. Ерубаева 22, кв. 8",
            "г. Астана, ЖК «Highvill Астана», кв. 910",
            "г. Алматы, ул. Назарбаева 223, кв. 302",
            "г. Актау, мкр. 14, д. 5, кв. 16"
    };

    private static final String[] PHONES = {
            "+7 701 234 56 78", "+7 705 987 65 43", "+7 707 111 22 33", "+7 708 444 55 66",
            "+7 700 777 88 99", "+7 747 222 33 44", "+7 775 555 66 77", "+7 771 888 99 00",
            "+7 702 333 44 55", "+7 706 666 77 88", "+7 777 999 00 11", "+7 778 123 45 67"
    };

    private static final String[][] BANKS_AND_PROGRAMS = {
            {"Halyk Bank",        "Баспана Хит"},
            {"Halyk Bank",        "Ипотека 7-20-25"},
            {"Kaspi Bank",        "Kaspi Home"},
            {"Kaspi Bank",        "Kaspi Red Ипотека"},
            {"Otbasy Bank",       "Нурлы жер"},
            {"Otbasy Bank",       "Стандарт"},
            {"Jusan Bank",        "Jusan Home"},
            {"Jusan Bank",        "Первичный рынок"},
            {"ForteBank",         "Forte Home"},
            {"ForteBank",         "7-20-25"},
            {"Freedom Bank",      "Freedom Ипотека"},
            {"Bereke Bank",       "Семейная ипотека"},
            {"Bank CenterCredit", "BCC Ипотека"}
    };

    private static final String[] NOTES = {
            "Клиент — молодая семья с детьми. Хочет новостройку в центре.",
            "Первый взнос уже подтверждён. Документы в хорошем состоянии.",
            "Клиент переезжает из Алматы. Торопится с оформлением.",
            "Рекомендация от прежнего клиента. Высокая платёжеспособность.",
            "Ждёт одобрения по предыдущей заявке в другом банке.",
            "Нужно уточнить оценку — разница с договором 5%.",
            "Покупка под сдачу в аренду. Запрос на ипотеку 10 лет.",
            "Военный специалист, пакет документов стандартный.",
            null,
            "Сложная ситуация с пропиской, уже решается.",
            "Военная ипотека. Комиссия ниже обычной.",
            null
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // If the database already has deals (from a previous seed run or real usage),
        // do nothing — we only populate an empty system.
        if (dealRepository.count() > 0) {
            log.info("Seed skipped — deals already exist ({} total).", dealRepository.count());
            return;
        }

        Agency agency = agencyRepository.findAll().stream()
                .filter(a -> "Demo Agency".equals(a.getName()))
                .findFirst()
                .orElseGet(() -> agencyRepository.save(
                        Agency.builder()
                                .name("Demo Agency")
                                .subscriptionTier(SubscriptionTier.PRO)
                                .build()
                ));
        log.info("Using agency: {} ({})", agency.getName(), agency.getId());

        User owner = upsertUser("owner@demo.senim.kz", "Aibek Dzhaksybekov", Role.OWNER, agency.getId());
        User agent1 = upsertUser("agent1@demo.senim.kz", "Daniyar Seitkali", Role.AGENT, agency.getId());
        User agent2 = upsertUser("agent2@demo.senim.kz", "Zarina Bekova", Role.AGENT, agency.getId());
        User agent3 = upsertUser("agent3@demo.senim.kz", "Nurlan Kazhybayev", Role.AGENT, agency.getId());
        log.info("Seeded 3 agents");

        List<User> agents = List.of(agent1, agent2, agent3);

        // Deterministic random so demo is reproducible
        Random rnd = new Random(42L);
        LocalDate today = LocalDate.now();

        // Carefully-chosen distribution so the dashboard looks alive:
        //  - a few CRITICAL/HIGH with tight deadlines
        //  - mix of IN_PROGRESS, SUBMITTED, APPROVED, REJECTED, DRAFT, CANCELLED
        Object[][] specs = {
                // {status, risk, bookingOffset (days from today, or null), bankOffset, commission, property, index}
                {DealStatus.IN_PROGRESS,  RiskLevel.CRITICAL,  1,   1,   7500,  210000},
                {DealStatus.IN_PROGRESS,  RiskLevel.CRITICAL,  null,2,   6200,  180000},
                {DealStatus.IN_PROGRESS,  RiskLevel.HIGH,      4,   5,   5200,  156000},
                {DealStatus.IN_PROGRESS,  RiskLevel.HIGH,      6,   3,   8400,  260000},
                {DealStatus.IN_PROGRESS,  RiskLevel.MEDIUM,    12,  10,  3800,  128000},
                {DealStatus.IN_PROGRESS,  RiskLevel.MEDIUM,    20,  18,  4500,  145000},
                {DealStatus.IN_PROGRESS,  RiskLevel.LOW,       35,  28,  5100,  170000},
                {DealStatus.IN_PROGRESS,  RiskLevel.LOW,       45,  30,  6700,  220000},
                {DealStatus.SUBMITTED,    RiskLevel.HIGH,      null,3,   4600,  142000},
                {DealStatus.SUBMITTED,    RiskLevel.MEDIUM,    null,9,   5900,  185000},
                {DealStatus.SUBMITTED,    RiskLevel.LOW,       null,22,  7200,  230000},
                {DealStatus.SUBMITTED,    RiskLevel.LOW,       null,40,  4200,  138000},
                {DealStatus.APPROVED,     RiskLevel.LOW,       null,null,5800,  195000},
                {DealStatus.APPROVED,     RiskLevel.LOW,       null,null,9000,  310000},
                {DealStatus.APPROVED,     RiskLevel.LOW,       null,null,6300,  210000},
                {DealStatus.APPROVED,     RiskLevel.LOW,       null,null,4500,  150000},
                {DealStatus.APPROVED,     RiskLevel.LOW,       null,null,7100,  245000},
                {DealStatus.REJECTED,     RiskLevel.HIGH,      null,null,4800,  165000},
                {DealStatus.REJECTED,     RiskLevel.MEDIUM,    null,null,3200,  112000},
                {DealStatus.DRAFT,        RiskLevel.LOW,       null,null,5400,  175000},
                {DealStatus.DRAFT,        RiskLevel.LOW,       null,null,4100,  140000},
                {DealStatus.CANCELLED,    RiskLevel.LOW,       null,null,3800,  125000}
        };

        List<Deal> savedDeals = new ArrayList<>();
        for (int i = 0; i < specs.length; i++) {
            Object[] s = specs[i];
            DealStatus status = (DealStatus) s[0];
            RiskLevel risk = (RiskLevel) s[1];
            Integer bookingOffset = (Integer) s[2];
            Integer bankOffset = (Integer) s[3];
            int commission = (Integer) s[4];
            int property = (Integer) s[5];

            String[] bankPair = BANKS_AND_PROGRAMS[i % BANKS_AND_PROGRAMS.length];
            User assignedAgent = agents.get(i % agents.size());
            Instant createdAt = Instant.now().minus(rnd.nextInt(90) + 2, ChronoUnit.DAYS);

            Deal deal = Deal.builder()
                    .agencyId(agency.getId())
                    .agentId(assignedAgent.getId())
                    .clientName(CLIENT_NAMES[i % CLIENT_NAMES.length])
                    .clientPhone(PHONES[i % PHONES.length])
                    .propertyAddress(ADDRESSES[i % ADDRESSES.length])
                    .propertyValueUsd(BigDecimal.valueOf(property))
                    .commissionUsd(BigDecimal.valueOf(commission))
                    .bankName(bankPair[0])
                    .mortgageProgramName(bankPair[1])
                    .status(status)
                    .riskLevel(risk)
                    .bookingExpiryDate(bookingOffset == null ? null : today.plusDays(bookingOffset))
                    .bankResponseDeadline(bankOffset == null ? null : today.plusDays(bankOffset))
                    .notes(NOTES[i % NOTES.length])
                    .statusChangedAt(createdAt.plus(rnd.nextInt(5), ChronoUnit.DAYS))
                    .build();

            Deal persisted = dealRepository.save(deal);
            savedDeals.add(persisted);

            seedChecklist(persisted, status, risk, owner.getId());
        }
        log.info("Seeded {} deals across {} agents", savedDeals.size(), agents.size());

        // Seed a handful of unread notifications for the owner and agent1 so the UI has signal
        seedNotifications(savedDeals, owner, agent1);

        log.info("Seed complete.");
        log.info("Credentials: owner@demo.senim.kz / Password1! · agent1@demo.senim.kz / Password1!");
    }

    private User upsertUser(String email, String fullName, Role role, UUID agencyId) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(
                User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("Password1!"))
                        .fullName(fullName)
                        .role(role)
                        .agencyId(agencyId)
                        .build()
        ));
    }

    private void seedChecklist(Deal deal, DealStatus dealStatus, RiskLevel risk, UUID verifierId) {
        List<ChecklistItem> items = new ArrayList<>();
        Random rnd = new Random(deal.getId().hashCode());

        for (Object[] row : DEFAULT_CHECKLIST) {
            ChecklistStatus itemStatus;

            if (dealStatus == DealStatus.APPROVED || dealStatus == DealStatus.SUBMITTED) {
                itemStatus = ChecklistStatus.VERIFIED;
            } else if (dealStatus == DealStatus.DRAFT) {
                itemStatus = ChecklistStatus.MISSING;
            } else if (dealStatus == DealStatus.CANCELLED) {
                itemStatus = rnd.nextBoolean() ? ChecklistStatus.MISSING : ChecklistStatus.UPLOADED;
            } else if (dealStatus == DealStatus.REJECTED) {
                itemStatus = ChecklistStatus.VERIFIED;
            } else {
                // IN_PROGRESS: spread across states
                double pick = rnd.nextDouble();
                if (risk == RiskLevel.CRITICAL) {
                    itemStatus = pick < 0.5 ? ChecklistStatus.MISSING
                            : pick < 0.75 ? ChecklistStatus.PENDING
                            : ChecklistStatus.UPLOADED;
                } else if (risk == RiskLevel.HIGH) {
                    itemStatus = pick < 0.3 ? ChecklistStatus.MISSING
                            : pick < 0.55 ? ChecklistStatus.PENDING
                            : pick < 0.85 ? ChecklistStatus.UPLOADED
                            : ChecklistStatus.VERIFIED;
                } else {
                    itemStatus = pick < 0.1 ? ChecklistStatus.MISSING
                            : pick < 0.25 ? ChecklistStatus.PENDING
                            : pick < 0.6 ? ChecklistStatus.UPLOADED
                            : ChecklistStatus.VERIFIED;
                }
            }

            ChecklistItem item = ChecklistItem.builder()
                    .dealId(deal.getId())
                    .itemKey((String) row[0])
                    .itemName((String) row[1])
                    .sortOrder((Integer) row[2])
                    .status(itemStatus)
                    .build();

            if (itemStatus == ChecklistStatus.VERIFIED) {
                item.setVerifiedAt(Instant.now().minus(rnd.nextInt(10) + 1, ChronoUnit.DAYS));
                item.setVerifiedByUserId(verifierId);
            }
            items.add(item);
        }
        checklistItemRepository.saveAll(items);
    }

    private void seedNotifications(List<Deal> deals, User owner, User agent) {
        List<Notification> toSave = new ArrayList<>();
        int created = 0;

        for (Deal d : deals) {
            if (d.getRiskLevel() == RiskLevel.CRITICAL) {
                toSave.add(Notification.builder()
                        .userId(owner.getId())
                        .dealId(d.getId())
                        .type(NotificationType.DEAL_CRITICAL)
                        .message(String.format("Сделка «%s» в критическом состоянии — требуется немедленное внимание",
                                d.getClientName()))
                        .build());

                if (d.getAgentId().equals(agent.getId())) {
                    toSave.add(Notification.builder()
                            .userId(agent.getId())
                            .dealId(d.getId())
                            .type(NotificationType.DEAL_CRITICAL)
                            .message(String.format("Ваша сделка «%s» стала критической", d.getClientName()))
                            .build());
                }
                created++;
            }
            if (d.getBookingExpiryDate() != null
                    && !d.getBookingExpiryDate().isBefore(LocalDate.now())
                    && ChronoUnit.DAYS.between(LocalDate.now(), d.getBookingExpiryDate()) <= 5) {
                toSave.add(Notification.builder()
                        .userId(owner.getId())
                        .dealId(d.getId())
                        .type(NotificationType.BOOKING_EXPIRY_WARNING)
                        .message(String.format("Бронь по сделке «%s» истекает через %d дн.",
                                d.getClientName(),
                                ChronoUnit.DAYS.between(LocalDate.now(), d.getBookingExpiryDate())))
                        .build());
                created++;
            }
            if (d.getBankResponseDeadline() != null
                    && !d.getBankResponseDeadline().isBefore(LocalDate.now())
                    && ChronoUnit.DAYS.between(LocalDate.now(), d.getBankResponseDeadline()) <= 5) {
                toSave.add(Notification.builder()
                        .userId(owner.getId())
                        .dealId(d.getId())
                        .type(NotificationType.BANK_DEADLINE_WARNING)
                        .message(String.format("Срок ответа банка по сделке «%s» — через %d дн.",
                                d.getClientName(),
                                ChronoUnit.DAYS.between(LocalDate.now(), d.getBankResponseDeadline())))
                        .build());
                created++;
            }
        }

        if (!toSave.isEmpty()) {
            notificationRepository.saveAll(toSave);
        }
        log.info("Seeded {} notifications ({} triggers)", toSave.size(), created);
    }
}
