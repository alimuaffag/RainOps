package com.rainops.config;

import com.rainops.incident.entity.IncidentPriority;
import com.rainops.incident.entity.IncidentStatus;
import com.rainops.incident.repository.IncidentRepository;
import com.rainops.incident.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
@ConditionalOnProperty(name = "rainops.demo.seed", havingValue = "true")
@RequiredArgsConstructor
public class DemoData implements ApplicationRunner {
    private final IncidentRepository repository;
    private final IncidentService service;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (repository.count() != 0) return;
        String[] titles = {"تجمع مياه عند تقاطع شارع", "انسداد مصرف بجوار حديقة",
                "تجمع مياه أمام مدخل مدرسة", "مياه على جانب الطريق",
                "تجمع مياه في ممر مشاة", "ارتفاع مياه قرب موقف سيارات"};
        String[] districts = {"النزهة", "الروضة", "الصفا", "السلامة", "الزهراء", "النعيم"};
        IncidentPriority[] priorities = {IncidentPriority.HIGH, IncidentPriority.MEDIUM,
                IncidentPriority.HIGH, IncidentPriority.LOW, IncidentPriority.MEDIUM, IncidentPriority.LOW};
        for (int index = 0; index < titles.length; index++) {
            var incident = service.createIncident(titles[index],
                    "بلاغ تجريبي لعرض النظام. لا يمثل حادثة فعلية.", districts[index],
                    "موقع افتراضي لأغراض المراجعة", null, null, priorities[index]);
            if (index % 3 != 0) {
                service.changeStatus(incident.getId(), IncidentStatus.IN_PROGRESS, "بدء المعالجة — مثال تجريبي");
            }
            if (index % 3 == 2) {
                service.changeStatus(incident.getId(), IncidentStatus.RESOLVED, "اكتملت المعالجة — مثال تجريبي");
            }
        }
    }
}
