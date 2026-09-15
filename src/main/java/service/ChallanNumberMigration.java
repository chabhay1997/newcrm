package service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import repository.ChallanRepository;

@Component
public class ChallanNumberMigration implements ApplicationRunner {
    private final ChallanRepository challanRepository;

    public ChallanNumberMigration(ChallanRepository challanRepository) {
        this.challanRepository = challanRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        challanRepository.migrateLegacyChallanNumbersToDc();
    }
}
