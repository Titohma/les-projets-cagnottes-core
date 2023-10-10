package fr.lesprojetscagnottes.core.account.repository;

import fr.lesprojetscagnottes.core.account.entity.AccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    AccountEntity findByOwnerIdAndBudgetId(Long ownerId, Long budgetId);

    Set<AccountEntity> findAllByOwnerId(Long ownerId);

    @Query(value = "select a.* from accounts a inner join users u on u.id = a.owner_id where a.budget_id = :budget_id", nativeQuery = true)
    Page<AccountEntity> findAllByBudgetIdOrderByUser(@Param("budget_id") Long budgetId, Pageable pageable);

    Set<AccountEntity> findAllByBudgetId(Long id);

    Set<AccountEntity> findAllByOwnerIdAndBudgetIdIn(Long id, Set<Long  > budgetIds);
}