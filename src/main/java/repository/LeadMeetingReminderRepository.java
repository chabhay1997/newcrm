package repository;

import model.LeadMeetingReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeadMeetingReminderRepository extends JpaRepository<LeadMeetingReminder, Long> {
    List<LeadMeetingReminder> findByUserIdAndDismissedAtIsNullAndRemindAtLessThanEqualOrderByRemindAtAsc(
            Long userId, LocalDateTime remindAt);

    Optional<LeadMeetingReminder> findByIdAndUserIdAndDismissedAtIsNull(Long id, Long userId);
}